package com.inf.expedition.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EncounterPlanTest {
    @Test
    void buildsScaledWaves() {
        EncounterDefinition definition = new EncounterDefinition(
                ResourceLocation.parse("inf_expedition:e"),
                "e",
                false,
                List.of(new EncounterDefinition.Wave(List.of(
                        new EncounterDefinition.MobSpawn(ResourceLocation.parse("minecraft:zombie"), 4)))),
                new EncounterDefinition.Scale(0.25, 0.15, 0.2, 0.5, 0.25),
                ResourceLocation.parse("inf_expedition:chests/x"));
        List<EncounterPlan.WavePlan> plan = EncounterPlan.build(definition, 2, 2, 16);
        assertEquals(1, plan.size());
        assertEquals(5, plan.getFirst().mobs().getFirst().count());
    }
}
