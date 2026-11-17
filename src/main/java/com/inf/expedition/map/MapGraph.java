package com.inf.expedition.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapGraph {
    private final List<List<MapNode>> floors;
    private final List<MapNode> nodes;
    private final Map<Integer, MapNode> byId;
    private final int startId;
    private final int bossId;

    public MapGraph(List<List<MapNode>> floors, int startId, int bossId) {
        this.floors = floors.stream().map(List::copyOf).toList();
        this.startId = startId;
        this.bossId = bossId;
        List<MapNode> all = new ArrayList<>();
        for (List<MapNode> floor : this.floors) {
            all.addAll(floor);
        }
        all.sort(Comparator.comparingInt(MapNode::id));
        this.nodes = List.copyOf(all);
        Map<Integer, MapNode> index = new HashMap<>();
        for (MapNode node : nodes) {
            index.put(node.id(), node);
        }
        this.byId = Map.copyOf(index);
    }

    public List<MapNode> nodeList() {
        return nodes;
    }

    public MapNode node(int id) {
        return byId.get(id);
    }

    public List<MapNode> floor(int floor) {
        if (floor < 0 || floor >= floors.size()) {
            return List.of();
        }
        return floors.get(floor);
    }

    public int startId() {
        return startId;
    }

    public int bossId() {
        return bossId;
    }

    public boolean canMove(int fromId, int toId) {
        MapNode from = node(fromId);
        MapNode to = node(toId);
        return from != null
                && to != null
                && from.connections().contains(toId)
                && to.floor() == from.floor() + 1;
    }

    public MapSnapshot snapshot() {
        List<NodeSnapshot> snapshots = nodes.stream()
                .sorted(Comparator.comparingInt(MapNode::id))
                .map(node -> new NodeSnapshot(
                        node.id(),
                        node.floor(),
                        node.column(),
                        node.type().id(),
                        node.connections(),
                        node.visited(),
                        node.completed()))
                .toList();
        return new MapSnapshot(startId, bossId, snapshots);
    }

    public static MapGraph fromSnapshot(MapSnapshot snapshot) {
        int maxFloor = snapshot.nodes().stream().mapToInt(NodeSnapshot::floor).max().orElse(0);
        List<List<MapNode>> floors = new ArrayList<>(maxFloor + 1);
        for (int i = 0; i <= maxFloor; i++) {
            floors.add(new ArrayList<>());
        }
        Map<Integer, MapNode> byId = new HashMap<>();
        for (NodeSnapshot node : snapshot.nodes()) {
            MapNode built = new MapNode(node.id(), node.floor(), node.column(), NodeType.byId(node.type()));
            if (node.visited()) {
                built.markVisited();
            }
            if (node.completed()) {
                built.markCompleted();
            }
            byId.put(node.id(), built);
            floors.get(node.floor()).add(built);
        }
        for (NodeSnapshot node : snapshot.nodes()) {
            MapNode built = byId.get(node.id());
            for (int target : node.connections()) {
                if (!byId.containsKey(target)) {
                    throw new IllegalArgumentException("Snapshot connection points at missing node " + target);
                }
                built.addConnection(target);
            }
        }
        for (List<MapNode> floor : floors) {
            floor.sort(Comparator.comparingInt(MapNode::column));
        }
        return new MapGraph(floors, snapshot.startId(), snapshot.bossId());
    }
}
