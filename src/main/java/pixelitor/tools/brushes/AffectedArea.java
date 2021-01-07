/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.debug.DebugNode;

import java.awt.Rectangle;

/**
 * Represents the area affected by a brush. Used for the undo.
 */
public class AffectedArea {
    // affected area coordinates (in image space)
    private double minX = Double.POSITIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    public AffectedArea() {
    }

    /**
     * Initialize the area with a brush position.
     *
     * Currently there is only one {@link AffectedArea} object for each
     * brush tool, and it gets reinitialized for each independent brush stroke.
     */
    public void initAt(PPoint p) {
        double x = p.getImX();
        double y = p.getImY();
        minX = x;
        minY = y;
        maxX = x;
        maxY = y;
    }

    /**
     * Update the area with a brush position
     */
    public void updateWith(PPoint p) {
        double x = p.getImX();
        double y = p.getImY();
        if (x > maxX) {
            maxX = x;
        }
        if (x < minX) {
            minX = x;
        }

        if (y > maxY) {
            maxY = y;
        }
        if (y < minY) {
            minY = y;
        }
    }

    /**
     * Returns the rectangle affected by a brush stroke for the undo
     */
    public Rectangle asRectangle(double radius) {
        double saveX = minX - radius;
        double saveY = minY - radius;

        double extraSize = 2 * radius + 2.0;
        double saveWidth = maxX - minX + extraSize;
        double saveHeight = maxY - minY + extraSize;

        return new Rectangle((int) saveX, (int) saveY,
            (int) saveWidth, (int) saveHeight);
    }

    public DebugNode getDebugNode() {
        var node = new DebugNode("affected area", this);

        node.addDouble("min x", minX);
        node.addDouble("min y", minY);
        node.addDouble("max x", maxX);
        node.addDouble("max y", maxY);

        return node;
    }

    @Override
    public String toString() {
        return "{minX=" + minX +
            ", minY=" + minY +
            ", maxX=" + maxX +
            ", maxY=" + maxY +
            '}';
    }
}
