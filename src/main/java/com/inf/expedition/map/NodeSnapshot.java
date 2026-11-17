package com.inf.expedition.map;

import java.util.Set;

public record NodeSnapshot(
        int id,
        int floor,
        int column,
        String type,
        Set<Integer> connections,
        boolean visited,
        boolean completed) {
    public NodeSnapshot {
        connections = Set.copyOf(connections);
    }
}
