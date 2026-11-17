package com.inf.expedition.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inf.expedition.map.MapConfig;
import com.inf.expedition.map.MapGenerator;
import com.inf.expedition.map.MapGraph;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunStateTest {
    private MapGraph graph;
    private int start;
    private int next;
    private int far;

    @BeforeEach
    void setup() {
        graph = new MapGenerator(42L).generate(MapConfig.DEFAULT);
        start = graph.startId();
        next = graph.node(start).connections().iterator().next();
        far = graph.floor(3).getFirst().id();
    }

    @Test
    void beginMarksStartVisited() {
        RunState state = RunState.idle();
        state.begin(1L, graph, List.of(UUID.randomUUID()));
        assertEquals(RunStatus.RUNNING, state.status());
        assertEquals(start, state.currentNodeId());
        assertTrue(graph.node(start).visited());
    }

    @Test
    void movingTwiceIsRejected() {
        RunState state = RunState.idle();
        state.begin(1L, graph, List.of(UUID.randomUUID()));
        assertFalse(state.moveTo(graph, next));
        state.completeCurrent(graph);
        assertTrue(state.moveTo(graph, next));
        assertFalse(state.moveTo(graph, next));
    }

    @Test
    void nonAdjacentMoveIsRejected() {
        RunState state = RunState.idle();
        state.begin(1L, graph, List.of(UUID.randomUUID()));
        state.completeCurrent(graph);
        assertFalse(state.moveTo(graph, far));
    }

    @Test
    void completeMarksCurrentNode() {
        RunState state = RunState.idle();
        state.begin(1L, graph, List.of(UUID.randomUUID()));
        state.completeCurrent(graph);
        assertTrue(graph.node(start).completed());
        assertTrue(state.currentCompleted(graph));
    }

    @Test
    void finishSetsStatus() {
        RunState state = RunState.idle();
        state.begin(1L, graph, List.of(UUID.randomUUID()));
        state.finish(true);
        assertEquals(RunStatus.COMPLETE, state.status());
        state.finish(false);
        assertEquals(RunStatus.FAILED, state.status());
    }
}
