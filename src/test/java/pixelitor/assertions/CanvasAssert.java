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

import java.awt.Dimension;

/**
 * Custom AssertJ assertions for {@link Canvas} objects.
 */
public class CanvasAssert extends AbstractAssert<CanvasAssert, Canvas> {
    /**
     * Creates a new <code>{@link CanvasAssert}</code> to make assertions on actual Canvas.
     *
     * @param actual the Canvas we want to make assertions on.
     */
    public CanvasAssert(Canvas actual) {
        super(actual, CanvasAssert.class);
    }

    public CanvasAssert hasSize(int width, int height) {
        isNotNull();

        int actualWidth = actual.getWidth();
        int actualHeight = actual.getHeight();
        if (actualWidth != width || actualHeight != height) {
            failWithMessage("""

                Expecting the image space size of:
                  <%s>
                to be:
                  <%dx%d>
                but was:
                  <%dx%d>""", actual, width, height, actualWidth, actualHeight);
        }

        return this;
    }

    public CanvasAssert hasCoSize(Dimension coSize) {
        return hasCoSize(coSize.width, coSize.height);
    }

    public CanvasAssert hasCoSize(int coWidth, int coHeight) {
        isNotNull();

        int actualCoWidth = actual.getCoWidth();
        int actualCoHeight = actual.getCoHeight();
        if (actualCoWidth != coWidth || actualCoHeight != coHeight) {
            failWithMessage("""

                Expecting component space size of:
                  <%s>
                to be:
                  <%dx%d>
                but was:
                  <%dx%d>""", actual, coWidth, coHeight, actualCoWidth, actualCoHeight);
        }

        return this;
    }
}
