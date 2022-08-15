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

package pixelitor.tools.gradient;

import pixelitor.AppContext;
import pixelitor.gui.ImageArea;
import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * The three handles that can be used to manipulate a gradient.
 */
public class GradientHandles implements ToolWidget {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;
    private final GradientCenterPoint middle;
    private final View view;

    public GradientHandles(PPoint startPos, PPoint endPos, View view) {
        this.view = view;
        Color defaultColor = Color.WHITE;
        Color activeColor = Color.RED;

        start = new GradientDefiningPoint("start", startPos, view,
            defaultColor, activeColor, this);
        end = new GradientDefiningPoint("end", endPos, view,
            defaultColor, activeColor, this);
        middle = new GradientCenterPoint(start, end, view, defaultColor, activeColor);

        end.setOther(start);
        end.setCenter(middle);
        start.setOther(end);
        start.setCenter(middle);
    }

    @Override
    public DraggablePoint findHandleAt(double x, double y) {
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
        Shapes.drawDirectionArrow(g, start.x, start.y, end.x, end.y);

        start.paintHandle(g);
        end.paintHandle(g);
        middle.paintHandle(g);
    }

    public Drag toDrag(View view) {
        double startX = view.componentXToImageSpace(start.x);
        double startY = view.componentYToImageSpace(start.y);
        double endX = view.componentXToImageSpace(end.x);
        double endY = view.componentYToImageSpace(end.y);

        return new Drag(startX, startY, endX, endY);
    }

    public Drag toDrag(GradientDefiningPoint movingPoint) {
        Drag drag;
        if (movingPoint == end) {
            drag = new Drag();
            drag.setStart(start.asPPoint());
            drag.setEnd(end.asPPoint());
        } else if (movingPoint == start) {
            // if the user is moving the start point, then return
            // a Drag that points backwards, but calculates
            // the forward angle
            drag = new Drag() {
                @Override
                public double calcAngle() {
                    return calcReversedAngle();
                }
            };
            drag.setStart(end.asPPoint());
            drag.setEnd(start.asPPoint());
        } else {
            throw new IllegalStateException("movingPoint = " + movingPoint);
        }
        return drag;
    }

    @Override
    public void coCoordsChanged(View view) {
        if (view == this.view) {
            start.restoreCoordsFromImSpace(view);
            end.restoreCoordsFromImSpace(view);
            middle.restoreCoordsFromImSpace(view);
        } else { // in random tests they can be different
            if (AppContext.isDevelopment()) {
                System.out.println("GradientHandles::viewSizeChanged: different views, ui = "
                    + ImageArea.getMode());
            }
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        start.imTransformOnlyThis(at, false);
        end.imTransformOnlyThis(at, false);
        middle.imTransformOnlyThis(at, false);
    }

    @Override
    public void arrowKeyPressed(ArrowKey key, View view) {
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

    public DebugNode createDebugNode() {
        DebugNode node = new DebugNode("handles", this);
        node.add(start.createDebugNode());
        node.add(middle.createDebugNode());
        node.add(end.createDebugNode());
        return node;
    }
}
