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

package pixelitor.filters.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

public class RangeParamTest {
    @Test
    void isIgnoreRandomizeWorking() {
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
        assertThat(param).isSetToDefault();

        var adjListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(adjListener);

        param.setValue(50, true);
        assertThat(param).isSetToDefault();
        // expect no triggering because the value didn't change
        verify(adjListener, never()).paramAdjusted();

        param.setValue(60, true);
        assertThat(param).isNotSetToDefault();
        // expect one triggering
        verify(adjListener, times(1)).paramAdjusted();

        param.setValue(50, false);
        assertThat(param).isSetToDefault();
        // expect no new triggering, because triggering was set to false
        verify(adjListener, times(1)).paramAdjusted();
    }

    @Test
    void invalidArgsMinIsMax() {
        assertThrows(AssertionError.class, () ->
                new RangeParam("name", 10, 10, 10));
    }

    @Test
    void invalidArgsMinBiggerThanMax() {
        assertThrows(AssertionError.class, () ->
                new RangeParam("name", 11, 10, 10));
    }

    @Test
    void invalidArgsDefaultSmallerThanMin() {
        assertThrows(AssertionError.class, () ->
                new RangeParam("name", 5, 2, 10));
    }

    @Test
    void invalidArgsDefaultBiggerThanMax() {
        assertThrows(AssertionError.class, () ->
                new RangeParam("name", 5, 15, 10));
    }
}
