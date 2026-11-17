package com.inf.expedition.run;

import com.inf.expedition.map.MapSnapshot;
import com.inf.expedition.map.NodeSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class MapSnapshotNbt {
    private MapSnapshotNbt() {
    }

    public static CompoundTag write(MapSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("start", snapshot.startId());
        tag.putInt("boss", snapshot.bossId());
        ListTag nodes = new ListTag();
        for (NodeSnapshot node : snapshot.nodes()) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putInt("id", node.id());
            nodeTag.putInt("floor", node.floor());
            nodeTag.putInt("column", node.column());
            nodeTag.putString("type", node.type());
            nodeTag.putIntArray("connections",
                    node.connections().stream().mapToInt(Integer::intValue).sorted().toArray());
            nodeTag.putBoolean("visited", node.visited());
            nodeTag.putBoolean("completed", node.completed());
            nodes.add(nodeTag);
        }
        tag.put("nodes", nodes);
        return tag;
    }

    public static MapSnapshot read(CompoundTag tag) {
        int start = tag.getInt("start");
        int boss = tag.getInt("boss");
        ListTag nodes = tag.getList("nodes", Tag.TAG_COMPOUND);
        List<NodeSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            CompoundTag node = nodes.getCompound(i);
            int[] rawConnections = node.getIntArray("connections");
            Set<Integer> connections = new LinkedHashSet<>();
            for (int connection : rawConnections) {
                connections.add(connection);
            }
            snapshots.add(new NodeSnapshot(
                    node.getInt("id"),
                    node.getInt("floor"),
                    node.getInt("column"),
                    node.getString("type"),
                    connections,
                    node.getBoolean("visited"),
                    node.getBoolean("completed")));
        }
        snapshots.sort(Comparator.comparingInt(NodeSnapshot::id));
        return new MapSnapshot(start, boss, snapshots);
    }
}
