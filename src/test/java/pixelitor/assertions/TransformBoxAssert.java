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
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DraggablePoint;

import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Custom AssertJ assertions for {@link TransformBox} objects.
 */
public class TransformBoxAssert extends AbstractAssert<TransformBoxAssert, TransformBox> {
    private static final double DOUBLE_TOLERANCE = 0.001;

    public TransformBoxAssert(TransformBox actual) {
        super(actual, TransformBoxAssert.class);
    }

    /**
     * Asserts that the transform box's rotation angle is the given value in degrees.
     */
    public TransformBoxAssert angleDegreesIs(int expected) {
        isNotNull();

        assertThat(actual.getAngleDegrees()).isEqualTo(expected);

        return this;
    }

    /**
     * Asserts that the transform box's rotated image size matches the given width and height.
     */
    public TransformBoxAssert rotSizeIs(double width, double height) {
        isNotNull();

        Dimension2D size = actual.getRotatedImSize();
        assertThat(size.getWidth())
            .as("width")
            .isCloseTo(width, within(DOUBLE_TOLERANCE));
        assertThat(size.getHeight())
            .as("height")
            .isCloseTo(height, within(DOUBLE_TOLERANCE));

        return this;
    }

    /**
     * Asserts that a specific handle is at the given image-space coordinates.
     */
    public TransformBoxAssert handleImPosIs(Function<TransformBox, DraggablePoint> getter,
                                            double x, double y) {
        isNotNull();

        DraggablePoint handle = getter.apply(actual);
        assertThat(handle.getImX())
            .as("im x")
            .isCloseTo(x, within(DOUBLE_TOLERANCE));
        assertThat(handle.getImY())
            .as("im y")
            .isCloseTo(y, within(DOUBLE_TOLERANCE));

        return this;
    }

    /**
     * Asserts that the transform box's corners are at the given component-space coordinates.
     */
    public TransformBoxAssert hasCornersAt(Point2D nw, Point2D sw, Point2D ne, Point2D se) {
        isNotNull();
        assertThat(actual.getNW()).isAt(nw.getX(), nw.getY());
        assertThat(actual.getSW()).isAt(sw.getX(), sw.getY());
        assertThat(actual.getNE()).isAt(ne.getX(), ne.getY());
        assertThat(actual.getSE()).isAt(se.getX(), se.getY());
        return this;
    }

    /**
     * Asserts that the transform box's corners are at the given image-space coordinates.
     */
    public TransformBoxAssert hasCornersAtIm(Point2D nw, Point2D sw, Point2D ne, Point2D se) {
        isNotNull();
        assertThat(actual.getNW()).isAtIm(nw.getX(), nw.getY());
        assertThat(actual.getSW()).isAtIm(sw.getX(), sw.getY());
        assertThat(actual.getNE()).isAtIm(ne.getX(), ne.getY());
        assertThat(actual.getSE()).isAtIm(se.getX(), se.getY());
        return this;
    }
}
