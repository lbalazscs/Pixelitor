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
import pixelitor.filters.gui.IntChoiceParam.Value;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

@DisplayName("IntChoiceParam tests")
@TestMethodOrder(MethodOrderer.Random.class)
public class IntChoiceParamTest {
    @Test
    @DisplayName("is IGNORE_RANDOMIZE working")
    void isIgnoreRandomizeWorking() {
        var param = new IntChoiceParam("Test", new Value[]{
                new Value("Name 1", 1),
                new Value("Name 2", 2),
                new Value("Name 3", 3),
                new Value("Name 4", 4),
        }, IGNORE_RANDOMIZE);
        for (int i = 0; i < 10; i++) {
            param.randomize();
            assertThat(param).valueIs(1);
        }
    }

    @Test
    void setSelectedItem() {
        Value v1 = new Value("Item 1", 1);
        Value v2 = new Value("Item 2", 2);
        var param = new IntChoiceParam("Test", new Value[]{v1, v2});

        assertThat(param)
                .isSetToDefault()
                .valueIs(1)
                .selectedAsStringIs("Item 1");

        var adjListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(adjListener);

        param.setSelectedItem(v1, true);
        assertThat(param)
                .isSetToDefault()
                .valueIs(1)
                .selectedAsStringIs("Item 1");
        // expect no triggering because the value didn't change
        verify(adjListener, never()).paramAdjusted();

        param.setSelectedItem(v2, true);
        assertThat(param)
                .isNotSetToDefault()
                .valueIs(2)
                .selectedAsStringIs("Item 2");
        // expect one triggering
        verify(adjListener, times(1)).paramAdjusted();

        param.setSelectedItem(v1, false);
        assertThat(param)
                .isSetToDefault()
                .valueIs(1)
                .selectedAsStringIs("Item 1");
        // expect no new triggering, because triggering was set to false
        verify(adjListener, times(1)).paramAdjusted();
    }
}