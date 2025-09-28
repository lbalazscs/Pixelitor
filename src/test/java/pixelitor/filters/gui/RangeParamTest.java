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

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.assertions.PixelitorAssertions.assertThatThrownBy;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;

@DisplayName("RangeParam tests")
@TestMethodOrder(MethodOrderer.Random.class)
class RangeParamTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("randomize() with IGNORE_RANDOMIZE should not change value")
    void randomize_whenModeIsIgnore_shouldNotChangeValue() {
        var param = new RangeParam("Test", 0, 100, 1000,
            true, NONE, IGNORE_RANDOMIZE);
        for (int i = 0; i < 5; i++) {
            param.randomize();
            assertThat(param.getValue()).isEqualTo(100);
        }
    }

    @Test
    void setValue() {
        var param = new RangeParam("Test", 0, 50, 100);
        assertThat(param.getValue()).isEqualTo(50);
        assertThat(param).isAtDefault();

        var adjListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(adjListener);

        param.setValue(50, true);
        assertThat(param).isAtDefault();
        // expect no triggering because the value didn't change
        verify(adjListener, never()).paramAdjusted();

        param.setValue(60, true);
        assertThat(param).isNotAtDefault();
        // expect one triggering
        verify(adjListener, times(1)).paramAdjusted();

        param.setValue(50, false);
        assertThat(param).isAtDefault();
        // expect no new triggering, because triggering was set to false
        verify(adjListener, times(1)).paramAdjusted();
    }

    @Test
    @DisplayName("invalid arguments: min == max")
    void invalidArgsMinIsMax() {
        assertThatThrownBy(() -> new RangeParam("name", 10, 10, 10))
            .isInstanceOf(AssertionError.class)
            .hasMessage("name: maxValue (10) <= minValue (10)");
    }

    @Test
    @DisplayName("invalid arguments: min > max")
    void invalidArgsMinBiggerThanMax() {
        assertThatThrownBy(() -> new RangeParam("name", 11, 10, 10))
            .isInstanceOf(AssertionError.class)
            .hasMessage("name: maxValue (10) <= minValue (11)");
    }

    @Test
    @DisplayName("invalid arguments: default < min")
    void invalidArgsDefaultSmallerThanMin() {
        assertThatThrownBy(() -> new RangeParam("name", 5, 2, 10))
            .isInstanceOf(AssertionError.class)
            .hasMessage("name: value (2.00) < minValue (5)");
    }

    @Test
    @DisplayName("invalid arguments: default > max")
    void invalidArgsDefaultBiggerThanMax() {
        assertThatThrownBy(() -> new RangeParam("name", 5, 15, 10))
            .isInstanceOf(AssertionError.class)
            .hasMessage("name: value (15.00) > maxValue (10)");
    }
}
