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
package pixelitor.utils.debug;

import pixelitor.selection.Selection;

import java.awt.Rectangle;
import java.awt.Shape;

/**
 * A debugging node for a selection
 */
public class SelectionNode extends DebugNode {

    public SelectionNode(Selection selection) {
        super("Selection", selection);

        Shape shape = selection.getShape();
        addString("Shape Class", shape.getClass().getName());

        Rectangle bounds = selection.getShapeBounds();
        addString("Bounds", bounds.toString());
    }
}
