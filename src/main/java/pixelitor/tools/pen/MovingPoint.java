/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.pen;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Utils;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * A point that follows the mouse cursor when the mouse
 * is moving (not dragging) as the path is being built.
 * It has an inverse mouse cycle: it is dragged when the mouse is actually
 * moved; its mousePressed is called when the mouse is released, etc.
 * If the Shift key is pressed, the point is constrained
 * relative to the previous anchor point.
 */
public class MovingPoint extends DraggablePoint {
    private final AnchorPoint prevAnchor;

    public MovingPoint(double coX, double coY, AnchorPoint prevAnchor, View view) {
        super("moving", new PPoint(coX, coY, view), view);
        this.prevAnchor = prevAnchor;
    }

    @Override
    public void paintHandle(Graphics2D g) {
        throw new IllegalStateException("should not be painted");
    }

    @Override
    public void setConstrainedLocation(double mouseX, double mouseY) {
        // constrain it relative to the previous anchor point
        Point2D p = Utils.constrainToNearestAngle(
            prevAnchor.x, prevAnchor.y, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    /**
     * Converts this moving point into a regular anchor point when the mouse is pressed.
     */
    public AnchorPoint toAnchor() {
        return new AnchorPoint(x, y, view, prevAnchor.getSubPath());
    }
}
