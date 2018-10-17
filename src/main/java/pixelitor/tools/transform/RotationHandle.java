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
import pixelitor.tools.util.DragDisplay;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.awt.Graphics2D;
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

    private static final int MOUSE_DISPLAY_CENTER_DISTANCE = 12 + DragDisplay.ONE_LINER_BG_HEIGHT / 2;

    public RotationHandle(String name, TransformBox box, Point2D pos, View view) {
        super(name, pos.getX(), pos.getY(), view, Color.WHITE, Color.RED);
        this.box = box;
    }

    @Override
    public void mousePressed(double x, double y) {
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

        double angle = Math.atan2(newY - cy, newX - cx) + Math.PI / 2;
        box.setAngle(angle);

        box.rotate(AffineTransform.getRotateInstance(angle - rotStartAngle, cx, cy));
    }

    @Override
    public void restoreCoordsFromImSpace(View view) {
        // do nothing, the position of the rotation handles is
        // calculated from the restored transform handle positions
    }

    @Override
    public void paintHandle(Graphics2D g) {
        super.paintHandle(g);
        if (isActive()) {
            int displayBgWidth = DragDisplay.BG_WIDTH_ANGLE;
            DragDisplay dd = new DragDisplay(g, displayBgWidth);
            int dragAngle = (int) Math.toDegrees(
                    Utils.atan2AngleToIntuitive(box.getAngle()));
            String angleInfo = "\u2221 = " + dragAngle + " \u00b0";

            double sin = box.getSin();
            double cos = box.getCos();

            float drawX = (float) (this.x
                    - displayBgWidth / 2.0f
                    + displayBgWidth * 0.7 * sin);
            float drawY = (float) (this.y
                    - MOUSE_DISPLAY_CENTER_DISTANCE * cos
                    + DragDisplay.ONE_LINER_BG_HEIGHT / 2.0f);
            dd.drawOneLine(angleInfo, drawX, drawY);
            dd.finish();
        }
    }

}
