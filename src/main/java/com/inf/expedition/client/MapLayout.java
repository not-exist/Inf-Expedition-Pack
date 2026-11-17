package com.inf.expedition.client;

public final class MapLayout {
    private static final float MAX_COLUMN_SPACING = 92.0F;
    private static final float MIN_COLUMN_SPACING = 24.0F;
    private static final float MAX_ROW_SPACING = 46.0F;
    private static final float MIN_ROW_SPACING = 14.0F;

    private MapLayout() {
    }

    public static float columnSpacing(int width, int maxColumn) {
        if (maxColumn <= 0) {
            return 0.0F;
        }
        return Math.min(MAX_COLUMN_SPACING,
                Math.max(MIN_COLUMN_SPACING, (width - 40.0F) / maxColumn));
    }

    public static float rowSpacing(int height, int maxFloor) {
        if (maxFloor <= 0) {
            return 0.0F;
        }
        return Math.min(MAX_ROW_SPACING,
                Math.max(MIN_ROW_SPACING, (height - 120.0F) / maxFloor));
    }

    public static int x(int width, int column, int maxColumn) {
        int offset = maxColumn / 2;
        return Math.round(width / 2.0F + (column - offset) * columnSpacing(width, maxColumn));
    }

    public static int y(int height, int floor, int maxFloor) {
        return Math.round(height - 70.0F - floor * rowSpacing(height, maxFloor));
    }
}
