package com.inf.expedition.network;

import com.inf.expedition.Expedition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapOpenRequestPayload() implements CustomPacketPayload {
    public static final Type<MapOpenRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "map_open_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapOpenRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new MapOpenRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
