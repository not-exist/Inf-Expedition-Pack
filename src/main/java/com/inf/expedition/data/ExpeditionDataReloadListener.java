package com.inf.expedition.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.inf.expedition.map.MapConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class ExpeditionDataReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public ExpeditionDataReloadListener() {
        super(GSON, "expedition");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ExpeditionDataReloadListener());
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<String, String> rooms = new LinkedHashMap<>();
        Map<String, String> encounters = new LinkedHashMap<>();
        String mapJson = null;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            String path = entry.getKey().getPath();
            String json = entry.getValue().toString();
            if (path.equals("map")) {
                mapJson = json;
            } else if (path.startsWith("rooms/")) {
                rooms.put(path.substring("rooms/".length()), json);
            } else if (path.startsWith("encounters/")) {
                encounters.put(path.substring("encounters/".length()), json);
            }
        }
        MapConfig config = mapJson == null ? MapConfig.DEFAULT : MapConfig.fromJson(mapJson);
        ExpeditionData.set(config, RoomCatalog.fromJson(rooms, encounters));
    }
}
