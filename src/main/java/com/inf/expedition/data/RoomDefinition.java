package com.inf.expedition.data;

import net.minecraft.resources.ResourceLocation;

public record RoomDefinition(
        String id,
        RoomType type,
        int weight,
        ResourceLocation structure,
        ResourceLocation encounter,
        int minAct,
        int maxAct,
        String titleKey,
        ResourceLocation rewardLoot) {
}
