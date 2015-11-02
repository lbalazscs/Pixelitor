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

import static org.assertj.core.api.Assertions.assertThat;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

public class BooleanParamTest {
    @Test
    public void isIgnoreRandomizeWorking() {
        BooleanParam param = new BooleanParam("Test", true, IGNORE_RANDOMIZE);
        for (int i = 0; i < 10; i++) {
            param.randomize();
            assertThat(param.isChecked()).isTrue();
        }
    }
}