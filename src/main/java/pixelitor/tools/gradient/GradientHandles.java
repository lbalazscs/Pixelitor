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

package pixelitor.tools.gradient;

import pixelitor.AppMode;
import pixelitor.gui.ImageArea;
import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.CustomShapes;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * A controller/container class for the three draggable
 * handles that can be used to manipulate a gradient.
 */
public class GradientHandles implements ToolWidget, Debuggable {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;
    private final GradientCenterPoint middle;
    private final View view;

    public GradientHandles(PPoint startPos, PPoint endPos, View view) {
        this.view = view;

        start = new GradientDefiningPoint("start", startPos, view, this);
        end = new GradientDefiningPoint("end", endPos, view, this);
        middle = new GradientCenterPoint(start, end, view);

        end.setOther(start);
        end.setCenter(middle);
        start.setOther(end);
        start.setCenter(middle);
    }

    @Override
    public DraggablePoint findHandleAt(double coX, double coY) {
        if (end.contains(coX, coY)) {
            return end;
        }
        if (start.contains(coX, coY)) {
            return start;
        }
        if (middle.contains(coX, coY)) {
            return middle;
        }
        return null;
    }

    @Override
    public void paint(Graphics2D g) {
        CustomShapes.drawDirectionArrow(g, start.x, start.y, end.x, end.y);

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

    public Drag toOverlayDrag(GradientDefiningPoint movedPoint) {
        Drag drag;
        if (movedPoint == end) {
            drag = new Drag();
            drag.setStart(start.getLocationCopy());
            drag.setEnd(end.getLocationCopy());
        } else if (movedPoint == start) {
            // if the user is moving the start point, then return a Drag
            // that points backwards, but calculates the forward angle,
            // because the overlay measurement is shown near the
            // Drag's end, but we want it to be near the moved start
            drag = new Drag() {
                @Override
                public double calcAngle() {
                    // compensate for the fact that the Drag is backwards
                    return calcReversedAngle();
                }
            };
            drag.setStart(end.getLocationCopy());
            drag.setEnd(start.getLocationCopy());
        } else {
            throw new IllegalStateException("movedPoint = " + movedPoint);
        }
        return drag;
    }

    @Override
    public void coCoordsChanged(View view) {
        if (view == this.view) {
            start.restoreCoordsFromImSpace(view);
            end.restoreCoordsFromImSpace(view);
            middle.restoreCoordsFromImSpace(view);
        } else {
            if (AppMode.isDevelopment()) {
                throw new IllegalStateException("different views, ui = " + ImageArea.getMode());
            }
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        assert view == this.view;

        start.imTransformOnlyThis(at, false);
        end.imTransformOnlyThis(at, false);
        middle.imTransformOnlyThis(at, false);
    }

    @Override
    public void arrowKeyPressed(ArrowKey key, View view) {
        assert view == this.view;

        // arrow keys move the entire gradient by nudging the center point
        middle.arrowKeyPressed(key);
    }

    public GradientDefiningPoint getStart() {
        return start;
    }

    public GradientDefiningPoint getEnd() {
        return end;
    }

    public GradientCenterPoint getMiddle() {
        return middle;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);
        node.add(start.createDebugNode());
        node.add(middle.createDebugNode());
        node.add(end.createDebugNode());
        return node;
    }
}
