package com.inf.expedition.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MapGeneratorTest {
    private static final MapConfig C = MapConfig.DEFAULT;

    @Test
    void sameSeedProducesSameGraph() {
        assertEquals(new MapGenerator(42L).generate(C).snapshot(),
                     new MapGenerator(42L).generate(C).snapshot());
    }

    @Test
    void startAndBossAreUnique() {
        MapGraph g = new MapGenerator(7L).generate(C);
        assertEquals(1, g.floor(0).size());
        assertEquals(1, g.floor(C.bossFloor()).size());
        assertEquals(NodeType.START, g.node(g.startId()).type());
        assertEquals(NodeType.BOSS, g.node(g.bossId()).type());
    }

    @Test
    void connectionsAlwaysAdvanceOneFloor() {
        MapGraph g = new MapGenerator(11L).generate(C);
        for (MapNode n : g.nodeList()) {
            for (int target : n.connections()) {
                assertEquals(n.floor() + 1, g.node(target).floor());
            }
        }
    }

    @Test
    void everyContentNodeHasIncomingAndOutgoing() {
        MapGraph g = new MapGenerator(13L).generate(C);
        Set<Integer> incoming = new HashSet<>();
        for (MapNode n : g.nodeList()) {
            incoming.addAll(n.connections());
        }
        for (MapNode n : g.nodeList()) {
            if (n.type() != NodeType.START) {
                assertTrue(incoming.contains(n.id()), "no incoming " + n.id());
            }
            if (n.type() != NodeType.BOSS) {
                assertFalse(n.connections().isEmpty(), "no outgoing " + n.id());
            }
        }
    }

    @Test
    void minFloorRulesHold() {
        MapGraph g = new MapGenerator(17L).generate(C);
        for (MapNode n : g.nodeList()) {
            if (n.type() == NodeType.ELITE) {
                assertTrue(n.floor() >= C.eliteMinFloor());
            }
            if (n.type() == NodeType.SHOP) {
                assertTrue(n.floor() >= C.shopMinFloor());
            }
            if (n.type() == NodeType.FORGE) {
                assertTrue(n.floor() >= C.forgeMinFloor());
            }
        }
    }

    @Test
    void restIsGuaranteedBeforeBoss() {
        MapGraph g = new MapGenerator(19L).generate(C);
        assertTrue(g.floor(C.bossFloor() - 1).stream().anyMatch(n -> n.type() == NodeType.REST));
    }

    @Test
    void snapshotRoundTrip() {
        MapGraph g = new MapGenerator(23L).generate(C);
        assertEquals(g.snapshot(), MapGraph.fromSnapshot(g.snapshot()).snapshot());
    }
}
