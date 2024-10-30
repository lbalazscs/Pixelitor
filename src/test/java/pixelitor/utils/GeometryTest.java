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

package pixelitor.utils;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryTest {
    @Test
    void testingPerpendicularCalculation() {
        var C = new Point2D.Float();
        var R = new Point2D.Float(2, 2);
        float distance = (float) Math.sqrt(2);

        var A = new Point2D.Float();
        var B = new Point2D.Float();

        Geometry.calcPerpendicularPoints(C, R, distance, A, B);

//                   |
//                   |
//                   |       R
//               A   |
//                   |
//      _____________C____________
//                   |
//                   |   B
//                   |
//                   |

        assertEquals(-1.0f, A.x, 0.1);
        assertEquals(1.0f, A.y, 0.1);
        assertEquals(1.0f, B.x, 0.1);
        assertEquals(-1.0f, B.y, 0.1);
    }
}