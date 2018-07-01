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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.tools.DraggablePoint;
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.AnchorPointType;
import pixelitor.tools.pen.ControlPoint;

/**
 * Custom AssertJ assertions for {@link DraggablePoint} objects.
 */
public class DraggablePointAssert extends AbstractAssert<DraggablePointAssert, DraggablePoint> {
    public DraggablePointAssert(DraggablePoint actual) {
        super(actual, DraggablePointAssert.class);
    }

    public DraggablePointAssert isAt(double x, double y) {
        isNotNull();

        double dx = Math.abs(actual.x - x);
        double dy = Math.abs(actual.y - y);
        if ((dx > 0.1) || (dy > 0.1)) {
            throw new AssertionError(String.format(
                    "found (%.2f, %.2f) instead of the expected (%.2f, %.2f)",
                    actual.x, actual.y, x, y));
        }

        return this;
    }

    public DraggablePointAssert isAtIm(double x, double y) {
        isNotNull();

        double dImX = Math.abs(actual.imX - x);
        double dImY = Math.abs(actual.imY - y);
        if (dImX > 2 || dImY > 2) {
            throw new AssertionError(String.format(
                    "found image coords (%.2f, %.2f) instead of the expected (%.2f, %.2f)",
                    actual.imX, actual.imY, x, y));
        }

        return this;
    }

    // can be called only on an AnchorPoint
    public DraggablePointAssert anchorPointTypeIs(AnchorPointType expected) {
        isNotNull();

        if (!(actual instanceof AnchorPoint)) {
            throw new AssertionError("This is not an AnchorPoint");
        }

        AnchorPointType type = ((AnchorPoint) actual).getType();
        if (type != expected) {
            throw new AssertionError("Type is " + type + ", expected " + expected);
        }

        return this;
    }

    // can be called only on a ControlPoint
    public DraggablePointAssert isRetracted() {
        isNotNull();

        if (!(actual instanceof ControlPoint)) {
            throw new AssertionError("This is not an ControlPoint");
        }

        ControlPoint cp = (ControlPoint) actual;
        if (!cp.isRetracted()) {
            throw new AssertionError("not retracted");
        }

        return this;
    }
}
