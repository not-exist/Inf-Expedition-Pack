package com.inf.expedition.run;

import com.inf.expedition.map.MapGraph;
import com.inf.expedition.map.MapNode;
import java.util.Collection;
import java.util.UUID;

public final class RunLifecycle {
    public enum MoveResult {
        OK,
        NOT_RUNNING,
        NOT_IN_PARTY,
        ROOM_INCOMPLETE,
        NOT_CONNECTED,
        TARGET_VISITED,
        MISSING_NODE
    }

    private RunLifecycle() {
    }

    public static MoveResult validateMove(RunState run, MapGraph graph, UUID playerId, int targetId) {
        if (run.status() != RunStatus.RUNNING) {
            return MoveResult.NOT_RUNNING;
        }
        if (!run.isPartyMember(playerId)) {
            return MoveResult.NOT_IN_PARTY;
        }
        MapNode current = graph.node(run.currentNodeId());
        if (current == null || !current.completed()) {
            return MoveResult.ROOM_INCOMPLETE;
        }
        MapNode target = graph.node(targetId);
        if (target == null) {
            return MoveResult.MISSING_NODE;
        }
        if (!graph.canMove(run.currentNodeId(), targetId)) {
            return MoveResult.NOT_CONNECTED;
        }
        if (target.visited()) {
            return MoveResult.TARGET_VISITED;
        }
        return MoveResult.OK;
    }

    public static boolean shouldEnd(Collection<UUID> party) {
        return party.isEmpty();
    }
}
