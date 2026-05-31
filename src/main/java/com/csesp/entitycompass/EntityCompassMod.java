package com.csesp.entitycompass;

import com.csesp.entitycompass.network.SetTargetEntityPayload;
import com.csesp.entitycompass.network.OpenTargetMenuPayload;
import com.csesp.entitycompass.network.SetTargetPlayerPayload;
import com.csesp.entitycompass.network.SyncGlobalSettingsPayload;
import com.csesp.entitycompass.network.UpdateGlobalSettingsPayload;
import com.mojang.serialization.Codec;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class EntityCompassMod implements ModInitializer {
    public static final String MOD_ID = "entity_compass";
    public static final String WATERMARK = "MADE BY XM6680";
    public static final int DISTANCE_DISPLAY_OFF = 0;
    public static final int DISTANCE_DISPLAY_ABOVE_HOTBAR = 1;
    public static final int DISTANCE_DISPLAY_ABOVE_CROSSHAIR = 2;
    public static final int DISTANCE_DISPLAY_BELOW_CROSSHAIR = 3;
    public static final int DISTANCE_DISPLAY_MODE_COUNT = 4;
    public static final int DEFAULT_DISTANCE_DISPLAY_MODE = DISTANCE_DISPLAY_ABOVE_HOTBAR;
    public static final boolean DEFAULT_GLINT_ENABLED = true;
    public static final boolean DEFAULT_DISTANCE_LIMIT_ENABLED = false;
    public static final int DEFAULT_DISTANCE_LIMIT_BLOCKS = 255;
    public static final int MIN_DISTANCE_LIMIT_BLOCKS = 0;
    public static final int MAX_DISTANCE_LIMIT_BLOCKS = 255;

    private static final double TRACKING_RANGE = 256.0D;
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final PermissionLevel GLOBAL_SETTINGS_PERMISSION_LEVEL = PermissionLevel.GAMEMASTERS;

    public static final ComponentType<Identifier> TARGET_ENTITY_TYPE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            id("target_entity_type"),
            ComponentType.<Identifier>builder()
                    .codec(Identifier.CODEC)
                    .packetCodec(Identifier.PACKET_CODEC)
                    .build()
    );

    public static final ComponentType<UUID> TARGET_PLAYER_UUID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            id("target_player_uuid"),
            ComponentType.<UUID>builder()
                    .codec(Uuids.CODEC)
                    .packetCodec(Uuids.PACKET_CODEC)
                    .build()
    );

    public static final ComponentType<String> TARGET_PLAYER_NAME = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            id("target_player_name"),
            ComponentType.<String>builder()
                    .codec(Codec.STRING)
                    .packetCodec(PacketCodecs.STRING)
                    .build()
    );

    public static final ComponentType<Boolean> TARGET_NEARBY = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            id("target_nearby"),
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOLEAN)
                    .build()
    );

    public static final ComponentType<Integer> DISTANCE_DISPLAY_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            id("distance_display_mode"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build()
    );

    public static final RegistryKey<Item> ENTITY_COMPASS_KEY =
            RegistryKey.of(RegistryKeys.ITEM, id("entity_compass"));

    public static final Item ENTITY_COMPASS = Registry.register(
            Registries.ITEM,
            ENTITY_COMPASS_KEY,
            new EntityCompassItem(new Item.Settings()
                    .registryKey(ENTITY_COMPASS_KEY)
                    .maxCount(1)
                    .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, DEFAULT_GLINT_ENABLED)
                    .component(TARGET_NEARBY, false)
                    .component(DISTANCE_DISPLAY_MODE, DEFAULT_DISTANCE_DISPLAY_MODE)
                    .rarity(Rarity.UNCOMMON))
    );

    private static int tickCounter;
    private static boolean distanceLimitEnabled = DEFAULT_DISTANCE_LIMIT_ENABLED;
    private static int distanceLimitBlocks = DEFAULT_DISTANCE_LIMIT_BLOCKS;
    private static boolean globalGlintEnabled = DEFAULT_GLINT_ENABLED;
    private static int globalDistanceDisplayMode = DEFAULT_DISTANCE_DISPLAY_MODE;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(SetTargetEntityPayload.ID, SetTargetEntityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetTargetPlayerPayload.ID, SetTargetPlayerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateGlobalSettingsPayload.ID, UpdateGlobalSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTargetMenuPayload.ID, OpenTargetMenuPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGlobalSettingsPayload.ID, SyncGlobalSettingsPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetTargetEntityPayload.ID, EntityCompassMod::handleSetTarget);
        ServerPlayNetworking.registerGlobalReceiver(SetTargetPlayerPayload.ID, EntityCompassMod::handleSetTargetPlayer);
        ServerPlayNetworking.registerGlobalReceiver(UpdateGlobalSettingsPayload.ID, EntityCompassMod::handleUpdateGlobalSettings);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendGlobalSettings(handler.getPlayer()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ENTITY_COMPASS));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter = (tickCounter + 1) % UPDATE_INTERVAL_TICKS;
            if (tickCounter != 0) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                applyGlobalOptionsToPlayerCompasses(player);
                refreshHeldCompass(player, player.getMainHandStack());
                refreshHeldCompass(player, player.getOffHandStack());
            }
        });
    }

    public static boolean isSelectableEntityType(EntityType<?> type) {
        return type.getSpawnGroup() != SpawnGroup.MISC;
    }

    private static void handleSetTarget(SetTargetEntityPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> setTarget(context.player(), payload.entityTypeId()));
    }

    private static void handleSetTargetPlayer(SetTargetPlayerPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> setTargetPlayer(context.server(), context.player(), payload.playerUuid()));
    }

    private static void handleUpdateGlobalSettings(UpdateGlobalSettingsPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            if (!canUpdateGlobalSettings(context.server(), context.player())) {
                context.player().sendMessage(Text.translatable("message.entity_compass.no_permission"), true);
                sendGlobalSettings(context.player());
                return;
            }

            updateGlobalSettings(
                    context.server(),
                    payload.distanceLimitEnabled(),
                    payload.distanceLimitBlocks(),
                    payload.glintEnabled(),
                    payload.distanceDisplayMode()
            );
        });
    }

    public static void openTargetMenu(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, OpenTargetMenuPayload.ID)) {
            ServerPlayNetworking.send(player, OpenTargetMenuPayload.INSTANCE);
        }
    }

    private static void setTarget(ServerPlayerEntity player, Identifier entityTypeId) {
        Optional<EntityType<?>> entityType = Registries.ENTITY_TYPE.getOptionalValue(entityTypeId);
        if (entityType.isEmpty() || !isSelectableEntityType(entityType.get())) {
            player.sendMessage(Text.translatable("message.entity_compass.invalid_target"), true);
            return;
        }

        ItemStack stack = getHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendMessage(Text.translatable("message.entity_compass.need_compass"), true);
            return;
        }

        stack.set(TARGET_ENTITY_TYPE, entityTypeId);
        stack.remove(TARGET_PLAYER_UUID);
        stack.remove(TARGET_PLAYER_NAME);
        stack.set(TARGET_NEARBY, false);
        refreshHeldCompass(player, stack);
        player.sendMessage(Text.translatable("message.entity_compass.target_set", entityType.get().getName()), true);
    }

    private static void setTargetPlayer(MinecraftServer server, ServerPlayerEntity player, UUID targetUuid) {
        if (targetUuid.equals(player.getUuid())) {
            player.sendMessage(Text.translatable("message.entity_compass.invalid_player_target"), true);
            return;
        }

        ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetUuid);
        if (targetPlayer == null) {
            player.sendMessage(Text.translatable("message.entity_compass.target_not_found"), true);
            return;
        }

        ItemStack stack = getHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendMessage(Text.translatable("message.entity_compass.need_compass"), true);
            return;
        }

        String playerName = targetPlayer.getGameProfile().name();
        stack.remove(TARGET_ENTITY_TYPE);
        stack.set(TARGET_PLAYER_UUID, targetUuid);
        stack.set(TARGET_PLAYER_NAME, playerName);
        stack.set(TARGET_NEARBY, false);
        refreshHeldCompass(player, stack);
        player.sendMessage(Text.translatable("message.entity_compass.target_set", Text.literal(playerName)), true);
    }

    private static void updateGlobalSettings(MinecraftServer server, boolean enabled, int blocks, boolean glintEnabled, int distanceDisplayMode) {
        distanceLimitEnabled = enabled;
        distanceLimitBlocks = sanitizeDistanceLimitBlocks(blocks);
        globalGlintEnabled = glintEnabled;
        globalDistanceDisplayMode = sanitizeDistanceDisplayMode(distanceDisplayMode);
        broadcastGlobalSettings(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            applyGlobalOptionsToPlayerCompasses(player);
            refreshHeldCompass(player, player.getMainHandStack());
            refreshHeldCompass(player, player.getOffHandStack());
        }
    }

    private static void broadcastGlobalSettings(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendGlobalSettings(player);
        }
    }

    private static void sendGlobalSettings(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, SyncGlobalSettingsPayload.ID)) {
            ServerPlayNetworking.send(player, new SyncGlobalSettingsPayload(
                    distanceLimitEnabled,
                    distanceLimitBlocks,
                    globalGlintEnabled,
                    globalDistanceDisplayMode
            ));
        }
    }

    private static boolean canUpdateGlobalSettings(MinecraftServer server, ServerPlayerEntity player) {
        return server.isSingleplayer()
                || (player.getPermissions() instanceof LeveledPermissionPredicate permissions
                && permissions.getLevel().isAtLeast(GLOBAL_SETTINGS_PERMISSION_LEVEL));
    }

    private static void applyGlobalOptionsToPlayerCompasses(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            applyGlobalOptions(player.getInventory().getStack(slot));
        }
    }

    private static void applyGlobalOptions(ItemStack stack) {
        if (!stack.isOf(ENTITY_COMPASS)) {
            return;
        }

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, globalGlintEnabled);
        stack.set(DISTANCE_DISPLAY_MODE, globalDistanceDisplayMode);
    }

    public static boolean isDistanceLimitEnabled() {
        return distanceLimitEnabled;
    }

    public static int getDistanceLimitBlocks() {
        return distanceLimitBlocks;
    }

    public static boolean isGlobalGlintEnabled() {
        return globalGlintEnabled;
    }

    public static int getGlobalDistanceDisplayMode() {
        return globalDistanceDisplayMode;
    }

    public static int sanitizeDistanceLimitBlocks(int blocks) {
        return Math.max(MIN_DISTANCE_LIMIT_BLOCKS, Math.min(MAX_DISTANCE_LIMIT_BLOCKS, blocks));
    }

    public static boolean hasVisualGlint(ItemStack stack) {
        Boolean glintOverride = stack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        return glintOverride != null ? glintOverride : DEFAULT_GLINT_ENABLED;
    }

    public static int getDistanceDisplayMode(ItemStack stack) {
        Integer mode = stack.get(DISTANCE_DISPLAY_MODE);
        return sanitizeDistanceDisplayMode(mode != null ? mode : DEFAULT_DISTANCE_DISPLAY_MODE);
    }

    public static int nextDistanceDisplayMode(int mode) {
        return (sanitizeDistanceDisplayMode(mode) + 1) % DISTANCE_DISPLAY_MODE_COUNT;
    }

    public static int sanitizeDistanceDisplayMode(int mode) {
        return mode >= 0 && mode < DISTANCE_DISPLAY_MODE_COUNT ? mode : DEFAULT_DISTANCE_DISPLAY_MODE;
    }

    public static ItemStack getHeldCompass(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.isOf(ENTITY_COMPASS)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffHandStack();
        return offHand.isOf(ENTITY_COMPASS) ? offHand : ItemStack.EMPTY;
    }

    private static void refreshHeldCompass(ServerPlayerEntity player, ItemStack stack) {
        if (!stack.isOf(ENTITY_COMPASS)) {
            return;
        }

        applyGlobalOptions(stack);

        UUID targetPlayerUuid = stack.get(TARGET_PLAYER_UUID);
        Identifier targetId = stack.get(TARGET_ENTITY_TYPE);
        if (targetPlayerUuid == null && targetId == null) {
            clearTracker(stack, false);
            return;
        }

        if (targetPlayerUuid != null) {
            ServerPlayerEntity targetPlayer = findTargetPlayer(player, targetPlayerUuid);
            if (targetPlayer == null) {
                clearTracker(stack, false);
                return;
            }

            if (isBelowDistanceLimit(player.squaredDistanceTo(targetPlayer))) {
                clearTracker(stack, true);
                return;
            }

            stack.set(TARGET_NEARBY, false);
            ServerWorld world = (ServerWorld) targetPlayer.getEntityWorld();
            stack.set(
                    DataComponentTypes.LODESTONE_TRACKER,
                    new LodestoneTrackerComponent(Optional.of(GlobalPos.create(world.getRegistryKey(), targetPlayer.getBlockPos())), false)
            );
            return;
        }

        Optional<EntityType<?>> entityType = Registries.ENTITY_TYPE.getOptionalValue(targetId);
        if (entityType.isEmpty()) {
            clearTracker(stack, false);
            return;
        }

        LivingEntity nearest = findNearestTarget(player, entityType.get());
        if (nearest == null) {
            clearTracker(stack, false);
            return;
        }

        if (isBelowDistanceLimit(player.squaredDistanceTo(nearest))) {
            clearTracker(stack, true);
            return;
        }

        stack.set(TARGET_NEARBY, false);
        ServerWorld world = (ServerWorld) nearest.getEntityWorld();
        stack.set(
                DataComponentTypes.LODESTONE_TRACKER,
                new LodestoneTrackerComponent(Optional.of(GlobalPos.create(world.getRegistryKey(), nearest.getBlockPos())), false)
        );
    }

    private static void clearTracker(ItemStack stack, boolean targetNearby) {
        stack.set(TARGET_NEARBY, targetNearby);
        stack.remove(DataComponentTypes.LODESTONE_TRACKER);
    }

    private static @Nullable ServerPlayerEntity findTargetPlayer(ServerPlayerEntity player, UUID targetUuid) {
        ServerPlayerEntity targetPlayer = player.getEntityWorld().getServer().getPlayerManager().getPlayer(targetUuid);
        if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.getEntityWorld() != player.getEntityWorld()) {
            return null;
        }

        double squaredDistance = player.squaredDistanceTo(targetPlayer);
        return isWithinTrackingRange(squaredDistance) ? targetPlayer : null;
    }

    private static @Nullable LivingEntity findNearestTarget(ServerPlayerEntity player, EntityType<?> targetType) {
        World entityWorld = player.getEntityWorld();
        if (!(entityWorld instanceof ServerWorld world)) {
            return null;
        }

        Box searchBox = player.getBoundingBox().expand(TRACKING_RANGE);
        return world.getEntitiesByClass(
                        LivingEntity.class,
                        searchBox,
                        entity -> entity.isAlive()
                                && entity.getType() == targetType
                                && entity != player
                                && isWithinTrackingRange(player.squaredDistanceTo(entity))
                )
                .stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo))
                .orElse(null);
    }

    private static boolean isWithinTrackingRange(double squaredDistance) {
        return squaredDistance <= TRACKING_RANGE * TRACKING_RANGE;
    }

    private static boolean isBelowDistanceLimit(double squaredDistance) {
        if (!distanceLimitEnabled) {
            return false;
        }

        double minDistance = distanceLimitBlocks;
        return squaredDistance < minDistance * minDistance;
    }
}
