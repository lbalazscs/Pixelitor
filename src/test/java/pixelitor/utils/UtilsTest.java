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

package pixelitor.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Utils tests")
public class UtilsTest {
    @Test
    void angleFunctions() {
        for (double a = -Math.PI; a < Math.PI; a += 0.1) {
            double intuitive = Utils.atan2AngleToIntuitive(a);
            double atan = Utils.intuitiveToAtan2Angle(intuitive);
            Assertions.assertEquals(atan, a, 0.01);
        }

        for (double a = 0.0; a < 2 * Math.PI; a += 0.1) {
            double atan = Utils.intuitiveToAtan2Angle(a);
            double b = Utils.atan2AngleToIntuitive(atan);
            Assertions.assertEquals(a, b, 0.01);
        }
    }
}