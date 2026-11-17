package com.inf.expedition.world;

import com.inf.expedition.Expedition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class DimensionIds {
    public static final ResourceLocation HUB =
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "hub");
    public static final ResourceLocation RUN =
            ResourceLocation.fromNamespaceAndPath(Expedition.MODID, "run");
    public static final ResourceKey<Level> HUB_KEY = ResourceKey.create(Registries.DIMENSION, HUB);
    public static final ResourceKey<Level> RUN_KEY = ResourceKey.create(Registries.DIMENSION, RUN);

    private DimensionIds() {
    }
}
