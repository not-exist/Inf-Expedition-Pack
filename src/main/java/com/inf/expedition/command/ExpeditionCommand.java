package com.inf.expedition.command;

import com.inf.expedition.ExpeditionConfig;
import com.inf.expedition.data.ExpeditionData;
import com.inf.expedition.data.RoomType;
import com.inf.expedition.data.RoomDefinition;
import com.inf.expedition.map.MapGraph;
import com.inf.expedition.network.ExpeditionNetwork;
import com.inf.expedition.room.RoomInstance;
import com.inf.expedition.room.RoomManager;
import com.inf.expedition.room.RoomPlacer;
import com.inf.expedition.run.ExpeditionSavedData;
import com.inf.expedition.run.RunManager;
import com.inf.expedition.run.RunState;
import com.inf.expedition.world.DimensionIds;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ExpeditionCommand {
    private ExpeditionCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("infexpedition")
                .then(Commands.literal("start")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            MinecraftServer server = player.getServer();
                            if (server != null) {
                                RunManager.start(server, server.getPlayerList().getPlayers());
                            }
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            ExpeditionSavedData.get(server).resetRun();
                            RoomManager.clear(server);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Inf Expedition reset"), true);
                            return 1;
                        }))
                .then(Commands.literal("move")
                        .then(Commands.argument("node", IntegerArgumentType.integer())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    RunManager.move(player,
                                            IntegerArgumentType.getInteger(context, "node"));
                                    return 1;
                                })))
                .then(Commands.literal("map")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ExpeditionNetwork.sendMap(player);
                            return 1;
                        }))
                .then(Commands.literal("complete")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            RunManager.complete(context.getSource().getServer(), true);
                            return 1;
                        }))
                .then(Commands.literal("info")
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            ExpeditionSavedData data = ExpeditionSavedData.get(server);
                            RunState run = data.run();
                            MapGraph map = data.map();
                            String room = RoomManager.current() == null
                                    ? "none" : RoomManager.current().definitionId();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Inf Expedition: status=" + run.status()
                                            + " seed=" + run.seed()
                                            + " node=" + run.currentNodeId()
                                            + " nodes=" + (map == null ? 0 : map.nodeList().size())
                                            + " party=" + run.party().size()
                                            + " room=" + room
                                            + " catalog=" + ExpeditionData.catalog().rooms().size()
                                            + " floors=" + ExpeditionData.mapConfig().bossFloor()), false);
                            return 1;
                        }))
                .then(Commands.literal("debugplace")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("structure", StringArgumentType.greedyString())
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    ServerLevel level = server.getLevel(DimensionIds.RUN_KEY);
                                    if (level == null) {
                                        context.getSource().sendFailure(
                                                Component.literal("Run dimension is not loaded"));
                                        return 0;
                                    }
                                    String raw = StringArgumentType.getString(context, "structure");
                                    ResourceLocation id = raw.contains(":")
                                            ? ResourceLocation.tryParse(raw)
                                            : ResourceLocation.tryParse("inf_expedition:" + raw);
                                    if (id == null) {
                                        context.getSource().sendFailure(
                                                Component.literal("Invalid structure id"));
                                        return 0;
                                    }
                                    RoomDefinition definition = new RoomDefinition(
                                            id.toString(), RoomType.BATTLE, 1, id, null,
                                            1, 1, "room.debug", null);
                                    var placed = RoomPlacer.place(level, definition, -1,
                                            new BlockPos(0, ExpeditionConfig.roomCenterY(), 0));
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Placed: " + placed.isPresent()), false);
                                    return placed.isPresent() ? 1 : 0;
                                }))));
    }
}
