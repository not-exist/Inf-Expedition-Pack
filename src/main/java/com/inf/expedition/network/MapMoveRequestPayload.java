package com.inf.expedition.network;

import com.inf.expedition.Expedition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapMoveRequestPayload(int nodeId) implements CustomPacketPayload {
    public static final Type<MapMoveRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "map_move_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapMoveRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MapMoveRequestPayload::nodeId,
                    MapMoveRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
