package com.csesp.entitycompass.network;

import com.csesp.entitycompass.EntityCompassMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SyncGlobalSettingsPayload(
        boolean distanceLimitEnabled,
        int distanceLimitBlocks,
        boolean glintEnabled,
        int distanceDisplayMode
) implements CustomPayload {
    public static final Id<SyncGlobalSettingsPayload> ID = new Id<>(EntityCompassMod.id("sync_global_settings"));

    public static final PacketCodec<RegistryByteBuf, SyncGlobalSettingsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            SyncGlobalSettingsPayload::distanceLimitEnabled,
            PacketCodecs.VAR_INT,
            SyncGlobalSettingsPayload::distanceLimitBlocks,
            PacketCodecs.BOOLEAN,
            SyncGlobalSettingsPayload::glintEnabled,
            PacketCodecs.VAR_INT,
            SyncGlobalSettingsPayload::distanceDisplayMode,
            SyncGlobalSettingsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
