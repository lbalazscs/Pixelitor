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

package pixelitor.testutils;

import java.awt.Shape;

import static java.awt.geom.PathIterator.*;

public class ShapeChecker {
    private int numMoveTos = 0;
    private int numLineTos = 0;
    private int numQuadTos = 0;
    private int numCubicTos = 0;
    private int numCloses = 0;

    public ShapeChecker(Shape shape) {
        var pathIterator = shape.getPathIterator(null);
        float[] coords = new float[6];
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);
            switch (type) {
                case SEG_MOVETO -> numMoveTos++;
                case SEG_LINETO -> numLineTos++;
                case SEG_QUADTO -> numQuadTos++;
                case SEG_CUBICTO -> numCubicTos++;
                case SEG_CLOSE -> numCloses++;
                default -> throw new IllegalArgumentException("type = " + type);
            }

            pathIterator.next();
        }
    }

    public void assertNumMoveTosIs(int expected) {
        if (numMoveTos != expected) {
            throw new AssertionError("numMoveTos = " + numMoveTos
                + ", expected = " + expected);
        }
    }

    public void assertNumLineTosIs(int expected) {
        if (numLineTos != expected) {
            throw new AssertionError("numLineTos = " + numLineTos
                + ", expected = " + expected);
        }
    }

    public void assertNumQuadTosIs(int expected) {
        if (numQuadTos != expected) {
            throw new AssertionError("numQuadTos = " + numQuadTos
                + ", expected = " + expected);
        }
    }

    public void assertNumCubicTosIs(int expected) {
        if (numCubicTos != expected) {
            throw new AssertionError("numCubicTos = " + numCubicTos
                + ", expected = " + expected);
        }
    }

    public void assertNumClosesIs(int expected) {
        if (numCloses != expected) {
            throw new AssertionError("numCloses = " + numCloses
                + ", expected = " + expected);
        }
    }
}
