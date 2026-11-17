package com.inf.expedition.run;

import com.inf.expedition.map.MapGraph;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

public final class ExpeditionSavedData extends SavedData {
    public static final String DATA_NAME = "inf_expedition";

    private static final SavedData.Factory<ExpeditionSavedData> FACTORY =
            new SavedData.Factory<>(ExpeditionSavedData::new, ExpeditionSavedData::load);

    private MapGraph map;
    private RunState run = RunState.idle();
    private boolean hubPlaced;
    private final Set<UUID> pendingWipe = new LinkedHashSet<>();
    private AABB roomBounds;
    private UUID roomId;

    public static ExpeditionSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    public MapGraph map() {
        return map;
    }

    public void setMap(MapGraph map) {
        this.map = map;
        setDirty();
    }

    public RunState run() {
        return run;
    }

    public void setRun(RunState run) {
        this.run = run;
        setDirty();
    }

    public boolean hubPlaced() {
        return hubPlaced;
    }

    public void setHubPlaced(boolean hubPlaced) {
        this.hubPlaced = hubPlaced;
        setDirty();
    }

    public void resetRun() {
        this.run = RunState.idle();
        this.map = null;
        setDirty();
    }

    public void queueWipe(UUID playerId) {
        pendingWipe.add(playerId);
        setDirty();
    }

    public boolean hasPendingWipe(UUID playerId) {
        return pendingWipe.contains(playerId);
    }

    public boolean consumeWipe(UUID playerId) {
        boolean removed = pendingWipe.remove(playerId);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void setRoomBounds(AABB bounds, UUID id) {
        this.roomBounds = bounds;
        this.roomId = id;
        setDirty();
    }

    public AABB roomBounds() {
        return roomBounds;
    }

    public UUID roomId() {
        return roomId;
    }

    public void clearRoomBounds() {
        this.roomBounds = null;
        this.roomId = null;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (map != null) {
            tag.put("map", MapSnapshotNbt.write(map.snapshot()));
        }
        tag.putLong("seed", run.seed());
        tag.putInt("current", run.currentNodeId());
        tag.putString("status", run.status().name());
        ListTag party = new ListTag();
        for (UUID playerId : run.party()) {
            party.add(StringTag.valueOf(playerId.toString()));
        }
        tag.put("party", party);
        tag.putBoolean("hubPlaced", hubPlaced);
        ListTag wipe = new ListTag();
        for (UUID playerId : pendingWipe) {
            wipe.add(StringTag.valueOf(playerId.toString()));
        }
        tag.put("pendingWipe", wipe);
        if (roomBounds != null) {
            CompoundTag bounds = new CompoundTag();
            bounds.putDouble("minX", roomBounds.minX);
            bounds.putDouble("minY", roomBounds.minY);
            bounds.putDouble("minZ", roomBounds.minZ);
            bounds.putDouble("maxX", roomBounds.maxX);
            bounds.putDouble("maxY", roomBounds.maxY);
            bounds.putDouble("maxZ", roomBounds.maxZ);
            tag.put("roomBounds", bounds);
            if (roomId != null) {
                tag.putString("roomId", roomId.toString());
            }
        }
        return tag;
    }

    public static ExpeditionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ExpeditionSavedData data = new ExpeditionSavedData();
        if (tag.contains("map")) {
            data.map = MapGraph.fromSnapshot(MapSnapshotNbt.read(tag.getCompound("map")));
        }
        RunStatus status;
        try {
            status = RunStatus.valueOf(tag.getString("status"));
        } catch (IllegalArgumentException exception) {
            status = RunStatus.IDLE;
        }
        if (status == RunStatus.RUNNING) {
            status = RunStatus.FAILED;
        }
        Set<UUID> party = new LinkedHashSet<>();
        ListTag partyTag = tag.getList("party", Tag.TAG_STRING);
        for (int i = 0; i < partyTag.size(); i++) {
            try {
                party.add(UUID.fromString(partyTag.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // Drop malformed UUIDs rather than failing the whole save.
            }
        }
        ListTag wipeTag = tag.getList("pendingWipe", Tag.TAG_STRING);
        for (int i = 0; i < wipeTag.size(); i++) {
            try {
                data.pendingWipe.add(UUID.fromString(wipeTag.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // Drop malformed UUIDs rather than failing the whole save.
            }
        }
        if (tag.contains("roomBounds")) {
            CompoundTag bounds = tag.getCompound("roomBounds");
            data.roomBounds = new AABB(
                    bounds.getDouble("minX"), bounds.getDouble("minY"), bounds.getDouble("minZ"),
                    bounds.getDouble("maxX"), bounds.getDouble("maxY"), bounds.getDouble("maxZ"));
        }
        if (tag.contains("roomId")) {
            try {
                data.roomId = UUID.fromString(tag.getString("roomId"));
            } catch (IllegalArgumentException ignored) {
                data.roomId = null;
            }
        }
        boolean hadRun = status != RunStatus.IDLE;
        if (hadRun) {
            data.pendingWipe.addAll(party);
            data.map = null;
            data.run = RunState.idle();
        } else {
            data.run = RunState.restore(tag.getLong("seed"), tag.getInt("current"), status, party);
        }
        data.hubPlaced = tag.getBoolean("hubPlaced");
        return data;
    }
}
