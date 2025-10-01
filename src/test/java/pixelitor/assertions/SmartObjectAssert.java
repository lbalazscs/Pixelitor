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

package pixelitor.assertions;

import pixelitor.layers.ContentLayer;
import pixelitor.layers.SmartFilter;
import pixelitor.layers.SmartObject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertions for {@link ContentLayer} objects.
 */
public class SmartObjectAssert extends ContentLayerAssert<SmartObjectAssert, SmartObject> {
    public SmartObjectAssert(SmartObject actual) {
        super(actual, SmartObjectAssert.class);
    }

    public SmartObjectAssert hasNumSmartFilters(int expected) {
        isNotNull();
        assertThat(actual.getNumSmartFilters()).isEqualTo(expected);
        return myself;
    }

    public SmartObjectAssert smartFilterNamesAre(String... expected) {
        isNotNull();

        List<String> actualNames = actual.directChildrenStream()
            .map(SmartFilter::getName)
            .toList();
        assertThat(actualNames).containsExactly(expected);

        return myself;
    }

    public SmartObjectAssert smartFilterVisibilitiesAre(boolean... expected) {
        isNotNull();

        int numFilters = actual.getNumSmartFilters();
        assertThat(numFilters).isEqualTo(expected.length);
        for (int i = 0; i < numFilters; i++) {
            SmartFilter smartFilter = actual.getSmartFilter(i);
            assertThat(smartFilter.isVisible()).isEqualTo(expected[i]);
        }

        return myself;
    }

    public SmartObjectAssert activeLayerNameIs(String expected) {
        isNotNull();

        assertThat(actual.getComp().getActiveLayer().getName()).isEqualTo(expected);

        return myself;
    }

    public SmartObjectAssert invariantsAreOK() {
        isNotNull();

        assertThat(actual.checkInvariants()).isTrue();

        return myself;
    }
}
