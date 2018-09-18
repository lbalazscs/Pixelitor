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

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.Ansi;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 * A control point of a {@link AnchorPoint}
 */
public class ControlPoint extends DraggablePoint {
    private final AnchorPoint anchor;
    private ControlPoint sibling;

    // this needs to be remembered because otherwise the distance keeps
    // drifting for smooth anchors as rounding errors accumulate
    private double rememberedDistFromAnchor;

    public ControlPoint(String name, double x, double y, View view,
                        AnchorPoint anchor, Color color, Color activeColor) {
        super(name, x, y, view, color, activeColor);
        this.anchor = anchor;
    }

    @Override
    protected Shape createShape(double startX, double startY, double size) {
        return new Ellipse2D.Double(startX, startY, size, size);
    }

    public void setSibling(ControlPoint sibling) {
        this.sibling = sibling;
    }

    @Override
    public void setLocation(double x, double y) {
        anchor.getType().setLocationOfOtherControl(x, y, anchor, sibling);

        super.setLocation(x, y);
    }

    @Override
    public void setConstrainedLocation(double mouseX, double mouseY) {
        // constrain it relative to the anchor
        Point2D p = Utils.constrainEndPoint(anchor.x, anchor.y, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    @Override
    protected void afterMouseReleasedActions() {
        afterMovingActionsForThis();
        if (anchor.getType().isDependent()) {
            sibling.afterMovingActionsForThis();
        }
    }

    public void afterMovingActionsForThis() {
        calcImCoords();
        rememberDistFromAnchor();
    }

    private void rememberDistFromAnchor() {
        rememberedDistFromAnchor = distanceFrom(anchor);
    }

    public double getRememberedDistFromAnchor() {
        return rememberedDistFromAnchor;
    }

    public AnchorPoint getAnchor() {
        return anchor;
    }

    public ControlPoint getSibling() {
        return sibling;
    }

    @Override
    public void copyPositionFrom(DraggablePoint that) {
        super.copyPositionFrom(that);
        if (that instanceof ControlPoint) { // should be always the case
            rememberedDistFromAnchor = ((ControlPoint) that).getRememberedDistFromAnchor();
        }
    }

    public boolean isRetracted() {
        return samePositionAs(anchor);
    }

    public boolean isRetracted(double epsilon) {
        return samePositionAs(anchor, epsilon);
    }

    @Override
    public String toColoredString() {
        if (isRetracted()) {
            return super.toColoredString() + Ansi.red(" retracted!");
        }
        return super.toColoredString();
    }

    @Override
    public String toString() {
        if (isRetracted()) {
            return super.toString() + " retracted!";
        }
        return super.toString();
    }

    void retract() {
        setLocationOnlyForThis(anchor.x, anchor.y);
        rememberedDistFromAnchor = 0;
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (active) {
            AnchorPoint.recentlyEditedPoint = anchor;
        }
    }
}
