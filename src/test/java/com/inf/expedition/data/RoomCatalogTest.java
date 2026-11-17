package com.inf.expedition.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RoomCatalogTest {
    @Test
    void parsesRoomAndEncounter() {
        RoomCatalog catalog = RoomCatalog.fromJson(
                Map.of("battle/crypt", """
                        {"type":"battle","weight":10,
                         "structure":"inf_expedition:rooms/battle/crypt",
                         "encounter":"inf_expedition:battle/crypt",
                         "min_act":1,"max_act":1}
                        """),
                Map.of("battle/crypt", """
                        {"title":"encounter.inf_expedition.crypt","boss":false,
                         "waves":[{"mobs":[{"id":"minecraft:zombie","count":4}]}],
                         "scale":{"health_per_act":0.25,"damage_per_act":0.15,"count_per_act":0.2,
                                  "health_per_extra_player":0.5,"damage_per_extra_player":0.25},
                         "reward_loot":"inf_expedition:chests/battle_common"}
                        """));
        assertEquals(1, catalog.rooms().size());
        assertTrue(catalog.encounter(ResourceLocation.parse("inf_expedition:battle/crypt")).isPresent());
    }

    @Test
    void pickFiltersByTypeAndAct() {
        RoomCatalog catalog = RoomCatalog.fromJson(
                Map.of("battle/a", """
                        {"type":"battle","weight":1,
                         "structure":"inf_expedition:rooms/battle/a",
                         "min_act":2,"max_act":3}
                        """),
                Map.of());
        assertTrue(catalog.pick(RoomType.BATTLE, 1, new Random(1)).isEmpty());
        assertTrue(catalog.pick(RoomType.BATTLE, 2, new Random(1)).isPresent());
    }

    @Test
    void emptyEncounterIsSkipped() {
        RoomCatalog catalog = RoomCatalog.fromJson(Map.of(), Map.of(
                "battle/empty", "{\"waves\":[]}"));
        assertTrue(catalog.encounter(ResourceLocation.parse("inf_expedition:battle/empty")).isEmpty());
    }

    @Test
    void invalidTypeIsSkipped() {
        RoomCatalog catalog = RoomCatalog.fromJson(
                Map.of("bad", "{\"type\":\"nope\",\"weight\":1}"),
                Map.of());
        assertEquals(0, catalog.rooms().size());
    }
}
