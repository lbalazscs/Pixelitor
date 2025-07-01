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

package pixelitor.filters.gui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.TestHelper;

import java.util.stream.Stream;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static org.assertj.core.api.Assertions.assertThat;
import static pixelitor.filters.gui.TransparencyMode.ALPHA_ENABLED;
import static pixelitor.utils.AngleUnit.INTUITIVE_DEGREES;

@DisplayName("ParamState tests")
class ParamStateTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    static Stream<Arguments> instancesToTest() {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        var angleParamStart = new AngleParam("AngleParam", 0);
        var angleParamEnd = new AngleParam("AngleParam", 180, INTUITIVE_DEGREES);

        var rangeParamStart = new RangeParam("RangeParam", 0, 10, 100);
        var rangeParamEnd = new RangeParam("RangeParam", 0, 100, 100);

        var groupedRangeParamStart = new GroupedRangeParam("GroupedRangeParam", 0, 0, 200);
        var groupedRangeParamEnd = new GroupedRangeParam("GroupedRangeParam", 0, 100, 200);

        var gradientParamStart = new GradientParam("GradientParam", BLACK, GREEN);
        var gradientParamEnd = new GradientParam("GradientParam", BLUE, RED);

        var imagePositionParamStart = new ImagePositionParam("ImagePositionParam", 0.1f, 0.0f);
        var imagePositionParamEnd = new ImagePositionParam("ImagePositionParam", 0.9f, 1.0f);

        var colorParamStart = new ColorParam("ColorParam", RED, ALPHA_ENABLED);
        var colorParamEnd = new ColorParam("ColorParam", BLUE, ALPHA_ENABLED);

        return Stream.of(
            Arguments.of(angleParamStart.copyState(), angleParamEnd.copyState()),
            Arguments.of(rangeParamStart.copyState(), rangeParamEnd.copyState()),
            Arguments.of(groupedRangeParamStart.copyState(), groupedRangeParamEnd.copyState()),
            Arguments.of(gradientParamStart.copyState(), gradientParamEnd.copyState()),
            Arguments.of(imagePositionParamStart.copyState(), imagePositionParamEnd.copyState()),
            Arguments.of(colorParamStart.copyState(), colorParamEnd.copyState())
        );
    }

    @DisplayName("interpolation test")
    @ParameterizedTest(name = "#{index}: interpolate between {0} and {1}")
    @MethodSource("instancesToTest")
    <T extends ParamState<T>> void interpolate(T start, T end) {
        ParamState<T> interpolated = start.interpolate(end, 0.0);
        assertThat(interpolated).isNotNull();

        interpolated = start.interpolate(end, 0.5);
        assertThat(interpolated).isNotNull();

        interpolated = start.interpolate(end, 1.0);
        assertThat(interpolated).isNotNull();
    }
}