/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.util.PPoint;

import java.awt.Cursor;

/**
 * An individual handle in the {@link CropBox}
 * that can be dragged with the mouse
 */
public class CropHandle extends DraggablePoint {
    public CropHandle(String name, int cursorType, View view) {
        // The location of the crop handles is set only after the
        // constructor, so set it temporarily to this non-null value.
        super(name, PPoint.ZERO, view);
        cursor = Cursor.getPredefinedCursor(cursorType);
    }
}
