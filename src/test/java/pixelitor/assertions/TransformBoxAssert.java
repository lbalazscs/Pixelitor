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
import pixelitor.tools.transform.TransformBox;

import java.awt.geom.Dimension2D;

import static java.lang.String.format;

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

        int degrees = actual.getAngleDegrees();
        if (degrees != expected) {
            throw new AssertionError("Expected " + expected + ", found " + degrees);
        }

        return this;
    }

    public TransformBoxAssert rotSizeIs(double w, double h) {
        isNotNull();

        Dimension2D size = actual.getRotatedImSize();
        double width = size.getWidth();
        if (Math.abs(width - w) > DOUBLE_TOLERANCE) {
            throw new AssertionError(format("Expected width %.2f, found %.2f", w, width));
        }

        double height = size.getHeight();
        if (Math.abs(height - h) > DOUBLE_TOLERANCE) {
            throw new AssertionError(format("Expected height %.2f, found %.2f", h, height));
        }

        return this;
    }
}