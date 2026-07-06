/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.gradient;

import pixelitor.gui.View;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PPoint;

import java.awt.Graphics2D;

/**
 * The draggable point halfway between the start
 * and end points, which moves the entire gradient.
 */
public class GradientCenterPoint extends DraggablePoint {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;

    private boolean isDragging = false;
    private double dragStartImX;
    private double dragStartImY;

    public GradientCenterPoint(GradientDefiningPoint start,
                               GradientDefiningPoint end,
                               View view) {
        super("center", PPoint.halfPointBetween(start, end), view);
        this.start = start;
        this.end = end;
    }

    @Override
    public void mousePressed(double x, double y) {
        super.mousePressed(x, y);

        isDragging = true;
        dragStartImX = imX;
        dragStartImY = imY;
    }

    @Override
    protected void afterMouseReleasedActions() {
        super.afterMouseReleasedActions();
        isDragging = false;
    }

    @Override
    public void setLocation(double coX, double coY) {
        // calculate the delta before moving
        double dx = coX - this.x;
        double dy = coY - this.y;

        // move this center point
        super.setLocation(coX, coY);

        // also move the start and end points by the same delta
        start.translateOnlyThis(dx, dy);
        end.translateOnlyThis(dx, dy);
    }

    @Override
    public void paintHandle(Graphics2D g) {
        super.paintHandle(g);
        if (isActive() && isDragging) {
            Drag drag = new Drag(dragStartImX, dragStartImY, imX, imY);
            // ensure the drag is correctly mapped to component-space
            // coordinates so the overlay box positions itself correctly
            drag.calcCoCoords(view);
            OverlayType.REL_MOUSE_POS.draw(g, drag);
        }
    }
}
