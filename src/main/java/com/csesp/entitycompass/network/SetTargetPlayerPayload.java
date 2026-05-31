package com.csesp.entitycompass.network;

import com.csesp.entitycompass.EntityCompassMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record SetTargetPlayerPayload(UUID playerUuid) implements CustomPayload {
    public static final Id<SetTargetPlayerPayload> ID = new Id<>(EntityCompassMod.id("set_target_player"));

    public static final PacketCodec<RegistryByteBuf, SetTargetPlayerPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            SetTargetPlayerPayload::playerUuid,
            SetTargetPlayerPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
