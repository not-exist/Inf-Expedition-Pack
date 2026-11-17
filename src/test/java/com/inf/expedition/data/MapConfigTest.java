package com.inf.expedition.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inf.expedition.map.MapConfig;
import com.inf.expedition.map.NodeType;
import org.junit.jupiter.api.Test;

class MapConfigTest {
    @Test
    void readsJson() {
        MapConfig config = MapConfig.fromJson("""
                {"acts":1,"floors":8,"columns":3,"paths":2,"boss_floor":8,
                 "elite_min_floor":3,"shop_min_floor":2,"forge_min_floor":2,
                 "guarantee_rest_before_boss":true,
                 "weights":{"battle":40,"event":20,"forge":14,"rest":12,"shop":8,"elite":6}}
                """);
        assertEquals(8, config.bossFloor());
        assertEquals(3, config.columns());
        assertEquals(40, config.weight(NodeType.BATTLE));
    }

    @Test
    void badJsonFallsBackToDefault() {
        assertEquals(MapConfig.DEFAULT, MapConfig.fromJson("{"));
    }
}
