package com.csesp.entitycompass.client;

import com.mojang.authlib.GameProfile;
import com.csesp.entitycompass.EntityCompassMod;
import com.csesp.entitycompass.network.SetTargetEntityPayload;
import com.csesp.entitycompass.network.SetTargetPlayerPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EntityCompassScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int SIDE_MARGIN = 28;
    private static final int TOP = 58;
    private static final int BOTTOM_CONTROLS_HEIGHT = 30;

    private final List<TargetEntry> entries;
    private String query = "";
    private int page;

    public EntityCompassScreen() {
        super(Text.translatable("screen.entity_compass.title"));
        this.entries = createEntries();
    }

    @Override
    protected void init() {
        TextFieldWidget searchField = new TextFieldWidget(
                this.textRenderer,
                SIDE_MARGIN,
                30,
                this.width - SIDE_MARGIN * 2,
                20,
                Text.translatable("screen.entity_compass.search")
        );
        searchField.setPlaceholder(Text.translatable("screen.entity_compass.search"));
        searchField.setText(query);
        searchField.setCursorToEnd(false);
        searchField.setChangedListener(value -> {
            query = value;
            page = 0;
            clearAndInit();
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);

        List<TargetEntry> filtered = filteredEntries();
        int columns = getColumnCount();
        int rows = getRowCount();
        int pageSize = columns * rows;
        int pageCount = getPageCount(filtered.size(), pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));

        int buttonWidth = getButtonWidth(columns);
        int gridWidth = columns * buttonWidth + (columns - 1) * BUTTON_GAP;
        int startX = (this.width - gridWidth) / 2;
        int startIndex = page * pageSize;
        int endIndex = Math.min(filtered.size(), startIndex + pageSize);

        for (int index = startIndex; index < endIndex; index++) {
            TargetEntry entry = filtered.get(index);
            int localIndex = index - startIndex;
            int x = startX + (localIndex % columns) * (buttonWidth + BUTTON_GAP);
            int y = TOP + (localIndex / columns) * (BUTTON_HEIGHT + BUTTON_GAP);
            addDrawableChild(ButtonWidget.builder(entry.name(), button -> selectTarget(entry))
                    .dimensions(x, y, buttonWidth, BUTTON_HEIGHT)
                    .build());
        }

        int bottomY = this.height - BOTTOM_CONTROLS_HEIGHT;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.entity_compass.previous"), button -> {
                    page = Math.max(0, page - 1);
                    clearAndInit();
                })
                .dimensions(SIDE_MARGIN, bottomY, 80, BUTTON_HEIGHT)
                .build()).active = page > 0;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.entity_compass.close"), button -> close())
                .dimensions(this.width / 2 - 45, bottomY, 90, BUTTON_HEIGHT)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.entity_compass.next"), button -> {
                    page = Math.min(pageCount - 1, page + 1);
                    clearAndInit();
                })
                .dimensions(this.width - SIDE_MARGIN - 80, bottomY, 80, BUTTON_HEIGHT)
                .build()).active = page + 1 < pageCount;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, 0x90000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFFFF);

        List<TargetEntry> filtered = filteredEntries();
        int pageSize = getColumnCount() * getRowCount();
        int pageCount = getPageCount(filtered.size(), pageSize);

        if (filtered.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.entity_compass.empty"),
                    this.width / 2,
                    this.height / 2,
                    0xFFA0A0A0
            );
        } else {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.entity_compass.page", page + 1, pageCount),
                    this.width / 2,
                    this.height - 48,
                    0xFFA0A0A0
            );
        }

        super.render(context, mouseX, mouseY, deltaTicks);
        drawWatermark(context);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void selectTarget(TargetEntry entry) {
        if (entry.kind() == TargetKind.PLAYER) {
            selectPlayerTarget(entry);
            return;
        }

        Identifier entityTypeId = entry.entityTypeId();
        if (entityTypeId != null) {
            selectEntityTarget(entityTypeId);
        }
    }

    private void selectEntityTarget(Identifier entityTypeId) {
        if (!ClientPlayNetworking.canSend(SetTargetEntityPayload.ID)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.entity_compass.server_missing"), true);
            }
            close();
            return;
        }

        ClientPlayNetworking.send(new SetTargetEntityPayload(entityTypeId));
        close();
    }

    private void selectPlayerTarget(TargetEntry entry) {
        UUID playerUuid = entry.playerUuid();
        if (playerUuid == null) {
            close();
            return;
        }

        if (!ClientPlayNetworking.canSend(SetTargetPlayerPayload.ID)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.entity_compass.server_missing"), true);
            }
            close();
            return;
        }

        ClientPlayNetworking.send(new SetTargetPlayerPayload(playerUuid));
        close();
    }

    private List<TargetEntry> filteredEntries() {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return entries;
        }

        return entries.stream()
                .filter(entry -> entry.searchText().contains(normalizedQuery))
                .toList();
    }

    private int getColumnCount() {
        return Math.max(1, Math.min(4, (this.width - SIDE_MARGIN * 2) / 120));
    }

    private int getRowCount() {
        int availableHeight = this.height - TOP - BOTTOM_CONTROLS_HEIGHT - 26;
        return Math.max(1, availableHeight / (BUTTON_HEIGHT + BUTTON_GAP));
    }

    private int getButtonWidth(int columns) {
        int availableWidth = this.width - SIDE_MARGIN * 2 - (columns - 1) * BUTTON_GAP;
        return Math.max(80, availableWidth / columns);
    }

    private static int getPageCount(int entryCount, int pageSize) {
        return Math.max(1, (entryCount + pageSize - 1) / pageSize);
    }

    private static List<TargetEntry> createEntries() {
        List<TargetEntry> result = new ArrayList<>();
        result.addAll(createPlayerEntries());
        result.addAll(Registries.ENTITY_TYPE.stream()
                .filter(EntityCompassMod::isSelectableEntityType)
                .map(EntityCompassScreen::toTargetEntry)
                .sorted(Comparator.comparing(TargetEntry::searchText))
                .toList());
        return result;
    }

    private static List<TargetEntry> createPlayerEntries() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) {
            return List.of();
        }

        UUID ownUuid = client.player != null ? client.player.getUuid() : null;
        List<TargetEntry> playerEntries = new ArrayList<>();
        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            GameProfile profile = entry.getProfile();
            UUID uuid = profile.id();
            String name = profile.name();
            if (uuid == null || name == null || name.isBlank() || uuid.equals(ownUuid)) {
                continue;
            }

            playerEntries.add(toPlayerEntry(uuid, name));
        }

        playerEntries.sort(Comparator.comparing(TargetEntry::searchText));
        return playerEntries;
    }

    private static TargetEntry toPlayerEntry(UUID uuid, String name) {
        Text displayName = Text.translatable("screen.entity_compass.target.player", name);
        return new TargetEntry(TargetKind.PLAYER, null, uuid, displayName, normalize(name + " " + uuid + " player"));
    }

    private static TargetEntry toTargetEntry(EntityType<?> type) {
        Identifier id = Registries.ENTITY_TYPE.getId(type);
        Text name = type.getName();
        return new TargetEntry(TargetKind.ENTITY, id, null, name, normalize(name.getString() + " " + id));
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private void drawWatermark(DrawContext context) {
        Text watermark = Text.literal(EntityCompassMod.WATERMARK);
        int x = this.width - this.textRenderer.getWidth(watermark) - 6;
        int y = this.height - 12;
        context.drawTextWithShadow(this.textRenderer, watermark, x, y, 0x99FFFFFF);
    }

    private enum TargetKind {
        ENTITY,
        PLAYER
    }

    private record TargetEntry(
            TargetKind kind,
            @Nullable Identifier entityTypeId,
            @Nullable UUID playerUuid,
            Text name,
            String searchText
    ) {
    }
}
