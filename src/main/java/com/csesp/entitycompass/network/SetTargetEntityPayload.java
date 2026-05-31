package com.csesp.entitycompass.network;

import com.csesp.entitycompass.EntityCompassMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetTargetEntityPayload(Identifier entityTypeId) implements CustomPayload {
    public static final Id<SetTargetEntityPayload> ID = new Id<>(EntityCompassMod.id("set_target_entity"));

    public static final PacketCodec<RegistryByteBuf, SetTargetEntityPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC,
            SetTargetEntityPayload::entityTypeId,
            SetTargetEntityPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
