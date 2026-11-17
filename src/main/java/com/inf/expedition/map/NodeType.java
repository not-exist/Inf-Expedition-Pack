package com.inf.expedition.map;

public enum NodeType {
    BATTLE("battle"),
    ELITE("elite"),
    BOSS("boss"),
    SHOP("shop"),
    REST("rest"),
    EVENT("event"),
    FORGE("forge"),
    START("start");

    private final String id;

    NodeType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isCombat() {
        return this == BATTLE || this == ELITE || this == BOSS;
    }

    public static NodeType byId(String id) {
        for (NodeType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + id);
    }
}
