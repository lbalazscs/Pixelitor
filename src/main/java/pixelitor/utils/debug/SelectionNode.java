/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils.debug;

import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;

import java.awt.Rectangle;
import java.awt.Shape;

/**
 * A debugging node for a selection
 */
public class SelectionNode extends DebugNode {

    public SelectionNode(Selection selection) {
        super("Selection", selection);

        SelectionInteraction selectionInteraction = selection.getSelectionInteraction();
        addStringChild("Selection Interaction", selectionInteraction == null ? "null" : selectionInteraction.getNameForUndo());

        Shape shape = selection.getShape();
        addStringChild("Shape Class", shape.getClass().getName());

        Rectangle bounds = selection.getShapeBounds();
        addStringChild("Bounds", bounds.toString());
    }


}
