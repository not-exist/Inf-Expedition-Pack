package com.inf.expedition.client;

import com.inf.expedition.map.MapSnapshot;
import com.inf.expedition.map.NodeSnapshot;
import com.inf.expedition.network.MapSyncPayload;
import com.inf.expedition.run.MapSnapshotNbt;

public final class MapClientData {
    private static MapSnapshot snapshot;
    private static int currentNodeId = -1;
    private static int act = 1;
    private static String status = "IDLE";
    private static boolean canMove;

    private MapClientData() {
    }

    public static void update(MapSyncPayload payload) {
        snapshot = MapSnapshotNbt.read(payload.map());
        currentNodeId = payload.current();
        act = payload.act();
        status = payload.status();
        canMove = payload.canMove();
    }

    public static MapSnapshot snapshot() {
        return snapshot;
    }

    public static int currentNodeId() {
        return currentNodeId;
    }

    public static int act() {
        return act;
    }

    public static String status() {
        return status;
    }

    public static boolean canMove() {
        return canMove;
    }

    public static NodeSnapshot node(int id) {
        if (snapshot == null) {
            return null;
        }
        return snapshot.nodes().stream().filter(node -> node.id() == id).findFirst().orElse(null);
    }
}
