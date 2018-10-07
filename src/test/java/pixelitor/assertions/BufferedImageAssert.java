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

import java.awt.image.BufferedImage;

/**
 * Custom AssertJ assertions for {@link BufferedImage} objects.
 */
public class BufferedImageAssert extends AbstractAssert<BufferedImageAssert, BufferedImage> {
    public BufferedImageAssert(BufferedImage actual) {
        super(actual, BufferedImageAssert.class);
    }

    public BufferedImageAssert widthIs(int width) {
        isNotNull();

        String msg = "\nExpecting width of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        int actualWidth = actual.getWidth();
        if (actualWidth != width) {
            failWithMessage(msg, actual, width, actualWidth);
        }

        return this;
    }

    public BufferedImageAssert heightIs(int height) {
        isNotNull();

        String msg = "\nExpecting height of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        int actualHeight = actual.getHeight();
        if (actualHeight != height) {
            failWithMessage(msg, actual, height, actualHeight);
        }

        return this;
    }
}
