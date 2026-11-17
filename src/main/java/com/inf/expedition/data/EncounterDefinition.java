package com.inf.expedition.data;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record EncounterDefinition(
        ResourceLocation id,
        String titleKey,
        boolean boss,
        List<Wave> waves,
        Scale scale,
        ResourceLocation rewardLoot) {

    public EncounterDefinition {
        waves = List.copyOf(waves);
    }

    public record Wave(List<MobSpawn> mobs) {
        public Wave {
            mobs = List.copyOf(mobs);
        }
    }

    public record MobSpawn(ResourceLocation entity, int count) {
    }

    public record Scale(
            double healthPerAct,
            double damagePerAct,
            double countPerAct,
            double healthPerExtraPlayer,
            double damagePerExtraPlayer) {
        public static final Scale DEFAULT = new Scale(0.25, 0.15, 0.2, 0.5, 0.25);
    }
}
