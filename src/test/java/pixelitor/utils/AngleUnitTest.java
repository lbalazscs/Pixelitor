/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AngleUnitTest {
    private static final double EPSILON = 1e-9;

    @ParameterizedTest
    @EnumSource(AngleUnit.class)
    void roundTrip(AngleUnit fromUnit) {
        double[] testAngles = switch (fromUnit) {
            case RADIANS, INTUITIVE_RADIANS -> new double[]{
                Math.PI / 6, Math.PI / 4, Math.PI / 3, Math.PI / 2
            };
            case DEGREES, INTUITIVE_DEGREES -> new double[]{
                30.0, 45.0, 60.0, 90.0
            };
        };

        for (double angle : testAngles) {
            for (AngleUnit toUnit : AngleUnit.values()) {
                double converted = convert(angle, fromUnit, toUnit);
                double roundTrip = convert(converted, toUnit, fromUnit);

                assertThat(roundTrip)
                    .as("Round-trip from %s to %s and back for angle %f", fromUnit, toUnit, angle)
                    .isCloseTo(angle, within(EPSILON));
            }
        }
    }

    private static double convert(double angle, AngleUnit from, AngleUnit to) {
        return switch (to) {
            case RADIANS -> from.toRadians(angle);
            case INTUITIVE_RADIANS -> from.toIntuitiveRadians(angle);
            case DEGREES -> from.toDegrees(angle);
            case INTUITIVE_DEGREES -> from.toIntuitiveDegrees(angle);
        };
    }
}
