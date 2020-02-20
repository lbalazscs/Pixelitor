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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {
    @ParameterizedTest(name = "\"{0}\" should be parsed as {1}")
    @CsvSource({"1.8.0_161, 8", "9.0.1, 9", "10.0.1, 10", "11, 11"})
    void parseJavaVersion(String s, int versionNum) {
        int parsed = Utils.parseJavaVersion(s);
        assertThat(parsed).isEqualTo(versionNum);
    }

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