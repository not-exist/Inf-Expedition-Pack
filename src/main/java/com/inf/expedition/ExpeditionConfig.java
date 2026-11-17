package com.inf.expedition;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ExpeditionConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue ROOM_CENTER_Y =
            BUILDER.comment("Y level at which room templates are centered.")
                    .defineInRange("roomCenterY", 1, -64, 320);
    private static final ModConfigSpec.BooleanValue CLEAR_INVENTORY_ON_RUN_END =
            BUILDER.comment("Clear the player inventory when a run ends or a new run starts.")
                    .define("clearInventoryOnRunEnd", true);
    private static final ModConfigSpec.BooleanValue ENABLE_COOP =
            BUILDER.comment("Enable the shared-party co-op path in v0.1.")
                    .define("enableCoop", false);
    private static final ModConfigSpec.IntValue MAX_MOBS_PER_WAVE =
            BUILDER.comment("Hard cap for a single encounter wave.")
                    .defineInRange("maxMobsPerWave", 16, 1, 64);
    private static final ModConfigSpec.BooleanValue BOSS_BAR =
            BUILDER.comment("Show a boss bar for boss encounters.")
                    .define("bossBar", true);
    private static final ModConfigSpec.BooleanValue DEBUG =
            BUILDER.comment("Enable debug commands and log messages.")
                    .define("debug", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ExpeditionConfig() {
    }

    public static int roomCenterY() {
        return ROOM_CENTER_Y.get();
    }

    public static boolean clearInventoryOnRunEnd() {
        return CLEAR_INVENTORY_ON_RUN_END.get();
    }

    public static boolean enableCoop() {
        return ENABLE_COOP.get();
    }

    public static int maxMobsPerWave() {
        return MAX_MOBS_PER_WAVE.get();
    }

    public static boolean bossBar() {
        return BOSS_BAR.get();
    }

    public static boolean debug() {
        return DEBUG.get();
    }
}
