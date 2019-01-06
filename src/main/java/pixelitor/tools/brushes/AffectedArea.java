/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
 * Calculates the area affected by a brush for the undo.
 */
public class AffectedArea {
    // affected area coordinates (in image space)
    private double minX = 0;
    private double minY = 0;
    private double maxX = 0;
    private double maxY = 0;

    public AffectedArea() {
    }

    /**
     * Initialize the area with a brush position
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
        if(x > maxX) {
            maxX = x;
        } else if(x < minX) {
            minX = x;
        }

        if(y > maxY) {
            maxY = y;
        } else if(y < minY) {
            minY = y;
        }
    }

    /**
     * Returns the rectangle affected by a brush stroke for the undo
     */
    public Rectangle asRectangle(double radius) {
        double extraSize = 2 * radius + 2.0;

        double saveX = minX - radius;
        double saveY = minY - radius;
        double saveWidth = maxX - minX + extraSize;
        double saveHeight = maxY - minY + extraSize;

        return new Rectangle((int) saveX, (int) saveY,
                (int) saveWidth, (int) saveHeight);
    }

    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("Affected Area", this);

        node.addDouble("minX", minX);
        node.addDouble("minY", minY);
        node.addDouble("maxX", maxX);
        node.addDouble("maxY", maxY);

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
