package com.inf.expedition.room;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class RoomExitHandler {
    private RoomExitHandler() {
    }

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RoomInstance room = RoomManager.current();
        if (room == null || !room.markers().exits().contains(event.getPos())) {
            return;
        }
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.LODESTONE)) {
            return;
        }
        RoomManager.exitRoom(player);
        event.setCanceled(true);
    }
}
