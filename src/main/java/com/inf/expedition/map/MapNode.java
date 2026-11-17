package com.inf.expedition.map;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MapNode {
    private final int id;
    private final int floor;
    private final int column;
    private final Set<Integer> connections = new LinkedHashSet<>();
    private NodeType type;
    private boolean visited;
    private boolean completed;

    public MapNode(int id, int floor, int column, NodeType type) {
        this.id = id;
        this.floor = floor;
        this.column = column;
        this.type = type;
    }

    public int id() {
        return id;
    }

    public int floor() {
        return floor;
    }

    public int column() {
        return column;
    }

    public NodeType type() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public Set<Integer> connections() {
        return Collections.unmodifiableSet(connections);
    }

    public void addConnection(int targetId) {
        connections.add(targetId);
    }

    public boolean visited() {
        return visited;
    }

    public void markVisited() {
        visited = true;
    }

    public boolean completed() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }
}
