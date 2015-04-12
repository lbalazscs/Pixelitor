/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import static org.junit.Assert.assertEquals;
import static pixelitor.utils.SliderSpinner.TextPosition.NONE;

public class RangeParamTest {
    @Test
    public void isIgnoreRandomizeWorking() {
        RangeParam param = new RangeParam("Test", 0, 1000, 100, true, NONE, true);
        for (int i = 0; i < 5; i++) {
            param.randomize();
            assertEquals(100, param.getValue());
        }
    }

    @Test(expected = AssertionError.class)
    public void testInvalidConstructorMinIsMax() {
        new RangeParam("name", 10, 10, 10);
    }

    @Test(expected = AssertionError.class)
    public void testInvalidConstructorMinBiggerThanMax() {
        new RangeParam("name", 11, 10, 10);
    }

    @Test(expected = AssertionError.class)
    public void testInvalidConstructorDefaultSmallerThanMin() {
        new RangeParam("name", 5, 10, 2);
    }

    @Test(expected = AssertionError.class)
    public void testInvalidConstructorDefaultBiggerThanMax() {
        new RangeParam("name", 5, 10, 15);
    }
}
