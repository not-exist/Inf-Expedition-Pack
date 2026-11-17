package com.inf.expedition.network;

import com.inf.expedition.Expedition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record MapSyncPayload(
        CompoundTag map,
        int current,
        int act,
        String status,
        boolean canMove) implements CustomPacketPayload {

    public static final Type<MapSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "map_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, MapSyncPayload::map,
                    ByteBufCodecs.VAR_INT, MapSyncPayload::current,
                    ByteBufCodecs.VAR_INT, MapSyncPayload::act,
                    ByteBufCodecs.STRING_UTF8, MapSyncPayload::status,
                    ByteBufCodecs.BOOL, MapSyncPayload::canMove,
                    MapSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
