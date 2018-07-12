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

import pixelitor.Build;
import pixelitor.gui.ImageArea;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.ImDrag;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * The three handles that can be used to manipulate
 * a gradient by dragging
 */
public class GradientHandles {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;
    private final GradientCenterPoint middle;
    private final View view;

    public GradientHandles(Point2D start, Point2D end, View view) {
        this(start.getX(), start.getY(), end.getX(), end.getY(), view);
    }

    public GradientHandles(double startX, double startY, double endX, double endY, View view) {
        this.view = view;
        Color defaultColor = Color.WHITE;
        Color activeColor = Color.RED;

        start = new GradientDefiningPoint("start", startX, startY, view, defaultColor, activeColor);
        end = new GradientDefiningPoint("end", endX, endY, view, defaultColor, activeColor);
        middle = new GradientCenterPoint(start, end, view, defaultColor, activeColor);

        end.setOther(start);
        end.setCenter(middle);
        start.setOther(end);
        start.setCenter(middle);
    }

    public DraggablePoint handleWasHit(int x, int y) {
        if (end.handleContains(x, y)) {
            return end;
        }
        if (start.handleContains(x, y)) {
            return start;
        }
        if (middle.handleContains(x, y)) {
            return middle;
        }
        return null;
    }

    public void paint(Graphics2D g) {
        Shapes.drawGradientArrow(g, start.x, start.y, end.x, end.y);

        start.paintHandle(g);
        end.paintHandle(g);
        middle.paintHandle(g);
    }

    public ImDrag toImDrag(ImageComponent ic) {
        double startX = ic.componentXToImageSpace(start.x);
        double startY = ic.componentYToImageSpace(start.y);
        double endX = ic.componentXToImageSpace(end.x);
        double endY = ic.componentYToImageSpace(end.y);

        ImDrag imDrag = new ImDrag(startX, startY, endX, endY);
        return imDrag;
    }

    public void viewSizeChanged(View view) {
        if (view == this.view) {
            start.restoreCoordsFromImSpace(view);
            end.restoreCoordsFromImSpace(view);
            middle.restoreCoordsFromImSpace(view);
        } else { // in random tests they can be different
            if (Build.CURRENT.isDevelopment()) {
                System.out.println("GradientHandles::viewSizeChanged: different views, ui = "
                        + ImageArea.INSTANCE.getMode());
            }
        }
    }

    @VisibleForTesting
    public GradientDefiningPoint getStart() {
        return start;
    }

    @VisibleForTesting
    public GradientDefiningPoint getEnd() {
        return end;
    }

    @VisibleForTesting
    public GradientCenterPoint getMiddle() {
        return middle;
    }
}
