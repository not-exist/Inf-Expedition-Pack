package com.inf.expedition.network;

import com.inf.expedition.Expedition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapMoveResponsePayload(boolean ok, String message) implements CustomPacketPayload {
    public static final Type<MapMoveResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "map_move_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapMoveResponsePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, MapMoveResponsePayload::ok,
                    ByteBufCodecs.STRING_UTF8, MapMoveResponsePayload::message,
                    MapMoveResponsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
