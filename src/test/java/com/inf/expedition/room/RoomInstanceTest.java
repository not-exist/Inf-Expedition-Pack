package com.inf.expedition.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inf.expedition.data.RoomType;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class RoomInstanceTest {
    @Test
    void containsAndCenter() {
        RoomInstance room = new RoomInstance(
                UUID.randomUUID(),
                1,
                RoomType.BATTLE,
                "x",
                ResourceLocation.parse("inf_expedition:rooms/battle/x"),
                new AABB(0, 0, 0, 10, 5, 10),
                RoomMarkers.EMPTY);
        assertTrue(room.contains(new BlockPos(5, 2, 5)));
        assertFalse(room.contains(new BlockPos(11, 2, 5)));
        assertEquals(5.0, room.center().x, 1e-9);
        assertEquals(2.5, room.center().y, 1e-9);
    }
}
