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

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * The handle that can be used to rotate a {@link TransformBox}
 */
public class RotationHandle extends DraggablePoint {
    private final TransformBox box;
    private double cy;
    private double cx;
    private double rotStartAngle;
    private double angle;

    public RotationHandle(String name, TransformBox box, Point2D pos, View view) {
        super(name, pos.getX(), pos.getY(), view, Color.WHITE, Color.RED);
        this.box = box;
    }

    @Override
    public void mousePressed(int x, int y) {
        super.mousePressed(x, y); // sets dragStartX, dragStartY
        Point2D c = box.getCenter();

        cx = c.getX();
        cy = c.getY();
        box.copyHandleLocations();

        // recalculate because a flipping might have occurred
        rotStartAngle = Math.atan2(this.y - cy, this.x - cx) + Math.PI / 2;
    }

    @Override
    public void mouseDragged(double x, double y) {
        double dx = x - dragStartX;
        double dy = y - dragStartY;
        double newX = origX + dx;
        double newY = origY + dy;

        angle = Math.atan2(newY - cy, newX - cx) + Math.PI / 2;
        box.setAngle(angle);

        box.rotate(AffineTransform.getRotateInstance(angle - rotStartAngle, cx, cy));
    }

    @Override
    public void mouseReleased(int x, int y) {
        super.mouseReleased(x, y);
    }
}
