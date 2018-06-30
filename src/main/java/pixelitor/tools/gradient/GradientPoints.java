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

import pixelitor.gui.ImageComponent;
import pixelitor.gui.View;
import pixelitor.tools.DraggablePoint;
import pixelitor.tools.ImDrag;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * The three points that can be used to manipulate
 * a gradient by dragging handles
 */
public class GradientPoints {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;
    private final GradientCenterPoint middle;
    private final ImageComponent ic;

    public GradientPoints(int startX, int startY, int endX, int endY, ImageComponent ic) {
        this.ic = ic;
        Color defaultColor = Color.WHITE;
        Color activeColor = Color.RED;

        start = new GradientDefiningPoint("start", startX, startY, ic, defaultColor, activeColor);
        end = new GradientDefiningPoint("end", endX, endY, ic, defaultColor, activeColor);
        middle = new GradientCenterPoint(start, end, ic, defaultColor, activeColor);

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
        assert view == this.ic;
        start.restoreCoordsFromImSpace(view);
        end.restoreCoordsFromImSpace(view);
        middle.restoreCoordsFromImSpace(view);
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
