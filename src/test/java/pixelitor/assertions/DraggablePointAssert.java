/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.AnchorPointType;
import pixelitor.tools.pen.ControlPoint;
import pixelitor.tools.util.DraggablePoint;

import static java.lang.String.format;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

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
        if (dx > 0.1 || dy > 0.1) {
            throw new AssertionError(format(
                "found (%.1f, %.1f) instead of the expected (%.1f, %.1f)",
                actual.x, actual.y, x, y));
        }

        return this;
    }

    public DraggablePointAssert isAtIm(double x, double y) {
        isNotNull();

        double dImX = Math.abs(actual.imX - x);
        double dImY = Math.abs(actual.imY - y);
        if (dImX > 2 || dImY > 2) {
            throw new AssertionError(format(
                "found image coords (%.1f, %.1f) instead of the expected (%.1f, %.1f)",
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

        if (!(actual instanceof ControlPoint cp)) {
            throw new AssertionError("This is not an ControlPoint");
        }

        if (!cp.isRetracted()) {
            AnchorPoint anchor = cp.getAnchor();
            throw new AssertionError(format(
                "Not retracted: control is at (%.1f, %.1f), anchor is at (%.1f, %.1f)",
                cp.getX(), cp.getY(), anchor.getX(), anchor.getY()));
        }

        return this;
    }

    // can be called only on a ControlPoint
    public DraggablePointAssert isNotRetracted() {
        isNotNull();

        if (!(actual instanceof ControlPoint cp)) {
            throw new AssertionError("This is not an ControlPoint");
        }

        if (cp.isRetracted()) {
            throw new AssertionError("retracted");
        }

        return this;
    }

    // can be called only on an AnchorPoint
    public DraggablePointAssert bothControlsAreRetracted() {
        isNotNull();

        if (!(actual instanceof AnchorPoint ap)) {
            throw new AssertionError("This is not an AnchorPoint");
        }

        assertThat(ap.ctrlIn).isRetracted();
        assertThat(ap.ctrlOut).isRetracted();

        return this;
    }

    public DraggablePointAssert isActive() {
        isNotNull();

        if (!actual.isActive()) {
            throw new AssertionError("not active");
        }

        return this;
    }

    public DraggablePointAssert isNotActive() {
        isNotNull();

        if (actual.isActive()) {
            throw new AssertionError("active");
        }

        return this;
    }

    public DraggablePointAssert cursorNameIs(String expected) {
        isNotNull();

        String real = actual.getCursor().getName();
        if (!real.equals(expected)) {
            throw new AssertionError(format("expected '%s', found '%s'", expected, real));
        }

        return this;
    }
}
