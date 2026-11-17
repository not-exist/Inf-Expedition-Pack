package com.inf.expedition.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class MapKeyMapping {
    public static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.inf_expedition.map",
            InputConstants.KEY_M,
            "key.categories.inf_expedition");

    private MapKeyMapping() {
    }
}
