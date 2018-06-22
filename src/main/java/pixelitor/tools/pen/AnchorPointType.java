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

/**
 * The type of an anchor point determines how its control
 * handles behave relative to each other.
 */
enum AnchorPointType {
    /**
     * The control handles are both collinear and equidistant.
     */
    SYMMETRIC(true) {
        @Override
        void setLocationOfOtherControl(double x, double y,
                                       AnchorPoint anchor, ControlPoint other) {
            double dx = x - anchor.x;
            double dy = y - anchor.y;

            other.setLocationOnlyForThis(anchor.x - dx, anchor.y - dy);
        }
    },
    /**
     * Collinear, but the handles don't necessarily have the same length
     */
    SMOOTH(true) {
        @Override
        void setLocationOfOtherControl(double x, double y,
                                       AnchorPoint anchor, ControlPoint other) {
            // keep the distance, but adjust the angle to the new angle
            double dist = other.getRememberedDistanceFromAnchor();
            double newAngle = Math.PI + Math.atan2(y - anchor.y, x - anchor.x);

            double newX = anchor.x + dist * Math.cos(newAngle);
            double newY = anchor.y + dist * Math.sin(newAngle);
            other.setLocationOnlyForThis(newX, newY);
        }
    },
    /**
     * The two control handles are totally independent,
     * making sharp corners possible
     */
    CUSP(false) {
        @Override
        void setLocationOfOtherControl(double x, double y,
                                       AnchorPoint anchor, ControlPoint other) {
            // do nothing: the control points are independent
        }
    };

    private final boolean dependent;

    AnchorPointType(boolean dependent) {
        this.dependent = dependent;
    }

    public boolean isDependent() {
        return dependent;
    }

    abstract void setLocationOfOtherControl(double x, double y,
                                            AnchorPoint anchor, ControlPoint other);
}
