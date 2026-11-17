package com.inf.expedition.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.inf.expedition.Expedition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoomCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomCatalog.class);

    private final List<RoomDefinition> rooms;
    private final Map<ResourceLocation, EncounterDefinition> encounters;

    private RoomCatalog(List<RoomDefinition> rooms, Map<ResourceLocation, EncounterDefinition> encounters) {
        this.rooms = List.copyOf(rooms);
        this.encounters = Map.copyOf(encounters);
    }

    public static RoomCatalog empty() {
        return new RoomCatalog(List.of(), Map.of());
    }

    public static RoomCatalog fromJson(Map<String, String> roomJson, Map<String, String> encounterJson) {
        List<RoomDefinition> rooms = new ArrayList<>();
        for (Map.Entry<String, String> entry : roomJson.entrySet()) {
            try {
                rooms.add(parseRoom(entry.getKey(), entry.getValue()));
            } catch (RuntimeException exception) {
                LOGGER.warn("Skipping invalid room {}: {}", entry.getKey(), exception.toString());
            }
        }
        Map<ResourceLocation, EncounterDefinition> encounters = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : encounterJson.entrySet()) {
            try {
                EncounterDefinition definition = parseEncounter(entry.getKey(), entry.getValue());
                encounters.put(definition.id(), definition);
            } catch (RuntimeException exception) {
                LOGGER.warn("Skipping invalid encounter {}: {}", entry.getKey(), exception.toString());
            }
        }
        return new RoomCatalog(rooms, encounters);
    }

    public static RoomCatalog fromLists(
            Collection<RoomDefinition> rooms,
            Collection<EncounterDefinition> encounters) {
        Map<ResourceLocation, EncounterDefinition> byId = new LinkedHashMap<>();
        for (EncounterDefinition encounter : encounters) {
            byId.put(encounter.id(), encounter);
        }
        return new RoomCatalog(List.copyOf(rooms), byId);
    }

    public List<RoomDefinition> rooms() {
        return rooms;
    }

    public Optional<RoomDefinition> pick(RoomType type, int act, Random random) {
        List<RoomDefinition> candidates = rooms.stream()
                .filter(room -> room.type() == type)
                .filter(room -> act >= room.minAct() && act <= room.maxAct())
                .filter(room -> room.weight() > 0)
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        int total = candidates.stream().mapToInt(RoomDefinition::weight).sum();
        int roll = random.nextInt(total);
        for (RoomDefinition candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) {
                return Optional.of(candidate);
            }
        }
        return Optional.of(candidates.getLast());
    }

    public Optional<EncounterDefinition> encounter(ResourceLocation id) {
        return Optional.ofNullable(encounters.get(id));
    }

    private static RoomDefinition parseRoom(String key, String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        RoomType type = RoomType.byId(object.get("type").getAsString());
        String id = object.has("id") ? object.get("id").getAsString() : normalize(key).toString();
        ResourceLocation structure = ResourceLocation.parse(object.get("structure").getAsString());
        ResourceLocation encounter = object.has("encounter")
                ? ResourceLocation.parse(object.get("encounter").getAsString())
                : null;
        ResourceLocation rewardLoot = object.has("reward_loot")
                ? ResourceLocation.parse(object.get("reward_loot").getAsString())
                : null;
        String titleKey = object.has("title") ? object.get("title").getAsString() : "room." + id;
        return new RoomDefinition(
                id,
                type,
                intValue(object, "weight", 10),
                structure,
                encounter,
                intValue(object, "min_act", 1),
                intValue(object, "max_act", 1),
                titleKey,
                rewardLoot);
    }

    private static EncounterDefinition parseEncounter(String key, String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        ResourceLocation id = normalize(key);
        String titleKey = object.has("title")
                ? object.get("title").getAsString()
                : "encounter." + id.getPath();
        boolean boss = boolValue(object, "boss", false);
        List<EncounterDefinition.Wave> waves = new ArrayList<>();
        if (object.has("waves")) {
            for (JsonElement waveElement : object.getAsJsonArray("waves")) {
                JsonObject waveObject = waveElement.getAsJsonObject();
                List<EncounterDefinition.MobSpawn> mobs = new ArrayList<>();
                if (waveObject.has("mobs")) {
                    for (JsonElement mobElement : waveObject.getAsJsonArray("mobs")) {
                        JsonObject mobObject = mobElement.getAsJsonObject();
                        mobs.add(new EncounterDefinition.MobSpawn(
                                ResourceLocation.parse(mobObject.get("id").getAsString()),
                                intValue(mobObject, "count", 1)));
                    }
                }
                waves.add(new EncounterDefinition.Wave(mobs));
            }
        }
        EncounterDefinition.Scale defaults = EncounterDefinition.Scale.DEFAULT;
        EncounterDefinition.Scale scale = defaults;
        if (object.has("scale")) {
            JsonObject scaleObject = object.getAsJsonObject("scale");
            scale = new EncounterDefinition.Scale(
                    doubleValue(scaleObject, "health_per_act", defaults.healthPerAct()),
                    doubleValue(scaleObject, "damage_per_act", defaults.damagePerAct()),
                    doubleValue(scaleObject, "count_per_act", defaults.countPerAct()),
                    doubleValue(scaleObject, "health_per_extra_player", defaults.healthPerExtraPlayer()),
                    doubleValue(scaleObject, "damage_per_extra_player", defaults.damagePerExtraPlayer()));
        }
        if (waves.isEmpty() || waves.stream().allMatch(wave -> wave.mobs().isEmpty())) {
            throw new IllegalArgumentException("encounter has no mobs");
        }
        ResourceLocation rewardLoot = object.has("reward_loot")
                ? ResourceLocation.parse(object.get("reward_loot").getAsString())
                : null;
        return new EncounterDefinition(id, titleKey, boss, waves, scale, rewardLoot);
    }

    private static ResourceLocation normalize(String key) {
        return key.contains(":")
                ? ResourceLocation.parse(key)
                : ResourceLocation.fromNamespaceAndPath(Expedition.MODID, key);
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static double doubleValue(JsonObject object, String key, double fallback) {
        return object.has(key) ? object.get(key).getAsDouble() : fallback;
    }

    private static boolean boolValue(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }
}
