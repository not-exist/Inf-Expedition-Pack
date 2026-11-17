package com.inf.expedition.map;

import java.util.List;

public record MapSnapshot(int startId, int bossId, List<NodeSnapshot> nodes) {
    public MapSnapshot {
        nodes = List.copyOf(nodes);
    }
}
