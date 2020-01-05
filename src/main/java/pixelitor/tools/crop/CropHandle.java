/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.crop;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;

import java.awt.Color;
import java.awt.Cursor;

/**
 * An individual handle in the {@link CropBox}
 * that can be dragged with the mouse
 */
public class CropHandle extends DraggablePoint {
    // All handle coordinates and sizes are in component space

    public CropHandle(String name, int cursorType, View view) {
        super(name, 0, 0, view, Color.WHITE, Color.RED);
        cursor = Cursor.getPredefinedCursor(cursorType);
    }
}
