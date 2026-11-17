package com.inf.expedition.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MapLayoutTest {
    @Test
    void allFloorsFitInsideGuiHeight() {
        int height = 270; // 1080p at GUI scale 4
        int maxFloor = 8;
        int previous = Integer.MAX_VALUE;
        for (int floor = 0; floor <= maxFloor; floor++) {
            int y = MapLayout.y(height, floor, maxFloor);
            assertTrue(y >= 0 && y <= height, "floor " + floor + " at " + y);
            assertTrue(y < previous, "floors must descend");
            previous = y;
        }
    }

    @Test
    void allColumnsFitInsideGuiWidth() {
        int width = 240;
        int maxColumn = 2;
        int previous = Integer.MIN_VALUE;
        for (int column = 0; column <= maxColumn; column++) {
            int x = MapLayout.x(width, column, maxColumn);
            assertTrue(x >= 0 && x <= width, "column " + column + " at " + x);
            assertTrue(x > previous, "columns must advance");
            previous = x;
        }
    }
}
