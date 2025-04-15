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

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.MeasurementOverlay;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;

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
    private double pivotY;
    private double pivotX;
    private double rotStartAngle;

    private static final int MOUSE_DISPLAY_CENTER_DISTANCE = 12 + MeasurementOverlay.SINGLE_LINE_HEIGHT / 2;

    public RotationHandle(String name, TransformBox box, PPoint pos, View view) {
        super(name, pos, view);
        this.box = box;
        cursor = Cursors.DEFAULT;
    }

    public RotationHandle copy(TransformBox newBox) {
        PPoint pos;
        if (view != null) {
            pos = PPoint.fromIm(getImX(), getImY(), view);
        } else {
            pos = PPoint.lazyFromIm(getImX(), getImY(), view);
        }

        return new RotationHandle(name, newBox, pos, view);
    }

    @Override
    protected Shape createShape(double startX, double startY) {
        return new Ellipse2D.Double(startX, startY, HANDLE_SIZE, HANDLE_SIZE);
    }

    @Override
    public void mousePressed(double x, double y) {
        super.mousePressed(x, y); // sets dragStartX, dragStartY

        updatePivot();

        // recalculate because a flipping might have occurred
        rotStartAngle = Math.atan2(this.y - pivotY, this.x - pivotX) + Math.PI / 2;
    }

    @Override
    public void mouseDragged(double x, double y) {
        double dx = x - dragStartX;
        double dy = y - dragStartY;
        double newX = origX + dx;
        double newY = origY + dy;

        double newAngle = recalcAngle(newX, newY, false);

        AffineTransform dragTransform = AffineTransform.getRotateInstance(
            newAngle - rotStartAngle, pivotX, pivotY);
        box.coTransform(dragTransform);
        box.setRotated(true);
    }

    @Override
    public void mouseReleased(double x, double y) {
        super.mouseReleased(x, y);

        box.updateDirections();
    }

    public double recalcAngle(double newX, double newY, boolean updatePivot) {
        if (updatePivot) {
            updatePivot();
        }
        double angle = Math.atan2(newY - pivotY, newX - pivotX) + Math.PI / 2;
        box.setAngle(angle);
        return angle;
    }

    private void updatePivot() {
        Point2D p = box.getPivot();
        pivotX = p.getX();
        pivotY = p.getY();
    }

    @Override
    public void paintHandle(Graphics2D g) {
        super.paintHandle(g);
        if (isActive()) {
            // show the current angle in a measurement overlay
            int displayBgWidth = MeasurementOverlay.BG_WIDTH_ANGLES;
            MeasurementOverlay overlay = new MeasurementOverlay(g, displayBgWidth);
            int dragAngle = box.getAngleDegrees();
            String angleInfo = "∡ = " + dragAngle + " °";

            double sin = box.getSin();
            double cos = box.getCos();

            float drawX = (float) (x
                - displayBgWidth / 2.0f
                + displayBgWidth * 0.7 * sin);
            float drawY = (float) (y
                - MOUSE_DISPLAY_CENTER_DISTANCE * cos
                + MeasurementOverlay.SINGLE_LINE_HEIGHT / 2.0f);
            overlay.drawOneLine(angleInfo, drawX, drawY);
            overlay.cleanup();
        }
    }

    @Override
    public boolean shouldSnap() {
        return false;
    }
}
