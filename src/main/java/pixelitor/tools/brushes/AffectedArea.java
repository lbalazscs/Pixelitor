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

package pixelitor.tools.brushes;

import pixelitor.tools.util.PPoint;
import pixelitor.utils.BoundingBox;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Rectangle;

/**
 * Represents the area affected by a brush. Used for the undo.
 */
public class AffectedArea implements Debuggable {
    private final BoundingBox boundingBox = new BoundingBox();

    public AffectedArea() {
    }

    /**
     * Initialize the area with a brush position.
     *
     * There is only one {@link AffectedArea} object for each brush tool,
     * and it gets reinitialized for each independent brush stroke.
     */
    public void initAt(PPoint p) {
        boundingBox.reset();
        boundingBox.add(p.getImX(), p.getImY());
    }

    /**
     * Update the area with a brush position
     */
    public void updateWith(PPoint p) {
        boundingBox.add(p.getImX(), p.getImY());
    }

    /**
     * Returns the rectangle affected by a brush stroke for the undo
     */
    public Rectangle asRectangle(double radius) {
        return boundingBox.asRectangle(radius + 1.0);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);

        node.addDouble("min x", boundingBox.getMinX());
        node.addDouble("min y", boundingBox.getMinY());
        node.addDouble("max x", boundingBox.getMaxX());
        node.addDouble("max y", boundingBox.getMaxY());

        return node;
    }
}
