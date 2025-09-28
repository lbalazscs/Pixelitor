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

package pixelitor.testutils;

import java.awt.Shape;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;

/**
 * Analyzes shapes by counting different types of path segments.
 */
public class SegmentCounter {
    private int moveToCount = 0;
    private int lineToCount = 0;
    private int quadToCount = 0;
    private int cubicToCount = 0;
    private int pathCloseCount = 0;

    public SegmentCounter(Shape shape) {
        var pathIterator = shape.getPathIterator(null);
        float[] coords = new float[6];
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);
            switch (type) {
                case SEG_MOVETO -> moveToCount++;
                case SEG_LINETO -> lineToCount++;
                case SEG_QUADTO -> quadToCount++;
                case SEG_CUBICTO -> cubicToCount++;
                case SEG_CLOSE -> pathCloseCount++;
                default -> throw new IllegalArgumentException("type = " + type);
            }

            pathIterator.next();
        }
    }

    public SegmentCounter assertMoveToCount(int expected) {
        verifySegmentCount(moveToCount, expected, "moveTo");
        return this;
    }

    public SegmentCounter assertLineToCount(int expected) {
        verifySegmentCount(lineToCount, expected, "lineTo");
        return this;
    }

    public SegmentCounter assertQuadToCount(int expected) {
        verifySegmentCount(quadToCount, expected, "quadTo");
        return this;
    }

    public SegmentCounter assertCubicToCount(int expected) {
        verifySegmentCount(cubicToCount, expected, "cubicTo");
        return this;
    }

    public SegmentCounter assertPathCloseCount(int expected) {
        verifySegmentCount(pathCloseCount, expected, "close");
        return this;
    }

    private static void verifySegmentCount(int actual, int expected, String segmentType) {
        if (actual != expected) {
            throw new AssertionError(String.format(
                "Expected %d %s segments but found %d",
                expected, segmentType, actual));
        }
    }
}
