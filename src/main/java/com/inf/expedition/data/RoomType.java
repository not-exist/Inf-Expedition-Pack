package com.inf.expedition.data;

import com.inf.expedition.map.NodeType;

public enum RoomType {
    START("start", NodeType.START),
    BATTLE("battle", NodeType.BATTLE),
    ELITE("elite", NodeType.ELITE),
    BOSS("boss", NodeType.BOSS),
    FORGE("forge", NodeType.FORGE),
    REST("rest", NodeType.REST),
    SHOP("shop", NodeType.SHOP),
    EVENT("event", NodeType.EVENT);

    private final String id;
    private final NodeType nodeType;

    RoomType(String id, NodeType nodeType) {
        this.id = id;
        this.nodeType = nodeType;
    }

    public String id() {
        return id;
    }

    public NodeType nodeType() {
        return nodeType;
    }

    public static RoomType byId(String id) {
        for (RoomType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + id);
    }

    public static RoomType forNode(NodeType nodeType) {
        for (RoomType type : values()) {
            if (type.nodeType == nodeType) {
                return type;
            }
        }
        throw new IllegalArgumentException("No room type for node type: " + nodeType);
    }
}
