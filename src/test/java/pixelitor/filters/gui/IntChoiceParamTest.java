/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import org.junit.Test;
import pixelitor.filters.gui.IntChoiceParam.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

public class IntChoiceParamTest {
    @Test
    public void isIgnoreRandomizeWorking() {
        IntChoiceParam param = new IntChoiceParam("Test", new Value[]{
                new Value("Name 1", 1),
                new Value("Name 2", 2),
                new Value("Name 3", 3),
                new Value("Name 4", 4),
        }, IGNORE_RANDOMIZE);
        for (int i = 0; i < 10; i++) {
            param.randomize();
            assertThat(param.getValue()).isEqualTo(1);
        }
    }

    @Test
    public void test_setSelectedItem() {
        Value v1 = new Value("Item 1", 1);
        Value v2 = new Value("Item 2", 2);
        IntChoiceParam param = new IntChoiceParam("Test", new Value[]{
                v1,
                v2,
        });

        assertThat(param.getSelectedItem().toString()).isEqualTo("Item 1");
        assertThat(param.getValue()).isEqualTo(1);
        assertThat(param.isSetToDefault()).isTrue();

        ParamAdjustmentListener al = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(al);

        param.setSelectedItem(v1, true);
        assertThat(param.isSetToDefault()).isTrue();
        // expect no triggering because the value didn't change
        verify(al, never()).paramAdjusted();

        param.setSelectedItem(v2, true);
        assertThat(param.isSetToDefault()).isFalse();
        // expect one triggering
        verify(al, times(1)).paramAdjusted();

        param.setSelectedItem(v1, false);
        assertThat(param.isSetToDefault()).isTrue();
        // expect no new triggering, because triggering was set to false
        verify(al, times(1)).paramAdjusted();
    }
}