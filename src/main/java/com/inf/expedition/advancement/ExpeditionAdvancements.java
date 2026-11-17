package com.inf.expedition.advancement;

import com.inf.expedition.Expedition;
import java.util.Collection;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ExpeditionAdvancements {
    private ExpeditionAdvancements() {
    }

    public static void award(ServerPlayer player, String path) {
        if (player.getServer() == null) {
            return;
        }
        AdvancementHolder holder = player.getServer()
                .getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Expedition.MODID, path));
        if (holder != null) {
            player.getAdvancements().award(holder, "unlocked");
        }
    }

    public static void awardParty(Collection<ServerPlayer> players, String path) {
        for (ServerPlayer player : players) {
            award(player, path);
        }
    }
}
