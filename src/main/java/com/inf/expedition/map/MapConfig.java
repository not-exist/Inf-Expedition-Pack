package com.inf.expedition.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;

public record MapConfig(
        int acts,
        int floors,
        int columns,
        int paths,
        int bossFloor,
        int eliteMinFloor,
        int shopMinFloor,
        int forgeMinFloor,
        Map<NodeType, Integer> weights,
        boolean guaranteeRestBeforeBoss) {

    public static final MapConfig DEFAULT = new MapConfig(
            1,
            8,
            3,
            2,
            8,
            3,
            2,
            2,
            Map.of(
                    NodeType.BATTLE, 40,
                    NodeType.EVENT, 20,
                    NodeType.FORGE, 14,
                    NodeType.REST, 12,
                    NodeType.SHOP, 8,
                    NodeType.ELITE, 6),
            true);

    public MapConfig {
        weights = Map.copyOf(weights);
    }

    public int weight(NodeType type) {
        return weights.getOrDefault(type, 0);
    }

    public static MapConfig fromJson(String json) {
        if (json == null || json.isBlank()) {
            return DEFAULT;
        }
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            int acts = Math.max(1, Math.min(1, intValue(object, "acts", DEFAULT.acts)));
            int floors = Math.max(2, intValue(object, "floors", DEFAULT.floors));
            int columns = Math.max(1, intValue(object, "columns", DEFAULT.columns));
            int paths = Math.max(1, intValue(object, "paths", DEFAULT.paths));
            int bossFloor = Math.max(2, intValue(object, "boss_floor", DEFAULT.bossFloor));
            int eliteMinFloor = Math.max(0, intValue(object, "elite_min_floor", DEFAULT.eliteMinFloor));
            int shopMinFloor = Math.max(0, intValue(object, "shop_min_floor", DEFAULT.shopMinFloor));
            int forgeMinFloor = Math.max(0, intValue(object, "forge_min_floor", DEFAULT.forgeMinFloor));
            boolean guaranteeRest = boolValue(object, "guarantee_rest_before_boss", DEFAULT.guaranteeRestBeforeBoss);

            Map<NodeType, Integer> weights = new LinkedHashMap<>(DEFAULT.weights);
            if (object.has("weights") && object.get("weights").isJsonObject()) {
                Map<NodeType, Integer> parsed = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("weights").entrySet()) {
                    try {
                        parsed.put(NodeType.byId(entry.getKey()), Math.max(0, entry.getValue().getAsInt()));
                    } catch (RuntimeException ignored) {
                        // Unknown node types are ignored, not fatal.
                    }
                }
                if (!parsed.isEmpty()) {
                    weights = parsed;
                }
            }
            return new MapConfig(acts, floors, columns, paths, bossFloor, eliteMinFloor,
                    shopMinFloor, forgeMinFloor, Map.copyOf(weights), guaranteeRest);
        } catch (RuntimeException exception) {
            return DEFAULT;
        }
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static boolean boolValue(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }
}
