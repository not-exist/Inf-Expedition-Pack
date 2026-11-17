package com.inf.expedition.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EncounterScalerTest {
    private static final EncounterDefinition.Scale S =
            new EncounterDefinition.Scale(0.25, 0.15, 0.2, 0.5, 0.25);

    @Test
    void actAndPartyScale() {
        assertEquals(1.0, EncounterScaler.healthMultiplier(1, 1, S), 1e-9);
        assertEquals(1.25, EncounterScaler.healthMultiplier(2, 1, S), 1e-9);
        assertEquals(1.875, EncounterScaler.healthMultiplier(2, 2, S), 1e-9);
        assertEquals(1.4375, EncounterScaler.damageMultiplier(2, 2, S), 1e-9);
    }

    @Test
    void countScalesAndCaps() {
        assertEquals(1, EncounterScaler.scaledCount(1, 1, 0.2, 16));
        assertEquals(2, EncounterScaler.scaledCount(1, 3, 0.2, 16));
        assertEquals(16, EncounterScaler.scaledCount(100, 1, 0.2, 16));
    }
}
