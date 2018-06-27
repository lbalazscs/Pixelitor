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

package pixelitor.transform;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.DraggablePoint;

import java.awt.Color;
import java.awt.Cursor;

/**
 * An individual handle in the {@link CropBox}
 * that can be dragged with the mouse
 */
public class Handle extends DraggablePoint {
    // All handle coordinates and sizes are in component space

    private final Cursor cursor;

    public Handle(String name, int cursorType, ImageComponent ic) {
        super(name, 0, 0, ic, Color.WHITE, Color.RED);
        this.cursor = Cursor.getPredefinedCursor(cursorType);
    }

    public Cursor getCursor() {
        return cursor;
    }

    public int getX() {
        return (int) x;
    }

    public int getY() {
        return (int) y;
    }
}
