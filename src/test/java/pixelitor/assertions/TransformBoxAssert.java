/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Custom AssertJ assertions for {@link TransformBox} objects.
 */
public class TransformBoxAssert extends AbstractAssert<TransformBoxAssert, TransformBox> {
    private static final double DOUBLE_TOLERANCE = 0.001;

    public TransformBoxAssert(TransformBox actual) {
        super(actual, TransformBoxAssert.class);
    }

    public TransformBoxAssert angleDegreesIs(int expected) {
        isNotNull();

        assertThat(actual.getAngleDegrees()).isEqualTo(expected);

        return this;
    }

    public TransformBoxAssert rotSizeIs(double expectedWidth, double expectedHeight) {
        isNotNull();

        Dimension2D size = actual.getRotatedImSize();
        assertThat(size.getWidth()).isCloseTo(expectedWidth, within(DOUBLE_TOLERANCE));
        assertThat(size.getHeight()).isCloseTo(expectedHeight, within(DOUBLE_TOLERANCE));

        return this;
    }

    public TransformBoxAssert handleImPosIs(Function<TransformBox, DraggablePoint> getter,
                                            double expectedImX, double expectedImY) {
        isNotNull();

        DraggablePoint handle = getter.apply(actual);
        assertThat(handle.getImX()).isCloseTo(expectedImX, within(DOUBLE_TOLERANCE));
        assertThat(handle.getImY()).isCloseTo(expectedImY, within(DOUBLE_TOLERANCE));

        return this;
    }
}