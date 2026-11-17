package com.inf.expedition.run;

import com.inf.expedition.map.MapGraph;
import com.inf.expedition.map.MapNode;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class RunState {
    private long seed;
    private int currentNodeId = -1;
    private RunStatus status = RunStatus.IDLE;
    private final Set<UUID> party = new LinkedHashSet<>();

    private RunState() {
    }

    public static RunState idle() {
        return new RunState();
    }

    public static RunState restore(long seed, int currentNodeId, RunStatus status, Collection<UUID> party) {
        RunState state = new RunState();
        state.seed = seed;
        state.currentNodeId = currentNodeId;
        state.status = status;
        state.party.addAll(party);
        return state;
    }

    public void begin(long seed, MapGraph graph, Collection<UUID> party) {
        if (status != RunStatus.IDLE) {
            throw new IllegalStateException("A run is already active");
        }
        this.seed = seed;
        this.status = RunStatus.RUNNING;
        this.currentNodeId = graph.startId();
        this.party.clear();
        this.party.addAll(party);
        MapNode start = graph.node(graph.startId());
        if (start != null) {
            start.markVisited();
        }
    }

    public RunStatus status() {
        return status;
    }

    public long seed() {
        return seed;
    }

    public int currentNodeId() {
        return currentNodeId;
    }

    public Set<UUID> party() {
        return Collections.unmodifiableSet(party);
    }

    public boolean isPartyMember(UUID playerId) {
        return party.contains(playerId);
    }

    public void removeMember(UUID playerId) {
        party.remove(playerId);
    }

    public boolean moveTo(MapGraph graph, int targetId) {
        if (status != RunStatus.RUNNING) {
            return false;
        }
        MapNode current = graph.node(currentNodeId);
        if (current == null || !current.completed()) {
            return false;
        }
        if (!graph.canMove(currentNodeId, targetId)) {
            return false;
        }
        MapNode target = graph.node(targetId);
        if (target == null || target.visited()) {
            return false;
        }
        currentNodeId = targetId;
        target.markVisited();
        return true;
    }

    public void completeCurrent(MapGraph graph) {
        if (status != RunStatus.RUNNING) {
            return;
        }
        MapNode current = graph.node(currentNodeId);
        if (current != null) {
            current.markCompleted();
        }
    }

    public boolean currentCompleted(MapGraph graph) {
        MapNode current = graph.node(currentNodeId);
        return current != null && current.completed();
    }

    public void finish(boolean success) {
        status = success ? RunStatus.COMPLETE : RunStatus.FAILED;
    }
}
