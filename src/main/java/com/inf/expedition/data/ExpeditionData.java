package com.inf.expedition.data;

import com.inf.expedition.map.MapConfig;

public final class ExpeditionData {
    private static volatile MapConfig mapConfig = MapConfig.DEFAULT;
    private static volatile RoomCatalog catalog = RoomCatalog.empty();

    private ExpeditionData() {
    }

    public static MapConfig mapConfig() {
        return mapConfig;
    }

    public static RoomCatalog catalog() {
        return catalog;
    }

    public static void set(MapConfig config, RoomCatalog roomCatalog) {
        mapConfig = config;
        catalog = roomCatalog;
    }
}
