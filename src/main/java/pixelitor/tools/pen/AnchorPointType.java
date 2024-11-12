/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.PAction;

import javax.swing.*;

/**
 * Defines how control handles of an anchor point behave relative
 * to each other when moved. Each type implements different constraints
 * between the incoming and outgoing control handles.
 */
public enum AnchorPointType {
    /**
     * Keeps the control handles both collinear and equidistant.
     * This creates the smoothest possible curve transition through the anchor point.
     */
    SYMMETRIC("Symmetric") {
        @Override
        void updateSibling(ControlPoint moved,
                           AnchorPoint anchor,
                           ControlPoint opposite) {
            double dx = moved.x - anchor.x;
            double dy = moved.y - anchor.y;

            opposite.setLocationOnlyForThis(anchor.x - dx, anchor.y - dy);
        }
    },
    /**
     * Maintains collinearity between the control points but allows different distances.
     */
    SMOOTH("Smooth") {
        @Override
        void updateSibling(ControlPoint moved,
                           AnchorPoint anchor,
                           ControlPoint opposite) {
            // preserve the distance, but adjust the angle to the new angle
            double dist = opposite.getRememberedDistFromAnchor();
            double newAngle = Math.PI + Math.atan2(
                moved.y - anchor.y,
                moved.x - anchor.x);

            double newX = anchor.x + dist * Math.cos(newAngle);
            double newY = anchor.y + dist * Math.sin(newAngle);
            opposite.setLocationOnlyForThis(newX, newY);
        }
    },
    /**
     * Allows independent movement of the control handles with no constraints.
     */
    CUSP("Cusp (free handles)") {
        @Override
        void updateSibling(ControlPoint moved,
                           AnchorPoint anchor,
                           ControlPoint opposite) {
            // do nothing: the control points are independent
        }
    };

    private final String displayName;

    AnchorPointType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Updates the position of the opposite control handle based
     * on the type's constraints when one control handle is moved.
     */
    abstract void updateSibling(ControlPoint moved,
                                AnchorPoint anchor,
                                ControlPoint opposite);

    /**
     * Creates a radio button menu item for switching to this handle type.
     */
    public JRadioButtonMenuItem createConvertMenuItem(AnchorPoint ap) {
        return new AnchorPointTypeMenuItem(ap, this);
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Menu item for changing an anchor point's type.
     */
    static class AnchorPointTypeMenuItem extends JRadioButtonMenuItem {
        public AnchorPointTypeMenuItem(AnchorPoint ap, AnchorPointType type) {
            super(new PAction(type.toString(), () -> ap.setType(type)));
            if (ap.getType() == type) {
                setSelected(true);
            }
        }
    }
}