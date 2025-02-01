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

package pixelitor.tools.pen;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.Serial;

import static pixelitor.tools.pen.AnchorPoint.RETRACTION_TOLERANCE;
import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

/**
 * A control point of an {@link AnchorPoint}.
 */
public class ControlPoint extends DraggablePoint {
    @Serial
    private static final long serialVersionUID = 8776344572399099909L;

    private final AnchorPoint anchor;

    // each control point has a sibling on the opposite side of their shared anchor point
    private ControlPoint sibling;

    // this needs to be cached because otherwise the distance keeps
    // drifting for smooth anchors as rounding errors accumulate
    private double rememberedDistFromAnchor;

    public ControlPoint(String name, PPoint pos, View view, AnchorPoint anchor) {
        super(name, pos, view);
        this.anchor = anchor;
    }

    public void setSibling(ControlPoint sibling) {
        this.sibling = sibling;
    }

    @Override
    protected Shape createShape(double startX, double startY) {
        return new Ellipse2D.Double(startX, startY, HANDLE_SIZE, HANDLE_SIZE);
    }

    @Override
    public void setLocation(double coX, double coY) {
        super.setLocation(coX, coY);

        // also update the sibling's position based on
        // the anchor point's type constraints
        anchor.getType().updateSibling(this, anchor, sibling);
    }

    /**
     * Moves the control point while maintaining angle constraints.
     * The point will snap to common angles relative to its anchor point.
     */
    @Override
    public void setConstrainedLocation(double mouseX, double mouseY) {
        Point2D p = Utils.constrainToNearestAngle(anchor.x, anchor.y, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    @Override
    protected void afterMouseReleasedActions() {
        if (anchor.getType() == AnchorPointType.SMOOTH) {
            rememberDistFromAnchor();
            sibling.rememberDistFromAnchor();
        }
    }

    public void rememberDistFromAnchor() {
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
        if (that instanceof ControlPoint thatCP) { // should be always the case
            rememberedDistFromAnchor = thatCP.getRememberedDistFromAnchor();
        }
    }

    public boolean isRetracted() {
        return isRetracted(RETRACTION_TOLERANCE);
    }

    public boolean isRetracted(double epsilon) {
        return hasSameImPosAs(anchor, epsilon);
    }

    void retract() {
        setLocationOnlyForThis(anchor.x, anchor.y);
        rememberedDistFromAnchor = 0;
    }

    /**
     * Breaks the handle, except when it's retracted:
     * then prepares dragging it out symmetrically.
     */
    public void breakOrDragOut() {
        anchor.setType(isRetracted() ? SYMMETRIC : CUSP);
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (active) {
            AnchorPoint.recentlyEditedPoint = anchor;
        }
    }

    @Override
    public String getMoveEditName() {
        return "Move Control Handle";
    }

    @Override
    public String toString() {
        return isRetracted()
            ? super.toString() + " retracted!"
            : super.toString();
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = super.createDebugNode();

        node.addBoolean("retracted", isRetracted());
        node.addDouble("cached dist from anchor", getRememberedDistFromAnchor());

        return node;
    }
}
