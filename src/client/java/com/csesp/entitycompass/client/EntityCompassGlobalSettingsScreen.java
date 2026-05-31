package com.csesp.entitycompass.client;

import com.csesp.entitycompass.EntityCompassMod;
import com.csesp.entitycompass.network.UpdateGlobalSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class EntityCompassGlobalSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;

    private boolean distanceLimitEnabled;
    private int distanceLimitBlocks;
    private boolean glintEnabled;
    private int distanceDisplayMode;
    private TextFieldWidget distanceField;
    private ButtonWidget toggleButton;
    private ButtonWidget glintButton;
    private ButtonWidget distanceDisplayButton;

    public EntityCompassGlobalSettingsScreen() {
        super(Text.translatable("screen.entity_compass.global_settings.title"));
        this.distanceLimitEnabled = EntityCompassClient.isDistanceLimitEnabled();
        this.distanceLimitBlocks = EntityCompassClient.getDistanceLimitBlocks();
        this.glintEnabled = EntityCompassClient.isGlintEnabled();
        this.distanceDisplayMode = EntityCompassClient.getDistanceDisplayMode();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int left = centerX - PANEL_WIDTH / 2;

        toggleButton = ButtonWidget.builder(toggleButtonText(), button -> {
                    distanceLimitEnabled = !distanceLimitEnabled;
                    sendSettings();
                    button.setMessage(toggleButtonText());
                })
                .dimensions(left, 54, PANEL_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(toggleButton);

        glintButton = ButtonWidget.builder(glintButtonText(), button -> {
                    glintEnabled = !glintEnabled;
                    sendSettings();
                    button.setMessage(glintButtonText());
                })
                .dimensions(left, 80, PANEL_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(glintButton);

        distanceDisplayButton = ButtonWidget.builder(distanceDisplayButtonText(), button -> {
                    distanceDisplayMode = EntityCompassMod.nextDistanceDisplayMode(distanceDisplayMode);
                    sendSettings();
                    button.setMessage(distanceDisplayButtonText());
                })
                .dimensions(left, 106, PANEL_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(distanceDisplayButton);

        distanceField = new TextFieldWidget(
                this.textRenderer,
                centerX - 45,
                158,
                90,
                BUTTON_HEIGHT,
                Text.translatable("screen.entity_compass.global_settings.distance_limit")
        );
        distanceField.setMaxLength(3);
        distanceField.setTextPredicate(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        distanceField.setText(Integer.toString(distanceLimitBlocks));
        addDrawableChild(distanceField);
        setInitialFocus(distanceField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.entity_compass.apply"), button -> sendSettings())
                .dimensions(left, 194, 110, BUTTON_HEIGHT)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.entity_compass.close"), button -> close())
                .dimensions(left + PANEL_WIDTH - 110, 194, 110, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, 0x90000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.entity_compass.global_settings.distance_limit"),
                this.width / 2,
                140,
                0xFFFFFFFF
        );
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.entity_compass.global_settings.range", EntityCompassMod.MIN_DISTANCE_LIMIT_BLOCKS, EntityCompassMod.MAX_DISTANCE_LIMIT_BLOCKS),
                this.width / 2,
                180,
                0xFFA0A0A0
        );

        super.render(context, mouseX, mouseY, deltaTicks);
        drawWatermark(context);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public void onGlobalSettingsSynced(boolean enabled, int blocks, boolean glintEnabled, int distanceDisplayMode) {
        distanceLimitEnabled = enabled;
        distanceLimitBlocks = EntityCompassMod.sanitizeDistanceLimitBlocks(blocks);
        this.glintEnabled = glintEnabled;
        this.distanceDisplayMode = EntityCompassMod.sanitizeDistanceDisplayMode(distanceDisplayMode);

        if (toggleButton != null) {
            toggleButton.setMessage(toggleButtonText());
        }

        if (glintButton != null) {
            glintButton.setMessage(glintButtonText());
        }

        if (distanceDisplayButton != null) {
            distanceDisplayButton.setMessage(distanceDisplayButtonText());
        }

        if (distanceField != null) {
            distanceField.setText(Integer.toString(distanceLimitBlocks));
        }
    }

    private void sendSettings() {
        distanceLimitBlocks = readDistanceLimit();
        EntityCompassClient.setGlobalSettings(distanceLimitEnabled, distanceLimitBlocks, glintEnabled, distanceDisplayMode);

        if (distanceField != null) {
            distanceField.setText(Integer.toString(distanceLimitBlocks));
        }

        if (!ClientPlayNetworking.canSend(UpdateGlobalSettingsPayload.ID)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.entity_compass.server_missing"), true);
            }
            close();
            return;
        }

        ClientPlayNetworking.send(new UpdateGlobalSettingsPayload(
                distanceLimitEnabled,
                distanceLimitBlocks,
                glintEnabled,
                distanceDisplayMode
        ));
    }

    private int readDistanceLimit() {
        if (distanceField == null || distanceField.getText().isBlank()) {
            return EntityCompassMod.MIN_DISTANCE_LIMIT_BLOCKS;
        }

        try {
            return EntityCompassMod.sanitizeDistanceLimitBlocks(Integer.parseInt(distanceField.getText()));
        } catch (NumberFormatException ignored) {
            return distanceLimitBlocks;
        }
    }

    private Text toggleButtonText() {
        return Text.translatable(
                "screen.entity_compass.global_settings.distance_limit_enabled",
                Text.translatable(distanceLimitEnabled ? "screen.entity_compass.option.on" : "screen.entity_compass.option.off")
        );
    }

    private Text glintButtonText() {
        return Text.translatable(
                "screen.entity_compass.glint",
                Text.translatable(glintEnabled ? "screen.entity_compass.option.on" : "screen.entity_compass.option.off")
        );
    }

    private Text distanceDisplayButtonText() {
        return Text.translatable("screen.entity_compass.distance_display", distanceModeText(distanceDisplayMode));
    }

    private static Text distanceModeText(int mode) {
        return switch (EntityCompassMod.sanitizeDistanceDisplayMode(mode)) {
            case EntityCompassMod.DISTANCE_DISPLAY_OFF -> Text.translatable("screen.entity_compass.distance_display.off");
            case EntityCompassMod.DISTANCE_DISPLAY_ABOVE_CROSSHAIR -> Text.translatable("screen.entity_compass.distance_display.above_crosshair");
            case EntityCompassMod.DISTANCE_DISPLAY_BELOW_CROSSHAIR -> Text.translatable("screen.entity_compass.distance_display.below_crosshair");
            default -> Text.translatable("screen.entity_compass.distance_display.above_hotbar");
        };
    }

    private void drawWatermark(DrawContext context) {
        Text watermark = Text.literal(EntityCompassMod.WATERMARK);
        int x = this.width - this.textRenderer.getWidth(watermark) - 6;
        int y = this.height - 12;
        context.drawTextWithShadow(this.textRenderer, watermark, x, y, 0x99FFFFFF);
    }
}
