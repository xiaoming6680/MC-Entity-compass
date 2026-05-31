package com.csesp.entitycompass.client;

import com.csesp.entitycompass.EntityCompassMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class EntityCompassHud {
    private EntityCompassHud() {
    }

    @SuppressWarnings("unused")
    public static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        ItemStack stack = getHeldCompass(client.player);
        if (stack.isEmpty()) {
            return;
        }

        int displayMode = EntityCompassMod.getDistanceDisplayMode(stack);
        if (displayMode == EntityCompassMod.DISTANCE_DISPLAY_OFF) {
            drawWatermark(context, client.textRenderer);
            return;
        }

        UUID targetPlayerUuid = stack.get(EntityCompassMod.TARGET_PLAYER_UUID);
        Identifier targetId = stack.get(EntityCompassMod.TARGET_ENTITY_TYPE);
        LodestoneTrackerComponent tracker = stack.get(DataComponentTypes.LODESTONE_TRACKER);
        if (targetPlayerUuid == null && targetId == null) {
            drawWatermark(context, client.textRenderer);
            return;
        }

        if (tracker == null || tracker.target().isEmpty()) {
            Text message = Boolean.TRUE.equals(stack.get(EntityCompassMod.TARGET_NEARBY))
                    ? Text.translatable("hud.entity_compass.target_nearby")
                    : Text.translatable("hud.entity_compass.not_found");
            drawCentered(context, client.textRenderer, message, displayMode, 0xFFFF5555);
            drawWatermark(context, client.textRenderer);
            return;
        }

        GlobalPos target = tracker.target().get();
        if (!target.dimension().equals(client.world.getRegistryKey())) {
            drawCentered(context, client.textRenderer, Text.translatable("hud.entity_compass.not_found"), displayMode, 0xFFFF5555);
            drawWatermark(context, client.textRenderer);
            return;
        }

        Text targetName = getTargetName(stack, targetPlayerUuid, targetId);
        int distance = (int) Math.round(Math.sqrt(client.player.squaredDistanceTo(Vec3d.ofCenter(target.pos()))));
        Text text = Text.translatable("hud.entity_compass.distance", targetName, distance);

        drawCentered(context, client.textRenderer, text, displayMode, 0xFFFFE066);
        drawWatermark(context, client.textRenderer);
    }

    private static Text getTargetName(ItemStack stack, @Nullable UUID targetPlayerUuid, @Nullable Identifier targetId) {
        if (targetPlayerUuid != null) {
            String playerName = stack.get(EntityCompassMod.TARGET_PLAYER_NAME);
            return Text.literal(playerName != null ? playerName : targetPlayerUuid.toString());
        }

        if (targetId == null) {
            return Text.translatable("hud.entity_compass.not_found");
        }

        return Registries.ENTITY_TYPE.getOptionalValue(targetId)
                .map(EntityType::getName)
                .orElse(Text.literal(targetId.toString()));
    }

    private static void drawCentered(DrawContext context, TextRenderer textRenderer, Text text, int displayMode, int color) {
        int textWidth = textRenderer.getWidth(text);
        int x = (context.getScaledWindowWidth() - textWidth) / 2;
        int y = getY(context, displayMode);

        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    private static void drawWatermark(DrawContext context, TextRenderer textRenderer) {
        Text watermark = Text.literal(EntityCompassMod.WATERMARK);
        int x = context.getScaledWindowWidth() - textRenderer.getWidth(watermark) - 4;
        int y = context.getScaledWindowHeight() - 12;
        context.drawTextWithShadow(textRenderer, watermark, x, y, 0x99FFFFFF);
    }

    private static int getY(DrawContext context, int displayMode) {
        return switch (EntityCompassMod.sanitizeDistanceDisplayMode(displayMode)) {
            case EntityCompassMod.DISTANCE_DISPLAY_ABOVE_CROSSHAIR -> context.getScaledWindowHeight() / 2 - 36;
            case EntityCompassMod.DISTANCE_DISPLAY_BELOW_CROSSHAIR -> context.getScaledWindowHeight() / 2 + 24;
            default -> context.getScaledWindowHeight() - 92;
        };
    }

    private static ItemStack getHeldCompass(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.isOf(EntityCompassMod.ENTITY_COMPASS)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffHandStack();
        return offHand.isOf(EntityCompassMod.ENTITY_COMPASS) ? offHand : ItemStack.EMPTY;
    }
}
