package com.inf.expedition.network;

import com.inf.expedition.map.MapGraph;
import com.inf.expedition.run.ExpeditionSavedData;
import com.inf.expedition.run.MapSnapshotNbt;
import com.inf.expedition.run.RunManager;
import com.inf.expedition.run.RunState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ExpeditionNetwork {
    private ExpeditionNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(MapSyncPayload.TYPE, MapSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    com.inf.expedition.client.MapClientData.update(payload);
                    com.inf.expedition.client.ExpeditionClient.openMap();
                }));
        registrar.playToClient(MapMoveResponsePayload.TYPE, MapMoveResponsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() != null) {
                        context.player().displayClientMessage(
                                net.minecraft.network.chat.Component.literal(payload.message()), true);
                    }
                }));
        registrar.playToServer(MapOpenRequestPayload.TYPE, MapOpenRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        sendMap(player);
                    }
                }));
        registrar.playToServer(MapMoveRequestPayload.TYPE, MapMoveRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        RunManager.move(player, payload.nodeId());
                    }
                }));
    }

    public static void sendMap(ServerPlayer player) {
        ExpeditionSavedData data = ExpeditionSavedData.get(player.getServer());
        RunGraph graph = new RunGraph(data.map(), data.run());
        if (graph.map() == null) {
            PacketDistributor.sendToPlayer(player,
                    new MapSyncPayload(new CompoundTag(), -1, 1, "IDLE", false));
            return;
        }
        CompoundTag tag = MapSnapshotNbt.write(graph.map().snapshot());
        boolean canMove = graph.run().status() == com.inf.expedition.run.RunStatus.RUNNING
                && com.inf.expedition.room.RoomManager.current() != null
                && com.inf.expedition.room.RoomManager.current().completed();
        PacketDistributor.sendToPlayer(player,
                new MapSyncPayload(tag, graph.run().currentNodeId(), 1,
                        graph.run().status().name(), canMove));
    }

    private record RunGraph(MapGraph map, RunState run) {
    }
}
