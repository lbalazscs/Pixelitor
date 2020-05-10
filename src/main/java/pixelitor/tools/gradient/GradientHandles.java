/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.RunContext;
import pixelitor.gui.ImageArea;
import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.ImDrag;
import pixelitor.tools.util.UserDrag;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * The three handles that can be used to manipulate
 * a gradient by dragging
 */
public class GradientHandles implements ToolWidget {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;
    private final GradientCenterPoint middle;
    private final View view;

    public GradientHandles(Point2D start, Point2D end, View view) {
        this(start.getX(), start.getY(), end.getX(), end.getY(), view);
    }

    public GradientHandles(double startX, double startY,
                           double endX, double endY, View view) {
        this.view = view;
        Color defaultColor = Color.WHITE;
        Color activeColor = Color.RED;

        start = new GradientDefiningPoint("start", startX, startY, view,
                defaultColor, activeColor, this);
        end = new GradientDefiningPoint("end", endX, endY, view,
                defaultColor, activeColor, this);
        middle = new GradientCenterPoint(start, end, view, defaultColor, activeColor);

        end.setOther(start);
        end.setCenter(middle);
        start.setOther(end);
        start.setCenter(middle);
    }

    @Override
    public DraggablePoint handleWasHit(double x, double y) {
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

    @Override
    public void paint(Graphics2D g) {
        Shapes.drawGradientArrow(g, start.x, start.y, end.x, end.y);

        start.paintHandle(g);
        end.paintHandle(g);
        middle.paintHandle(g);
    }

    public ImDrag toImDrag(View view) {
        double startX = view.componentXToImageSpace(start.x);
        double startY = view.componentYToImageSpace(start.y);
        double endX = view.componentXToImageSpace(end.x);
        double endY = view.componentYToImageSpace(end.y);

        ImDrag imDrag = new ImDrag(startX, startY, endX, endY);
        return imDrag;
    }

    public UserDrag toUserDrag(GradientDefiningPoint movingPoint) {
        UserDrag ud;
        if (movingPoint == end) {
            ud = new UserDrag();
            ud.setStart(start.asPPoint());
            ud.setEnd(end.asPPoint());
        } else if (movingPoint == start) {
            // if the user is moving the start point, then return
            // an UserDrag that points backwards, but calculates
            // the forward angle
            ud = new UserDrag() {
                @Override
                public double calcAngle() {
                    return calcReversedAngle();
                }
            };
            ud.setStart(end.asPPoint());
            ud.setEnd(start.asPPoint());
        } else {
            throw new IllegalStateException("movingPoint = " + movingPoint);
        }
        return ud;
    }

    @Override
    public void coCoordsChanged(View view) {
        if (view == this.view) {
            start.restoreCoordsFromImSpace(view);
            end.restoreCoordsFromImSpace(view);
            middle.restoreCoordsFromImSpace(view);
        } else { // in random tests they can be different
            if (RunContext.isDevelopment()) {
                System.out.println("GradientHandles::viewSizeChanged: different views, ui = "
                        + ImageArea.getMode());
            }
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at) {
        start.imTransformOnlyThis(at, false);
        end.imTransformOnlyThis(at, false);
        middle.imTransformOnlyThis(at, false);
    }

    @Override
    public void arrowKeyPressed(ArrowKey key) {
        middle.arrowKeyPressed(key);
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
