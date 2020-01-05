/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

public class ShapeChecker {
    private int numMoveTos = 0;
    private int numLineTos = 0;
    private int numQuadTos = 0;
    private int numCubicTos = 0;
    private int numCloses = 0;

    public ShapeChecker(Shape shape) {
        var it = shape.getPathIterator(null);
        float[] coords = new float[6];
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
//            float x = coords[0];
//            float y = coords[1];
//            float xx = coords[2];
//            float yy = coords[3];
//            float xxx = coords[4];
//            float yyy = coords[5];

            switch (type) {
                case SEG_MOVETO:
                    numMoveTos++;
                    break;
                case SEG_LINETO:
                    numLineTos++;
                    break;
                case SEG_QUADTO:
                    numQuadTos++;
                    break;
                case SEG_CUBICTO:
                    numCubicTos++;
                    break;
                case SEG_CLOSE:
                    numCloses++;
                    break;
                default:
                    throw new IllegalArgumentException("type = " + type);
            }

            it.next();
        }
    }

    public void assertNumMoveTosWas(int expected) {
        if (numMoveTos != expected) {
            throw new AssertionError("numMoveTos = " + numMoveTos
                    + ", expected = " + expected);
        }
    }

    public void assertNumLineTosWas(int expected) {
        if (numLineTos != expected) {
            throw new AssertionError("numLineTos = " + numLineTos
                    + ", expected = " + expected);
        }
    }

    public void assertNumQuadTosWas(int expected) {
        if (numQuadTos != expected) {
            throw new AssertionError("numQuadTos = " + numQuadTos
                    + ", expected = " + expected);
        }
    }

    public void assertNumCubicTosWas(int expected) {
        if (numCubicTos != expected) {
            throw new AssertionError("numCubicTos = " + numCubicTos
                    + ", expected = " + expected);
        }
    }

    public void assertNumClosesWas(int expected) {
        if (numCloses != expected) {
            throw new AssertionError("numCloses = " + numCloses
                    + ", expected = " + expected);
        }
    }

}
