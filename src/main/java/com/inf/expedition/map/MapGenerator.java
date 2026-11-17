package com.inf.expedition.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class MapGenerator {
    private final Random random;

    public MapGenerator(long seed) {
        this.random = new Random(seed);
    }

    public MapGraph generate(MapConfig config) {
        int columns = Math.max(1, config.columns());
        int paths = Math.max(1, config.paths());
        int bossFloor = Math.max(2, config.bossFloor());
        Map<Integer, Builder> builders = new LinkedHashMap<>();

        Builder start = builder(builders, 0, columns / 2);
        Builder boss = builder(builders, bossFloor, columns / 2);

        for (int path = 0; path < paths; path++) {
            int column = paths == 1
                    ? columns / 2
                    : (int) Math.round((double) path * (columns - 1) / (paths - 1));
            column = clamp(column, 0, columns - 1);
            Builder previous = start;
            for (int floor = 1; floor < bossFloor; floor++) {
                Builder current = builder(builders, floor, column);
                previous.connections.add(current);
                previous = current;
                if (floor < bossFloor - 1) {
                    column = clamp(column + random.nextInt(3) - 1, 0, columns - 1);
                }
            }
        }

        for (int floor = 1; floor < bossFloor - 1; floor++) {
            for (Builder node : buildersOnFloor(builders, floor)) {
                if (node.connections.isEmpty()) {
                    Builder target = nearest(buildersOnFloor(builders, floor + 1), node.column);
                    if (target == null) {
                        target = builder(builders, floor + 1, node.column);
                    }
                    node.connections.add(target);
                }
            }
        }
        for (Builder node : buildersOnFloor(builders, bossFloor - 1)) {
            node.connections.add(boss);
        }

        fillTypes(config, builders, start, boss, bossFloor);
        return assemble(builders, start, boss, bossFloor);
    }

    private void fillTypes(
            MapConfig config,
            Map<Integer, Builder> builders,
            Builder start,
            Builder boss,
            int bossFloor) {
        start.type = NodeType.START;
        boss.type = NodeType.BOSS;

        for (int floor = 1; floor < bossFloor; floor++) {
            Set<NodeType> specialOnFloor = EnumSet.noneOf(NodeType.class);
            for (Builder node : buildersOnFloor(builders, floor)) {
                NodeType picked = pickType(config, floor, specialOnFloor);
                if (isSpecial(picked) && specialOnFloor.contains(picked)) {
                    picked = NodeType.BATTLE;
                }
                node.type = picked;
                if (isSpecial(picked)) {
                    specialOnFloor.add(picked);
                }
            }
        }

        if (config.guaranteeRestBeforeBoss()) {
            List<Builder> lastFloor = buildersOnFloor(builders, bossFloor - 1);
            if (lastFloor.stream().noneMatch(node -> node.type == NodeType.REST)) {
                Builder target = lastFloor.stream()
                        .filter(node -> node.type == NodeType.BATTLE || node.type == NodeType.EVENT)
                        .findFirst()
                        .orElseGet(() -> lastFloor.stream()
                                .filter(node -> node.type != NodeType.ELITE)
                                .findFirst()
                                .orElse(lastFloor.getFirst()));
                target.type = NodeType.REST;
            }
        }
    }

    private NodeType pickType(MapConfig config, int floor, Set<NodeType> specialOnFloor) {
        List<Map.Entry<NodeType, Integer>> eligible = config.weights().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .filter(entry -> entry.getKey() != NodeType.START && entry.getKey() != NodeType.BOSS)
                .filter(entry -> floor >= minFloor(config, entry.getKey()))
                .sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
                .toList();
        if (eligible.isEmpty()) {
            return NodeType.BATTLE;
        }
        NodeType picked = weightedPick(eligible);
        if (isSpecial(picked) && specialOnFloor.contains(picked)) {
            picked = weightedPick(eligible);
        }
        return picked;
    }

    private NodeType weightedPick(List<Map.Entry<NodeType, Integer>> entries) {
        int total = entries.stream().mapToInt(Map.Entry::getValue).sum();
        int roll = random.nextInt(total);
        for (Map.Entry<NodeType, Integer> entry : entries) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return entries.getLast().getKey();
    }

    private static int minFloor(MapConfig config, NodeType type) {
        return switch (type) {
            case ELITE -> config.eliteMinFloor();
            case SHOP -> config.shopMinFloor();
            case FORGE -> config.forgeMinFloor();
            default -> 0;
        };
    }

    private static boolean isSpecial(NodeType type) {
        return type == NodeType.FORGE || type == NodeType.SHOP || type == NodeType.REST;
    }

    private MapGraph assemble(
            Map<Integer, Builder> builders,
            Builder start,
            Builder boss,
            int bossFloor) {
        List<Builder> ordered = new ArrayList<>(builders.values());
        ordered.sort(Comparator.comparingInt((Builder node) -> node.floor)
                .thenComparingInt(node -> node.column));
        Map<Builder, Integer> ids = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            ids.put(ordered.get(i), i);
        }
        Map<Builder, MapNode> nodes = new HashMap<>();
        List<List<MapNode>> floors = new ArrayList<>(bossFloor + 1);
        for (int i = 0; i <= bossFloor; i++) {
            floors.add(new ArrayList<>());
        }
        for (Builder builderNode : ordered) {
            NodeType type = builderNode.type == null ? NodeType.BATTLE : builderNode.type;
            MapNode node = new MapNode(ids.get(builderNode), builderNode.floor, builderNode.column, type);
            nodes.put(builderNode, node);
            floors.get(builderNode.floor).add(node);
        }
        for (Builder builderNode : ordered) {
            MapNode node = nodes.get(builderNode);
            for (Builder target : builderNode.connections) {
                node.addConnection(ids.get(target));
            }
        }
        for (List<MapNode> floor : floors) {
            floor.sort(Comparator.comparingInt(MapNode::column));
        }
        return new MapGraph(floors, ids.get(start), ids.get(boss));
    }

    private static Builder builder(Map<Integer, Builder> builders, int floor, int column) {
        int key = floor * 1_000 + column;
        return builders.computeIfAbsent(key, ignored -> new Builder(floor, column));
    }

    private static List<Builder> buildersOnFloor(Map<Integer, Builder> builders, int floor) {
        return builders.values().stream()
                .filter(node -> node.floor == floor)
                .sorted(Comparator.comparingInt(node -> node.column))
                .toList();
    }

    private static Builder nearest(List<Builder> candidates, int column) {
        Builder best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Builder candidate : candidates) {
            int distance = Math.abs(candidate.column - column);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Builder {
        private final int floor;
        private final int column;
        private final Set<Builder> connections = new LinkedHashSet<>();
        private NodeType type;

        private Builder(int floor, int column) {
            this.floor = floor;
            this.column = column;
        }
    }
}
