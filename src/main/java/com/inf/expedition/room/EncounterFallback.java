package com.inf.expedition.room;

import net.minecraft.resources.ResourceLocation;

public final class EncounterFallback {
    private static final ResourceLocation NORMAL = ResourceLocation.parse("minecraft:zombie");
    private static final ResourceLocation BOSS = ResourceLocation.parse("minecraft:ravager");

    private EncounterFallback() {
    }

    public static ResourceLocation forWave(boolean boss) {
        return boss ? BOSS : NORMAL;
    }

    public static int countForWave(boolean boss) {
        return boss ? 1 : 3;
    }
}
