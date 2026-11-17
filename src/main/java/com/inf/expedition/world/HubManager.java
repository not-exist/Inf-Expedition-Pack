package com.inf.expedition.world;

import com.inf.expedition.Expedition;
import com.inf.expedition.ExpeditionConfig;
import com.inf.expedition.registry.ModBlocks;
import com.inf.expedition.run.ExpeditionSavedData;
import com.inf.expedition.run.RunManager;
import com.inf.expedition.run.RunState;
import com.inf.expedition.run.RunStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class HubManager {
    private HubManager() {
    }

    public static BlockPos hubSpawn() {
        return new BlockPos(0, 66, 0);
    }

    public static void ensurePlatform(MinecraftServer server) {
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        if (data.hubPlaced()) {
            return;
        }
        ServerLevel hub = server.getLevel(DimensionIds.HUB_KEY);
        if (hub == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "hub_platform");
        var template = hub.getStructureManager().get(id);
        if (template.isPresent()) {
            var size = template.get().getSize();
            BlockPos origin = new BlockPos(-size.getX() / 2, 64, -size.getZ() / 2);
            template.get().placeInWorld(hub, origin, origin, new StructurePlaceSettings(), hub.random, 2);
        } else {
            buildFallbackPlatform(hub);
        }
        data.setHubPlaced(true);
    }

    public static void teleportToHub(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ensurePlatform(server);
        ServerLevel hub = server.getLevel(DimensionIds.HUB_KEY);
        if (hub == null) {
            return;
        }
        BlockPos spawn = hubSpawn();
        player.teleportTo(hub, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            wipeIfPending(server, player);
            ensurePlatform(server);
            RunState run = ExpeditionSavedData.get(server).run();
            if (run.status() == RunStatus.RUNNING && run.isPartyMember(player.getUUID())) {
                RunManager.teleportToCurrentRoom(player);
            } else {
                teleportToHub(player);
            }
        });
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            wipeIfPending(server, player);
            RunState run = ExpeditionSavedData.get(server).run();
            if (run.status() == RunStatus.RUNNING && run.isPartyMember(player.getUUID())) {
                RunManager.teleportToCurrentRoom(player);
            } else {
                teleportToHub(player);
            }
        });
    }

    private static void wipeIfPending(MinecraftServer server, ServerPlayer player) {
        ExpeditionSavedData data = ExpeditionSavedData.get(server);
        if (data.consumeWipe(player.getUUID()) && ExpeditionConfig.clearInventoryOnRunEnd()) {
            player.getInventory().clearContent();
        }
    }

    private static void buildFallbackPlatform(ServerLevel hub) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                hub.setBlockAndUpdate(new BlockPos(x, 64, z),
                        Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
        }
        hub.setBlockAndUpdate(new BlockPos(0, 65, 0),
                ModBlocks.EXPEDITION_ALTAR.get().defaultBlockState());
        hub.setBlockAndUpdate(new BlockPos(2, 65, 0),
                ModBlocks.QUEST_BOARD.get().defaultBlockState());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                hub.setBlockAndUpdate(new BlockPos(x, 67, z), Blocks.AIR.defaultBlockState());
            }
        }
    }
}
