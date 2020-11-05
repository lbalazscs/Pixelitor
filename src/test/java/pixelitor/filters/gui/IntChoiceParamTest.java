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

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.filters.gui.IntChoiceParam.Item;

import static org.mockito.Mockito.*;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

@DisplayName("IntChoiceParam tests")
@TestMethodOrder(MethodOrderer.Random.class)
class IntChoiceParamTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("is IGNORE_RANDOMIZE working")
    void isIgnoreRandomizeWorking() {
        var param = new IntChoiceParam("Test", new Item[]{
            new Item("Name 1", 1),
            new Item("Name 2", 2),
            new Item("Name 3", 3),
            new Item("Name 4", 4),
        }, IGNORE_RANDOMIZE);
        for (int i = 0; i < 10; i++) {
            param.randomize();
            assertThat(param).valueIs(1);
        }
    }

    @Test
    void setSelectedItem() {
        Item v1 = new Item("Item 1", 1);
        Item v2 = new Item("Item 2", 2);
        var param = new IntChoiceParam("Test", new Item[]{v1, v2});

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