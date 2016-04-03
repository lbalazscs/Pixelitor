/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Tracks the area affected by a brush for the undo
 * Can be used as a decorator to other brushes
 */
public class BrushAffectedArea implements Brush {
    // affected area coordinates
    private double minX = 0;
    private double minY = 0;
    private double maxX = 0;
    private double maxY = 0;

    private Brush delegate;

    public BrushAffectedArea(Brush delegate) {
        this.delegate = delegate;
    }

    // this constructor is used when this object is not used as a brush delegate
    public BrushAffectedArea() {
    }

    public void updateAffectedCoordinates(double x, double y) {
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

    public void initAffectedCoordinates(double x, double y) {
        minX = x;
        minY = y;
        maxX = x;
        maxY = y;
    }

    /**
     * Calculates the rectangle affected by a brush stroke for the undo mechanism
     */
    public Rectangle getRectangleAffectedByBrush(int radius) {
        // To be on the safe side, save a little more than necessary - some brushes have randomness
        int radius2 = 2 * radius;
        int radius4 = 4 * radius;

        double saveX = minX - radius2;
        double saveY = minY - radius2;
        double saveWidth = maxX - minX + radius4;
        double saveHeight = maxY - minY + radius4;
        Rectangle rectangleAffectedByBrush = new Rectangle((int) saveX, (int) saveY, (int) saveWidth, (int) saveHeight);
        return rectangleAffectedByBrush;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        delegate.setTarget(comp, g);
    }

    @Override
    public void setRadius(int radius) {
        delegate.setRadius(radius);
    }

    @Override
    public void onDragStart(double x, double y) {
        updateAffectedCoordinates(x, y);
        delegate.onDragStart(x, y);
    }

    @Override
    public void onNewMousePoint(double x, double y) {
        updateAffectedCoordinates(x, y);
        delegate.onNewMousePoint(x, y);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("Brush Affected Area", this);

        node.addDoubleChild("minX", minX);
        node.addDoubleChild("minY", minY);
        node.addDoubleChild("maxX", maxX);
        node.addDoubleChild("maxY", maxY);

        if (delegate != null) {
            node.add(delegate.getDebugNode());
        } else {
            node.addStringChild("delegate", "null");
        }

        return node;
    }
}
