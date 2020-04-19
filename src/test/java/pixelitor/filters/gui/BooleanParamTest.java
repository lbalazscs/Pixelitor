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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

@DisplayName("BooleanParam tests")
@TestMethodOrder(MethodOrderer.Random.class)
public class BooleanParamTest {
    @Test
    @DisplayName("is IGNORE_RANDOMIZE working")
    void isIgnoreRandomizeWorking() {
        var param = new BooleanParam("Test", true, IGNORE_RANDOMIZE);
        for (int i = 0; i < 10; i++) {
            param.randomize();
            assertThat(param.isChecked()).isTrue();
        }
    }

    @Test
    void setValue() {
        var param = new BooleanParam("Test", true);
        assertThat(param.isChecked()).isTrue();
        assertThat(param).isSetToDefault();

        var adjListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(adjListener);

        param.setValue(true, false, true);
        assertThat(param).isSetToDefault();
        // expect no triggering because the value didn't change
        verify(adjListener, never()).paramAdjusted();

        param.setValue(false, false, true);
        assertThat(param.isSetToDefault()).isFalse();
        // expect one triggering
        verify(adjListener, times(1)).paramAdjusted();

        param.setValue(true, false, false);
        assertThat(param).isSetToDefault();
        // expect no new triggering, because triggering was set to false
        verify(adjListener, times(1)).paramAdjusted();
    }
}