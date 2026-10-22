/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.channels;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;
import pixelitor.filters.curves.ToneCurvesFilter;
import pixelitor.filters.levels.Levels;
import pixelitor.filters.lookup.LookupOps;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChannelAdjustmentFilter tests")
class ChannelAdjustmentFilterTest {

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("supportsGray() of both filters equals LookupOps.supportsGray()")
    void supportsGrayEqualsLookupOps() {
        assertThat(new Levels().supportsGray()).isEqualTo(LookupOps.supportsGray());
        assertThat(new ToneCurvesFilter().supportsGray()).isEqualTo(LookupOps.supportsGray());
    }
}
