package com.inf.expedition.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class ExpeditionSavedDataTest {
    @Test
    void pendingWipeIsConsumedOnce() {
        ExpeditionSavedData data = new ExpeditionSavedData();
        UUID player = UUID.randomUUID();
        data.queueWipe(player);
        assertTrue(data.hasPendingWipe(player));
        assertTrue(data.consumeWipe(player));
        assertFalse(data.consumeWipe(player));
    }

    @Test
    void pendingWipeAndRoomBoundsSurviveSaveLoad() {
        ExpeditionSavedData data = new ExpeditionSavedData();
        UUID player = UUID.randomUUID();
        data.queueWipe(player);
        data.setRoomBounds(new AABB(0, 1, 0, 24, 8, 24), UUID.randomUUID());
        CompoundTag tag = data.save(new CompoundTag(), RegistryAccess.EMPTY);
        ExpeditionSavedData loaded = ExpeditionSavedData.load(tag, RegistryAccess.EMPTY);
        assertTrue(loaded.hasPendingWipe(player));
        assertEquals(24.0, loaded.roomBounds().maxX, 1e-9);
        assertEquals(8.0, loaded.roomBounds().maxY, 1e-9);
    }

    @Test
    void runningRunIsResetAndPartyQueuedOnLoad() {
        UUID player = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        tag.putString("status", "RUNNING");
        tag.putLong("seed", 1L);
        tag.putInt("current", 3);
        ListTag party = new ListTag();
        party.add(StringTag.valueOf(player.toString()));
        tag.put("party", party);
        ExpeditionSavedData loaded = ExpeditionSavedData.load(tag, RegistryAccess.EMPTY);
        assertEquals(RunStatus.IDLE, loaded.run().status());
        assertTrue(loaded.hasPendingWipe(player));
    }
}
