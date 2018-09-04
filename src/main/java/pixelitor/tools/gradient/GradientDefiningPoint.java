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

package pixelitor.tools.gradient;

import pixelitor.gui.View;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.UserDrag;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * Either a gradient start point or a gradient end point
 */
public class GradientDefiningPoint extends DraggablePoint {
    private final GradientHandles gradientHandles;
    private GradientDefiningPoint other;
    private GradientCenterPoint center;

    public GradientDefiningPoint(String name, double x, double y,
                                 View view, Color color, Color activeColor,
                                 GradientHandles gradientHandles) {
        super(name, x, y, view, color, activeColor);
        this.gradientHandles = gradientHandles;
    }

    public void setOther(GradientDefiningPoint other) {
        this.other = other;
    }

    public void setCenter(GradientCenterPoint center) {
        this.center = center;
    }

    @Override
    public void setLocation(double x, double y) {
        super.setLocation(x, y);

        // also move the center point
        double cx = (x + other.x) / 2.0;
        double cy = (y + other.y) / 2.0;
        center.setLocationOnlyForThis(cx, cy);
    }

    @Override
    public void setConstrainedLocation(double mouseX, double mouseY) {
        // constrain it relative to the other point:
        // it seems more useful than constraining it relative to its own drag start
        Point2D p = Utils.constrainEndPoint(other.x, other.y, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    @Override
    protected void afterMouseReleasedActions() {
        calcImCoords();
        center.calcImCoords();
    }

    @Override
    public void paintHandle(Graphics2D g) {
        super.paintHandle(g);
        if (dragging && isActive()) {
            UserDrag ud = gradientHandles.toUserDrag(this);
            DragDisplayType.ANGLE_DIST.draw(g, ud);
        }
    }
}
