package com.csesp.entitycompass.network;

import com.csesp.entitycompass.EntityCompassMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record UpdateGlobalSettingsPayload(
        boolean distanceLimitEnabled,
        int distanceLimitBlocks,
        boolean glintEnabled,
        int distanceDisplayMode
) implements CustomPayload {
    public static final Id<UpdateGlobalSettingsPayload> ID = new Id<>(EntityCompassMod.id("update_global_settings"));

    public static final PacketCodec<RegistryByteBuf, UpdateGlobalSettingsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            UpdateGlobalSettingsPayload::distanceLimitEnabled,
            PacketCodecs.VAR_INT,
            UpdateGlobalSettingsPayload::distanceLimitBlocks,
            PacketCodecs.BOOLEAN,
            UpdateGlobalSettingsPayload::glintEnabled,
            PacketCodecs.VAR_INT,
            UpdateGlobalSettingsPayload::distanceDisplayMode,
            UpdateGlobalSettingsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
