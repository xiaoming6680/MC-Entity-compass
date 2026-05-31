package com.csesp.entitycompass.network;

import com.csesp.entitycompass.EntityCompassMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record OpenTargetMenuPayload() implements CustomPayload {
    public static final OpenTargetMenuPayload INSTANCE = new OpenTargetMenuPayload();
    public static final Id<OpenTargetMenuPayload> ID = new Id<>(EntityCompassMod.id("open_target_menu"));
    public static final PacketCodec<RegistryByteBuf, OpenTargetMenuPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
