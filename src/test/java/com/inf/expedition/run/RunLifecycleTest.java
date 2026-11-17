package com.inf.expedition.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inf.expedition.map.MapConfig;
import com.inf.expedition.map.MapGenerator;
import com.inf.expedition.map.MapGraph;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunLifecycleTest {
    private MapGraph graph;
    private RunState run;
    private UUID member;
    private int next;

    @BeforeEach
    void setup() {
        graph = new MapGenerator(42L).generate(MapConfig.DEFAULT);
        member = UUID.randomUUID();
        run = RunState.idle();
        run.begin(1L, graph, List.of(member));
        next = graph.node(graph.startId()).connections().iterator().next();
    }

    @Test
    void nonPartyMemberCannotMove() {
        assertEquals(RunLifecycle.MoveResult.NOT_IN_PARTY,
                RunLifecycle.validateMove(run, graph, UUID.randomUUID(), next));
    }

    @Test
    void roomMustBeComplete() {
        assertEquals(RunLifecycle.MoveResult.ROOM_INCOMPLETE,
                RunLifecycle.validateMove(run, graph, member, next));
    }

    @Test
    void validMoveIsReady() {
        run.completeCurrent(graph);
        assertEquals(RunLifecycle.MoveResult.OK,
                RunLifecycle.validateMove(run, graph, member, next));
    }

    @Test
    void emptyPartyEndsRun() {
        assertTrue(RunLifecycle.shouldEnd(List.of()));
    }
}
