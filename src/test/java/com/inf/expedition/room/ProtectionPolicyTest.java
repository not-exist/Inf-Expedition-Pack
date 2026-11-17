package com.inf.expedition.room;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ProtectionPolicyTest {
    @Test
    void onlyForgeTableIsWhitelisted() {
        assertTrue(RoomProtection.isAllowedBlockId(ResourceLocation.parse("inf:forge_table")));
        assertFalse(RoomProtection.isAllowedBlockId(ResourceLocation.parse("minecraft:stone")));
        assertFalse(RoomProtection.isAllowedBlockId(ResourceLocation.parse("minecraft:chest")));
    }
}
