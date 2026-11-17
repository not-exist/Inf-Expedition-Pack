package com.inf.expedition.run;

import com.inf.expedition.ExpeditionConfig;
import com.inf.expedition.advancement.ExpeditionAdvancements;
import com.inf.expedition.data.ExpeditionData;
import com.inf.expedition.event.RoomEnteredEvent;
import com.inf.expedition.event.RunEndedEvent;
import com.inf.expedition.event.RunStartedEvent;
import com.inf.expedition.map.MapConfig;
import com.inf.expedition.map.MapGenerator;
import com.inf.expedition.map.MapGraph;
import com.inf.expedition.map.MapNode;
import com.inf.expedition.map.NodeType;
import com.inf.expedition.network.ExpeditionNetwork;
import com.inf.expedition.registry.ModItems;
import com.inf.expedition.room.EncounterManager;
import com.inf.expedition.room.RoomInstance;
import com.inf.expedition.room.RoomManager;
import com.inf.expedition.world.DimensionIds;
import com.inf.expedition.world.HubManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class RunManager {
    private static final List<String> BLANK_IDS = List.of(
            "sword_blank", "pickaxe_blank", "axe_blank", "scythe_blank", "dagger_blank");

    private RunManager() {
    }

    public static void start(MinecraftServer server, List<ServerPlayer> players) {
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        if (run.status() != RunStatus.IDLE) {
            players.forEach(player -> player.displayClientMessage(
                    Component.translatable("gui.inf_expedition.run.already_running"), false));
            return;
        }
        List<ServerPlayer> party = players.stream()
                .filter(player -> !player.isSpectator())
                .toList();
        if (party.isEmpty()) {
            players.forEach(player -> player.displayClientMessage(
                    Component.translatable("gui.inf_expedition.run.no_players"), false));
            return;
        }
        long seed = server.overworld().getRandom().nextLong();
        MapConfig config = ExpeditionData.mapConfig();
        MapGraph map = new MapGenerator(seed).generate(config);
        run.begin(seed, map, party.stream().map(ServerPlayer::getUUID).toList());
        data.setMap(map);
        data.setRun(run);
        Random random = new Random(seed);
        for (ServerPlayer player : party) {
            if (ExpeditionConfig.clearInventoryOnRunEnd()) {
                player.getInventory().clearContent();
            }
            giveBaseKit(player, random);
            player.setHealth(player.getMaxHealth());
        }
        if (!RoomManager.enter(server, run, map, map.startId())) {
            end(server, false);
            return;
        }
        NeoForge.EVENT_BUS.post(new RunStartedEvent(server, party));
        ExpeditionAdvancements.awardParty(party, "first_run");
        party.forEach(player -> player.displayClientMessage(
                Component.translatable("gui.inf_expedition.run.started"), false));
        sendMapToParty(server, run);
    }

    public static void move(ServerPlayer player, int nodeId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        MapGraph map = data.map();
        if (map == null) {
            return;
        }
        RunLifecycle.MoveResult result = RunLifecycle.validateMove(
                run, map, player.getUUID(), nodeId);
        if (result != RunLifecycle.MoveResult.OK) {
            player.displayClientMessage(
                    Component.translatable("gui.inf_expedition.map.cannot_move"), true);
            return;
        }
        if (!RoomManager.enter(server, run, map, nodeId)) {
            player.displayClientMessage(
                    Component.translatable("gui.inf_expedition.room.invalid"), true);
            end(server, false);
            return;
        }
        if (!run.moveTo(map, nodeId)) {
            end(server, false);
            return;
        }
        data.setDirty();
        sendMapToParty(server, run);
    }

    public static void complete(MinecraftServer server, boolean success) {
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        MapGraph map = data.map();
        if (map == null) {
            return;
        }
        MapNode node = map.node(run.currentNodeId());
        RoomManager.complete(server, success);
        sendMapToParty(server, run);
        if (success && node != null && node.type() == NodeType.BOSS) {
            end(server, true);
        }
    }

    public static void leave(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        if (!run.isPartyMember(player.getUUID())) {
            return;
        }
        if (ExpeditionConfig.clearInventoryOnRunEnd()) {
            player.getInventory().clearContent();
        }
        run.removeMember(player.getUUID());
        data.setDirty();
        HubManager.teleportToHub(player);
        if (RunLifecycle.shouldEnd(run.party())) {
            end(server, false);
        }
    }

    public static void end(MinecraftServer server, boolean success) {
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        if (run.status() != RunStatus.RUNNING) {
            return;
        }
        for (UUID playerId : run.party()) {
            data.queueWipe(playerId);
        }
        run.finish(success);
        data.setDirty();
        List<ServerPlayer> party = new ArrayList<>();
        for (UUID playerId : run.party()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                party.add(player);
            }
        }
        NeoForge.EVENT_BUS.post(new RunEndedEvent(server, success, party));
        if (success) {
            ExpeditionAdvancements.awardParty(party, "clear_act1");
        }
        server.tell(new TickTask(server.getTickCount() + 60, () -> {
            for (ServerPlayer player : party) {
                if (ExpeditionConfig.clearInventoryOnRunEnd()) {
                    player.getInventory().clearContent();
                }
                data.consumeWipe(player.getUUID());
                HubManager.teleportToHub(player);
                player.setHealth(player.getMaxHealth());
            }
            RoomManager.clear(server);
            data.resetRun();
        }));
        party.forEach(player -> player.displayClientMessage(
                Component.translatable(success
                        ? "gui.inf_expedition.run.complete"
                        : "gui.inf_expedition.run.failed"), false));
    }

    public static void tick(MinecraftServer server) {
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        if (run.status() != RunStatus.RUNNING) {
            return;
        }
        boolean anyOnline = run.party().stream()
                .anyMatch(playerId -> server.getPlayerList().getPlayer(playerId) != null);
        if (!anyOnline) {
            end(server, false);
        }
        EncounterManager.tick(server);
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        RunState run = data.run();
        if (run.status() != RunStatus.RUNNING || !run.isPartyMember(player.getUUID())) {
            return;
        }
        if (!player.level().dimension().equals(DimensionIds.RUN_KEY)) {
            return;
        }
        boolean othersAlive = run.party().stream()
                .filter(playerId -> !playerId.equals(player.getUUID()))
                .map(playerId -> server.getPlayerList().getPlayer(playerId))
                .filter(Objects::nonNull)
                .anyMatch(other -> other.isAlive() && !other.isSpectator());
        if (othersAlive) {
            event.setCanceled(true);
            player.setHealth(1.0F);
            player.setGameMode(GameType.SPECTATOR);
            player.displayClientMessage(
                    Component.translatable("gui.inf_expedition.death.spectator"), false);
        } else {
            end(server, false);
        }
    }

    public static void teleportToCurrentRoom(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        RoomInstance room = RoomManager.current();
        ServerLevel level = server.getLevel(DimensionIds.RUN_KEY);
        if (room == null || level == null || room.markers().spawns().isEmpty()) {
            HubManager.teleportToHub(player);
            return;
        }
        var spawn = room.markers().spawns().getFirst();
        player.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    private static void sendMapToParty(MinecraftServer server, RunState run) {
        for (UUID playerId : run.party()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                ExpeditionNetwork.sendMap(player);
            }
        }
    }

    private static void giveBaseKit(ServerPlayer player, Random random) {
        give(player, ResourceLocation.fromNamespaceAndPath("inf", "hammer"), 1);
        String blankId = BLANK_IDS.get(random.nextInt(BLANK_IDS.size()));
        give(player, ResourceLocation.fromNamespaceAndPath("inf", blankId), 2);
        player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 8));
        player.getInventory().add(new ItemStack(ModItems.RECALL_SCROLL.get()));
        Item questBook = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("ftbquests", "book"));
        if (questBook != Items.AIR) {
            player.getInventory().add(new ItemStack(questBook));
        }
    }

    private static void give(ServerPlayer player, ResourceLocation id, int count) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item != Items.AIR) {
            player.getInventory().add(new ItemStack(item, count));
        }
    }
}
