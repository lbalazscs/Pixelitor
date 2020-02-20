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

package pixelitor.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pixelitor.io.TrackedIO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TrackedIO}
 */
public class TrackedIOTest {
    @ParameterizedTest(name = "{0}x{1} image => {2} cols")
    @CsvSource({"5000, 2000, 50", "2000, 5000, 50", "250, 250, 3"})
    void calcSubsamplingCols(int imgWidth, int imgHeight, int expected) {
        int cols = TrackedIO.calcSubsamplingCols(imgWidth, imgHeight, 100, 100);
        assertThat(cols).isEqualTo(expected);
    }
}