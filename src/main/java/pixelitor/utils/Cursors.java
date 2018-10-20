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

    public static final Cursor NW = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
    public static final Cursor NE = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
    public static final Cursor SE = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
    public static final Cursor SW = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);

    private Cursors() {
    }
}
