/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import static org.assertj.core.api.Assertions.assertThat;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

public class RangeParamTest {
    @Test
    public void isIgnoreRandomizeWorking() {
        RangeParam param = new RangeParam("Test", 0, 100, 1000,
                AddDefaultButton.YES, NONE, IGNORE_RANDOMIZE);
        for (int i = 0; i < 5; i++) {
            param.randomize();
            assertThat(param.getValue()).isEqualTo(100);
        }
    }

    @Test(expected = AssertionError.class)
    public void invalidArgsMinIsMax() {
        new RangeParam("name", 10, 10, 10);
    }

    @Test(expected = AssertionError.class)
    public void invalidArgsMinBiggerThanMax() {
        new RangeParam("name", 11, 10, 10);
    }

    @Test(expected = AssertionError.class)
    public void invalidArgsDefaultSmallerThanMin() {
        new RangeParam("name", 5, 2, 10);
    }

    @Test(expected = AssertionError.class)
    public void invalidArgsDefaultBiggerThanMax() {
        new RangeParam("name", 5, 15, 10);
    }
}
