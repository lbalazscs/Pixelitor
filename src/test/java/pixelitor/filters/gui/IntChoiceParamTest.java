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
import pixelitor.filters.gui.IntChoiceParam.Item;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

@DisplayName("IntChoiceParam tests")
@TestMethodOrder(MethodOrderer.Random.class)
class IntChoiceParamTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("test String array constructor")
    void stringArrayConstructor() {
        String[] choices = {"First", "Second", "Third"};
        var param = new IntChoiceParam("Test", choices);

        // assert that the parameter has the correct number of choices
        assertThat(param.getSize()).isEqualTo(choices.length);

        // assert that each Item has the expected name and value
        for (int i = 0; i < choices.length; i++) {
            Item item = param.getElementAt(i);
            assertThat(item.name()).isEqualTo(choices[i]);
            assertThat(item.value()).isEqualTo(i);
        }

        // assert that the initial selected value is 0
        assertThat(param.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("randomize() with IGNORE_RANDOMIZE should not change value")
    void randomize_whenModeIsIgnore_shouldNotChangeValue() {
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
    @DisplayName("randomize() with ALLOW_RANDOMIZE should change value")
    void randomize_whenModeIsAllow_shouldChangeValue() {
        var param = new IntChoiceParam("Test", new Item[]{
            new Item("Name 1", 1),
            new Item("Name 2", 2),
            new Item("Name 3", 3),
            new Item("Name 4", 4),
            new Item("Name 5", 5)
        });

        int initialValue = param.getValue();
        boolean valueChanged = false;

        // with enough attempts, the value should change: the probability
        // of a false negative is (1/5)^100 = 1.26 * 10^-70
        for (int i = 0; i < 100; i++) {
            param.randomize();
            if (param.getValue() != initialValue) {
                valueChanged = true;
                break;
            }
        }

        assertThat(valueChanged).isTrue();
    }

    @Test
    void setSelectedItem() {
        Item v1 = new Item("Item 1", 1);
        Item v2 = new Item("Item 2", 2);
        var param = new IntChoiceParam("Test", new Item[]{v1, v2});

        assertThat(param)
            .isAtDefault()
            .valueIs(1)
            .selectedAsStringIs("Item 1");

        var adjListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(adjListener);

        param.setSelectedItem(v1, true);
        assertThat(param)
            .isAtDefault()
            .valueIs(1)
            .selectedAsStringIs("Item 1");
        // expect no triggering because the value didn't change
        verify(adjListener, never()).paramAdjusted();

        param.setSelectedItem(v2, true);
        assertThat(param)
            .isNotAtDefault()
            .valueIs(2)
            .selectedAsStringIs("Item 2");
        // expect one triggering
        verify(adjListener, times(1)).paramAdjusted();

        param.setSelectedItem(v1, false);
        assertThat(param)
            .isAtDefault()
            .valueIs(1)
            .selectedAsStringIs("Item 1");
        // expect no new triggering, because triggering was set to false
        verify(adjListener, times(1)).paramAdjusted();
    }
}
