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
 * Tests for {@link ImageUtils}
 */
public class ImageUtilsTest {
    @Test
    public void testCalcSubsamplingCols() {
        int cols = ImageUtils.calcSubsamplingCols(5000, 2000, 100, 100);
        assertThat(cols).isEqualTo(50);

        cols = ImageUtils.calcSubsamplingCols(2000, 5000, 100, 100);
        assertThat(cols).isEqualTo(50);

        cols = ImageUtils.calcSubsamplingCols(250, 250, 100, 100);
        assertThat(cols).isEqualTo(3);
    }
}