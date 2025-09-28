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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.AnchorPointType;
import pixelitor.tools.pen.ControlPoint;
import pixelitor.tools.util.DraggablePoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Custom AssertJ assertions for {@link DraggablePoint} objects.
 */
public class DraggablePointAssert extends AbstractAssert<DraggablePointAssert, DraggablePoint> {
    public DraggablePointAssert(DraggablePoint actual) {
        super(actual, DraggablePointAssert.class);
    }

    /**
     * Asserts that the point is at the given component-space location.
     */
    public DraggablePointAssert isAt(double x, double y) {
        isNotNull();
        assertThat(actual.getX()).as("x").isCloseTo(x, within(0.1));
        assertThat(actual.getY()).as("y").isCloseTo(y, within(0.1));
        return this;
    }

    /**
     * Asserts that the point is at the given image-space location.
     */
    public DraggablePointAssert isAtIm(double x, double y) {
        isNotNull();
        assertThat(actual.imX).as("imX").isCloseTo(x, within(2.0));
        assertThat(actual.imY).as("imY").isCloseTo(y, within(2.0));
        return this;
    }

    /**
     * Asserts that the point is an {@link AnchorPoint} of the given type.
     */
    public DraggablePointAssert typeIs(AnchorPointType expected) {
        isNotNull();
        isInstanceOf(AnchorPoint.class);
        assertThat(((AnchorPoint) actual).getType()).isSameAs(expected);
        return this;
    }

    /**
     * Asserts that the point is a {@link ControlPoint} and is retracted.
     */
    public DraggablePointAssert isRetracted() {
        isNotNull();
        isInstanceOf(ControlPoint.class);
        assertThat(((ControlPoint) actual).isRetracted()).as("isRetracted").isTrue();
        return this;
    }

    /**
     * Asserts that the point is a {@link ControlPoint} and is not retracted.
     */
    public DraggablePointAssert isNotRetracted() {
        isNotNull();
        isInstanceOf(ControlPoint.class);
        assertThat(((ControlPoint) actual).isRetracted()).as("isRetracted").isFalse();
        return this;
    }

    /**
     * Asserts that the point is an {@link AnchorPoint} and both of its control points are retracted.
     */
    public DraggablePointAssert bothControlsAreRetracted() {
        isNotNull();
        isInstanceOf(AnchorPoint.class);
        AnchorPoint ap = (AnchorPoint) actual;
        assertThat(ap.ctrlIn).isRetracted();
        assertThat(ap.ctrlOut).isRetracted();
        return this;
    }

    public DraggablePointAssert isActive() {
        isNotNull();
        assertThat(actual.isActive()).as("isActive").isTrue();
        return this;
    }

    public DraggablePointAssert isNotActive() {
        isNotNull();
        assertThat(actual.isActive()).as("isActive").isFalse();
        return this;
    }

    public DraggablePointAssert cursorNameIs(String expected) {
        isNotNull();
        assertThat(actual.getCursor().getName()).isEqualTo(expected);
        return this;
    }
}
