package com.inf.expedition.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class EncounterPlan {
    private EncounterPlan() {
    }

    public static List<WavePlan> build(
            EncounterDefinition definition,
            int act,
            int partySize,
            int maxMobs) {
        List<WavePlan> waves = new ArrayList<>();
        for (EncounterDefinition.Wave wave : definition.waves()) {
            List<MobPlan> mobs = new ArrayList<>();
            for (EncounterDefinition.MobSpawn spawn : wave.mobs()) {
                int count = EncounterScaler.scaledCount(
                        spawn.count(), act, definition.scale().countPerAct(), maxMobs);
                if (count > 0) {
                    mobs.add(new MobPlan(spawn.entity(), count));
                }
            }
            waves.add(new WavePlan(List.copyOf(mobs)));
        }
        return List.copyOf(waves);
    }

    public record WavePlan(List<MobPlan> mobs) {
        public WavePlan {
            mobs = List.copyOf(mobs);
        }
    }

    public record MobPlan(ResourceLocation entity, int count) {
    }
}
