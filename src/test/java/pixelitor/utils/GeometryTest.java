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

package pixelitor.utils;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
        float tolerance = 0.01f;
        assertThat(A.x).isCloseTo(-1.0f, within(tolerance));
        assertThat(A.y).isCloseTo(1.0f, within(tolerance));
        assertThat(B.x).isCloseTo(1.0f, within(tolerance));
        assertThat(B.y).isCloseTo(-1.0f, within(tolerance));
    }
}
