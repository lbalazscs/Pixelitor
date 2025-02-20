/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.tools.transform;

import pixelitor.utils.Cursors;

import java.awt.Cursor;

import static pixelitor.tools.util.DraggablePoint.HANDLE_RADIUS;
import static pixelitor.tools.util.MeasurementOverlay.BG_WIDTH_PIXELS;
import static pixelitor.tools.util.MeasurementOverlay.OFFSET_FROM_MOUSE;
import static pixelitor.tools.util.MeasurementOverlay.SINGLE_LINE_HEIGHT;

/**
 * The direction of a corner or an edge in a {@link TransformBox}.
 * It determines the (rotation-dependent) cursors and measurement overlay positions.
 */
public enum Direction {
    NORTH(Cursors.N,
        -BG_WIDTH_PIXELS / 2.0f,
        -OFFSET_FROM_MOUSE - HANDLE_RADIUS),
    NORTH_WEST(Cursors.NW,
        -BG_WIDTH_PIXELS - OFFSET_FROM_MOUSE,
        -OFFSET_FROM_MOUSE),
    WEST(Cursors.W,
        -BG_WIDTH_PIXELS - OFFSET_FROM_MOUSE,
        SINGLE_LINE_HEIGHT / 2.0f),
    SOUTH_WEST(Cursors.SW,
        -BG_WIDTH_PIXELS - OFFSET_FROM_MOUSE,
        OFFSET_FROM_MOUSE + SINGLE_LINE_HEIGHT),
    SOUTH(Cursors.S,
        -BG_WIDTH_PIXELS / 2.0f,
        OFFSET_FROM_MOUSE + SINGLE_LINE_HEIGHT),
    SOUTH_EAST(Cursors.SE,
        OFFSET_FROM_MOUSE,
        OFFSET_FROM_MOUSE + SINGLE_LINE_HEIGHT),
    EAST(Cursors.E,
        OFFSET_FROM_MOUSE,
        SINGLE_LINE_HEIGHT / 2.0f),
    NORTH_EAST(Cursors.NE,
        OFFSET_FROM_MOUSE,
        -OFFSET_FROM_MOUSE);

    // the corner offsets at 0 angle with a default transform box
    public static final int N_OFFSET = 0;
    public static final int NW_OFFSET = 1;
    public static final int W_OFFSET = 2;
    public static final int SW_OFFSET = 3;
    public static final int S_OFFSET = 4;
    public static final int SE_OFFSET = 5;
    public static final int E_OFFSET = 6;
    public static final int NE_OFFSET = 7;

    // the corner offsets at 0 angle with an "inside out" transform box,
    // where the width or the height are negative
    public static final int NW_OFFSET_IO = 7;
    public static final int W_OFFSET_IO = 6;
    public static final int SW_OFFSET_IO = 5;
    public static final int S_OFFSET_IO = 4;
    public static final int SE_OFFSET_IO = 3;
    public static final int E_OFFSET_IO = 2;
    public static final int NE_OFFSET_IO = 1;
    public static final int N_OFFSET_IO = 0;

    // the cursor shown for a corner facing the current direction
    private final Cursor cursor;

    // the relative distances needed for displaying the measurement overlay
    final double dx;
    final double dy;

    private static final Direction[] directions = values();

    Direction(Cursor cursor, double dx, double dy) {
        this.cursor = cursor;
        this.dx = dx;
        this.dy = dy;
    }

    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Return the direction at the given offset
     */
    public static Direction atOffset(int index) {
        assert index >= 0;
        if (index > 7) {
            index = index % 8;
        }
        return directions[index];
    }
}
