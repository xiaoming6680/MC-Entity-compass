package com.csesp.entitycompass.client;

import com.csesp.entitycompass.EntityCompassMod;
import com.csesp.entitycompass.network.OpenTargetMenuPayload;
import com.csesp.entitycompass.network.SyncGlobalSettingsPayload;
import com.csesp.entitycompass.network.UpdateGlobalSettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EntityCompassClient implements ClientModInitializer {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(EntityCompassMod.id("entity_compass"));
    private static KeyBinding openGlobalSettingsKey;
    private static boolean distanceLimitEnabled = EntityCompassMod.DEFAULT_DISTANCE_LIMIT_ENABLED;
    private static int distanceLimitBlocks = EntityCompassMod.DEFAULT_DISTANCE_LIMIT_BLOCKS;
    private static boolean glintEnabled = EntityCompassMod.DEFAULT_GLINT_ENABLED;
    private static int distanceDisplayMode = EntityCompassMod.DEFAULT_DISTANCE_DISPLAY_MODE;

    @Override
    public void onInitializeClient() {
        openGlobalSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.entity_compass.open_global_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                CATEGORY
        ));

        ClientPlayNetworking.registerGlobalReceiver(OpenTargetMenuPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            if (client.player != null && client.world != null && client.currentScreen == null) {
                client.setScreen(new EntityCompassScreen());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SyncGlobalSettingsPayload.ID, (payload, context) -> {
            setGlobalSettings(
                    payload.distanceLimitEnabled(),
                    payload.distanceLimitBlocks(),
                    payload.glintEnabled(),
                    payload.distanceDisplayMode()
            );
            if (context.client().currentScreen instanceof EntityCompassGlobalSettingsScreen screen) {
                screen.onGlobalSettingsSynced(
                        payload.distanceLimitEnabled(),
                        payload.distanceLimitBlocks(),
                        payload.glintEnabled(),
                        payload.distanceDisplayMode()
                );
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(EntityCompassClient::onEndClientTick);
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                EntityCompassMod.id("distance_display"),
                EntityCompassHud::render
        );
    }

    private static void onEndClientTick(MinecraftClient client) {
        while (openGlobalSettingsKey.wasPressed()) {
            if (client.player == null || client.world == null || client.currentScreen != null) {
                continue;
            }

            if (!ClientPlayNetworking.canSend(UpdateGlobalSettingsPayload.ID)) {
                client.player.sendMessage(Text.translatable("message.entity_compass.server_missing"), true);
                continue;
            }

            client.setScreen(new EntityCompassGlobalSettingsScreen());
        }
    }

    public static boolean isDistanceLimitEnabled() {
        return distanceLimitEnabled;
    }

    public static int getDistanceLimitBlocks() {
        return distanceLimitBlocks;
    }

    public static boolean isGlintEnabled() {
        return glintEnabled;
    }

    public static int getDistanceDisplayMode() {
        return distanceDisplayMode;
    }

    public static void setGlobalSettings(boolean distanceLimitEnabled, int blocks, boolean glintEnabled, int distanceDisplayMode) {
        EntityCompassClient.distanceLimitEnabled = distanceLimitEnabled;
        distanceLimitBlocks = EntityCompassMod.sanitizeDistanceLimitBlocks(blocks);
        EntityCompassClient.glintEnabled = glintEnabled;
        EntityCompassClient.distanceDisplayMode = EntityCompassMod.sanitizeDistanceDisplayMode(distanceDisplayMode);
    }
}
