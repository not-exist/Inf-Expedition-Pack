package com.inf.expedition.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EncounterFallbackTest {
    @Test
    void fallbackEntitiesAreVanillaAndHostile() {
        ResourceLocation normal = EncounterFallback.forWave(false);
        ResourceLocation boss = EncounterFallback.forWave(true);
        assertEquals("minecraft", normal.getNamespace());
        assertEquals("minecraft", boss.getNamespace());
        assertTrue(EncounterFallback.countForWave(false) >= 1);
        assertTrue(EncounterFallback.countForWave(true) >= 1);
    }
}
