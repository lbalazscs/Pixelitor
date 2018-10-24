/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import java.awt.Cursor;

/**
 * A convenience class for keeping track of mouse cursors
 */
public class Cursors {
    public static final Cursor MOVE = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    public static final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    public static final Cursor DEFAULT = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    public static final Cursor BUSY = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    public static final Cursor CROSSHAIR = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    private static final Cursor N = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
    private static final Cursor NW = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
    private static final Cursor W = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
    private static final Cursor SW = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
    private static final Cursor S = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
    private static final Cursor SE = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
    private static final Cursor E = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    private static final Cursor NE = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);

    // the values of the directional cursors as the angle increases CCW
    private static final Cursor[] DIRECTIONS = {N, NW, W, SW, S, SE, E, NE};

    // the corner offsets at 0 angle with a default transform box
    public static final int NW_OFFSET = 1;
    public static final int SW_OFFSET = 3;
    public static final int SE_OFFSET = 5;
    public static final int NE_OFFSET = 7;

    // the corner offsets at 0 angle with an "inside out" transform box,
    // where the width or the height are negative
    public static final int NW_OFFSET_IO = 7;
    public static final int SW_OFFSET_IO = 5;
    public static final int SE_OFFSET_IO = 3;
    public static final int NE_OFFSET_IO = 1;

    public static Cursor getCursorAtOffset(int index) {
        assert index >= 0;
        if (index > 7) {
            index = index % 8;
        }
        return DIRECTIONS[index];
    }

    private Cursors() {
    }
}
