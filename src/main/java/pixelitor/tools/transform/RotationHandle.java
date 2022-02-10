/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.util.DragDisplay;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.Cursors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.Serial;

/**
 * The handle that can be used to rotate a {@link TransformBox}
 */
public class RotationHandle extends DraggablePoint {
    @Serial
    private static final long serialVersionUID = 1L;

    private final TransformBox box;
    private double cy;
    private double cx;
    private double rotStartAngle;

    private static final int MOUSE_DISPLAY_CENTER_DISTANCE = 12 + DragDisplay.ONE_LINER_BG_HEIGHT / 2;

    public RotationHandle(String name, TransformBox box, Point2D pos, View view) {
        super(name, pos.getX(), pos.getY(), view, Color.WHITE, Color.RED);
        this.box = box;
        cursor = Cursors.DEFAULT;
    }

    public RotationHandle copy(TransformBox newBox) {
        return new RotationHandle(name, newBox, this, view);
    }

    @Override
    protected Shape createShape(double startX, double startY) {
        return new Ellipse2D.Double(startX, startY, HANDLE_SIZE, HANDLE_SIZE);
    }

    @Override
    public void mousePressed(double x, double y) {
        super.mousePressed(x, y); // sets dragStartX, dragStartY
        recalcCenter();

        // recalculate because a flipping might have occurred
        rotStartAngle = Math.atan2(this.y - cy, this.x - cx) + Math.PI / 2;
    }

    @Override
    public void mouseDragged(double x, double y) {
        double dx = x - dragStartX;
        double dy = y - dragStartY;
        double newX = origX + dx;
        double newY = origY + dy;

        double angle = reCalcAngle(newX, newY, false);

        box.coTransform(AffineTransform.getRotateInstance(angle - rotStartAngle, cx, cy));
    }

    @Override
    public void mouseReleased(double x, double y) {
        super.mouseReleased(x, y);

        box.updateDirections();
    }

    public double reCalcAngle(double newX, double newY, boolean recalcCenter) {
        if (recalcCenter) {
            recalcCenter();
        }
        double angle = Math.atan2(newY - cy, newX - cx) + Math.PI / 2;
        box.setAngle(angle);
        return angle;
    }

    private void recalcCenter() {
        Point2D c = box.getCenter();
        cx = c.getX();
        cy = c.getY();
    }

    @Override
    public void paintHandle(Graphics2D g) {
        super.paintHandle(g);
        if (isActive()) {
            int displayBgWidth = DragDisplay.BG_WIDTH_ANGLE;
            DragDisplay dd = new DragDisplay(g, displayBgWidth);
            int dragAngle = box.getAngleDegrees();
            String angleInfo = "\u2221 = " + dragAngle + " \u00b0";

            double sin = box.getSin();
            double cos = box.getCos();

            float drawX = (float) (x
                - displayBgWidth / 2.0f
                + displayBgWidth * 0.7 * sin);
            float drawY = (float) (y
                - MOUSE_DISPLAY_CENTER_DISTANCE * cos
                + DragDisplay.ONE_LINER_BG_HEIGHT / 2.0f);
            dd.drawOneLine(angleInfo, drawX, drawY);
            dd.finish();
        }
    }

}
