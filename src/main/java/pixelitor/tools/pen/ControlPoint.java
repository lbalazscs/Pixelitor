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

package pixelitor.tools.pen;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.DraggablePoint;

import java.awt.Color;

/**
 * A control point of a {@link AnchorPoint}
 */
public class ControlPoint extends DraggablePoint {
    private final AnchorPoint anchor;
    private ControlPoint sibling;

    // this needs to be remembered because otherwise the distance keeps
    // drifting for smooth anchors because of accumulating rounding errors
    private double rememberedDistanceFromAnchor;

    public ControlPoint(String name, int x, int y, ImageComponent ic, AnchorPoint anchor, Color color, Color activeColor) {
        super(name, x, y, ic, color, activeColor);
        this.anchor = anchor;
    }

    public void setSibling(ControlPoint sibling) {
        this.sibling = sibling;
    }

    @Override
    public void setLocation(int x, int y) {
        anchor.getType().setLocationOfOtherControl(x, y, anchor, sibling);

        super.setLocation(x, y);
    }

    @Override
    protected void afterMouseReleasedActions() {
        calcImCoords();
        rememberDistFromAnchor();
        if (anchor.getType().isDependent()) {
            sibling.calcImCoords();
            sibling.rememberDistFromAnchor();
        }
    }

    private void rememberDistFromAnchor() {
        rememberedDistanceFromAnchor = distanceFrom(anchor);
    }

    public double getRememberedDistanceFromAnchor() {
        return rememberedDistanceFromAnchor;
    }

    @Override
    public String toString() {
        if (samePositionAs(anchor)) {
            return super.toString() + " retracted!";
        }
        return super.toString();
    }
}
