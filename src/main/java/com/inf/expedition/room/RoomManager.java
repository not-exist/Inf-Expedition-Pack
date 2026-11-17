package com.inf.expedition.room;

import com.inf.expedition.ExpeditionConfig;
import com.inf.expedition.advancement.ExpeditionAdvancements;
import com.inf.expedition.data.EncounterDefinition;
import com.inf.expedition.data.ExpeditionData;
import com.inf.expedition.data.RoomDefinition;
import com.inf.expedition.data.RoomType;
import com.inf.expedition.event.RoomCompletedEvent;
import com.inf.expedition.event.RoomEnteredEvent;
import com.inf.expedition.map.MapGraph;
import com.inf.expedition.map.MapNode;
import com.inf.expedition.run.ExpeditionSavedData;
import com.inf.expedition.run.RunState;
import com.inf.expedition.world.DimensionIds;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoomManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomManager.class);
    private static RoomInstance current;

    private RoomManager() {
    }

    public static RoomInstance current() {
        return current;
    }

    public static boolean enter(MinecraftServer server, RunState run, MapGraph graph, int nodeId) {
        ServerLevel level = server.getLevel(DimensionIds.RUN_KEY);
        if (level == null) {
            LOGGER.warn("Run dimension is not loaded");
            return false;
        }
        MapNode node = graph.node(nodeId);
        if (node == null) {
            LOGGER.warn("Cannot enter missing node {}", nodeId);
            return false;
        }
        RoomType type = RoomType.forNode(node.type());
        Optional<RoomDefinition> picked = ExpeditionData.catalog()
                .pick(type, 1, new Random(level.random.nextLong()));
        if (picked.isEmpty()) {
            LOGGER.warn("No room definition for type {} at act 1", type);
            return false;
        }
        RoomDefinition definition = picked.get();
        if (level.getStructureManager().get(definition.structure()).isEmpty()) {
            LOGGER.warn("Missing structure {} for room {}", definition.structure(), definition.id());
            return false;
        }
        Optional<EncounterDefinition> encounter = definition.encounter() == null
                ? Optional.empty()
                : ExpeditionData.catalog().encounter(definition.encounter());
        if (type.nodeType().isCombat() && encounter.isEmpty()) {
            LOGGER.warn("Combat room {} has no resolvable encounter", definition.id());
            return false;
        }
        clear(server);
        Optional<RoomInstance> placed = RoomPlacer.place(
                level,
                definition,
                nodeId,
                new BlockPos(0, ExpeditionConfig.roomCenterY(), 0));
        if (placed.isEmpty()) {
            LOGGER.warn("Failed to place room {}", definition.id());
            return false;
        }
        current = placed.get();
        ExpeditionSavedData.get(server).setRoomBounds(current.bounds(), current.id());
        teleportParty(server, level, run, current);
        for (UUID playerId : run.party()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                if (player.isSpectator()) {
                    player.setGameMode(GameType.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                }
                NeoForge.EVENT_BUS.post(new RoomEnteredEvent(player, current));
            }
        }
        if (current.type() == RoomType.START) {
            current.markCompleted();
            run.completeCurrent(graph);
            NeoForge.EVENT_BUS.post(new RoomCompletedEvent(server, current, true));
            if (definition.rewardLoot() != null) {
                EncounterManager.spawnRewardChest(level, current, definition.rewardLoot());
            }
        } else if (encounter.isPresent()) {
            List<ServerPlayer> party = run.party().stream()
                    .map(server.getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            EncounterManager.start(level, current, encounter.orElseThrow(), party);
        } else if (definition.rewardLoot() != null) {
            EncounterManager.spawnRewardChest(level, current, definition.rewardLoot());
        }
        return true;
    }

    public static void complete(MinecraftServer server, boolean success) {
        RoomInstance room = current;
        if (room == null || room.completed()) {
            return;
        }
        room.markCompleted();
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        MapGraph map = data.map();
        if (map != null) {
            run.completeCurrent(map);
        }
        data.setDirty();
        NeoForge.EVENT_BUS.post(new RoomCompletedEvent(server, room, success));
        List<ServerPlayer> party = data.run().party().stream()
                .map(server.getPlayerList()::getPlayer)
                .filter(java.util.Objects::nonNull)
                .toList();
        ExpeditionAdvancements.awardParty(party, "first_room");
        if (room.type() == RoomType.BOSS) {
            ExpeditionAdvancements.awardParty(party, "first_boss");
        }
        EncounterManager.clear();
    }

    public static void clear(MinecraftServer server) {
        ServerLevel level = server.getLevel(DimensionIds.RUN_KEY);
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        if (level != null) {
            if (current != null) {
                RoomPlacer.clear(level, current);
            } else if (data.roomBounds() != null) {
                RoomPlacer.clear(level, data.roomBounds());
            }
        }
        EncounterManager.clear();
        current = null;
        data.clearRoomBounds();
    }

    public static void exitRoom(ServerPlayer player) {
        RoomInstance room = current;
        if (room == null) {
            return;
        }
        if (room.completed()) {
            player.displayClientMessage(
                    Component.translatable("gui.inf_expedition.room.completed"), true);
            return;
        }
        if (room.type().nodeType().isCombat() || EncounterManager.isActive()) {
            player.displayClientMessage(Component.translatable("gui.inf_expedition.map.locked"), true);
            return;
        }
        if (player.getServer() != null) {
            complete(player.getServer(), true);
        }
    }

    public static void tick(MinecraftServer server) {
    }

    private static void teleportParty(
            MinecraftServer server,
            ServerLevel level,
            RunState run,
            RoomInstance room) {
        List<BlockPos> spawns = room.markers().spawns();
        BlockPos fallback = new BlockPos(0, ExpeditionConfig.roomCenterY(), 0);
        int index = 0;
        for (UUID playerId : run.party()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            BlockPos spawn = spawns.isEmpty() ? fallback : spawns.get(index % spawns.size());
            player.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            index++;
        }
    }
}
