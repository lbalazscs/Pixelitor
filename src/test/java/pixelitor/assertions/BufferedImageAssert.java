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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.Canvas;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Custom AssertJ assertions for {@link BufferedImage} objects.
 */
@SuppressWarnings("UnusedReturnValue")
public class BufferedImageAssert extends AbstractAssert<BufferedImageAssert, BufferedImage> {
    public BufferedImageAssert(BufferedImage actual) {
        super(actual, BufferedImageAssert.class);
    }

    public BufferedImageAssert hasSameSizeAs(Canvas canvas) {
        return sizeIs(canvas.getWidth(), canvas.getHeight());
    }

    public BufferedImageAssert hasSameSizeAs(Rectangle rect) {
        return sizeIs(rect.width, rect.height);
    }

    public BufferedImageAssert hasSameSizeAs(BufferedImage img) {
        return sizeIs(img.getWidth(), img.getHeight());
    }

    public BufferedImageAssert sizeIs(int width, int height) {
        isNotNull();

        int actualWidth = actual.getWidth();
        int actualHeight = actual.getHeight();
        if (actualWidth != width || actualHeight != height) {
            failWithMessage("""

                Expecting the size of:
                  <%s>
                to be:
                  <%dx%d>
                but was:
                  <%dx%d>""", actual, width, height, actualWidth, actualHeight);
        }

        return this;
    }
}
