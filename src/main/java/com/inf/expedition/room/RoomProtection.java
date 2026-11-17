package com.inf.expedition.room;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.inf.expedition.world.DimensionIds;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class RoomProtection {
    private static final ResourceLocation FORGE_TABLE =
            ResourceLocation.fromNamespaceAndPath("inf", "forge_table");

    private RoomProtection() {
    }

    public static boolean isAllowedBlockId(ResourceLocation id) {
        return FORGE_TABLE.equals(id);
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        RoomInstance room = RoomManager.current();
        if (room == null || event.getPlayer().isCreative() || !room.contains(event.getPos())) {
            return;
        }
        if (event.getLevel() instanceof Level level
                && !level.dimension().equals(DimensionIds.RUN_KEY)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (isAllowedBlockId(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
            return;
        }
        event.setCanceled(true);
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        RoomInstance room = RoomManager.current();
        if (room == null || !room.contains(event.getPos())) {
            return;
        }
        if (event.getLevel() instanceof Level level
                && !level.dimension().equals(DimensionIds.RUN_KEY)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(placed.getBlock());
        if (isAllowedBlockId(id)) {
            return;
        }
        event.setCanceled(true);
    }

    public static void onExplosion(ExplosionEvent.Start event) {
        if (event.getLevel().dimension().equals(DimensionIds.RUN_KEY)
                && RoomManager.current() != null) {
            event.setCanceled(true);
        }
    }

    public static void onMobGriefing(EntityMobGriefingEvent event) {
        event.setCanGrief(false);
    }

    public static void onDestroyBlock(LivingDestroyBlockEvent event) {
        if (event.getEntity().level().dimension().equals(DimensionIds.RUN_KEY)
                && RoomManager.current() != null) {
            event.setCanceled(true);
        }
    }
}
