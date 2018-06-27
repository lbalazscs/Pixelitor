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

package pixelitor.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests utility helpers
 */
public class UtilsTest {
    @Test
    public void test_parseJavaVersion() {
        int v8 = Utils.parseJavaVersion("1.8.0_161");
        int v9 = Utils.parseJavaVersion("9.0.1");
        int v10 = Utils.parseJavaVersion("10.0.1");

        assertThat(v8).isEqualTo(8);
        assertThat(v9).isEqualTo(9);
        assertThat(v10).isEqualTo(10);
    }
}