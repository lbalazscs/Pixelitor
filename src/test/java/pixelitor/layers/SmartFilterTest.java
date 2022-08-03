/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.filters.Filter;
import pixelitor.utils.MockFilter;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Smart filter tests")
@TestMethodOrder(MethodOrderer.Random.class)
class SmartFilterTest {
    private SmartObject smartObject;

    private SmartFilter first;
    private SmartFilter middle;
    private SmartFilter last;

    private IconUpdateChecker soIconUpdates;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        Composition parent = TestHelper.createComp(1, false);
        Composition content = TestHelper.createComp(1, false);

        smartObject = new SmartObject(parent, content);
        parent.addLayerInInitMode(smartObject);

        Filter firstFilter = TestHelper.createMockFilter("Filter 1");
        Filter middleFilter = TestHelper.createMockFilter("Filter 2");
        Filter lastFilter = TestHelper.createMockFilter("Filter 3");

        smartObject.addSmartFilter(firstFilter);
        smartObject.addSmartFilter(middleFilter);
        smartObject.addSmartFilter(lastFilter);

        assertThat(smartObject).hasNumSmartFilters(3);

        first = smartObject.getSmartFilter(0);
        middle = smartObject.getSmartFilter(1);
        last = smartObject.getSmartFilter(2);

        smartObject.recalculateImage(false);
        checkCaches(true, true, true);
        assertThat(smartObject)
            .hasNumSmartFilters(3)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .isConsistent();

        smartObject.createUI();
        soIconUpdates = new IconUpdateChecker(smartObject, null, 0, 0);
    }

    @Test
    void invalidateChainStartingAtFirst() {
        first.invalidateChain();

        checkCaches(false, false, false);
        assertThat(smartObject).hasNumSmartFilters(3);
    }

    @Test
    void invalidateChainStartingAtMiddle() {
        middle.invalidateChain();

        checkCaches(true, false, false);
        assertThat(smartObject).hasNumSmartFilters(3);
    }

    @Test
    void invalidateChainStartingAtLast() {
        last.invalidateChain();

        checkCaches(true, true, false);
        assertThat(smartObject).hasNumSmartFilters(3);
    }

    @Test
    void hideFirst() {
        first.setVisible(false, false, true);

        checkNumFilterRuns(0, 1, 1);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(false, true, true)
            .isConsistent();
    }

    @Test
    void hideMiddle() {
        middle.setVisible(false, false, true);

        checkNumFilterRuns(0, 0, 1);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, false, true)
            .isConsistent();
    }

    @Test
    void hideLast() {
        last.setVisible(false, false, true);

        checkNumFilterRuns(0, 0, 0);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, true, false)
            .isConsistent();
    }

    @Test
    void editFirst() {
        // doesn't simply call edit() on the smart filter,
        // because that would try to use a dialog.
        first.filterSettingsChanged();

        checkNumFilterRuns(1, 1, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
    }

    @Test
    void editMiddle() {
        middle.filterSettingsChanged();

        checkNumFilterRuns(0, 1, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
    }

    @Test
    void editLast() {
        last.filterSettingsChanged();

        checkNumFilterRuns(0, 0, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
    }

    @Test
    void deleteFirst() {
        smartObject.deleteSmartFilter(0);

        checkNumFilterRuns(0, 1, 1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3")
            .isConsistent();
    }

    @Test
    void deleteMiddle() {
        smartObject.deleteSmartFilter(1);

        checkNumFilterRuns(0, 0, 1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3")
            .isConsistent();
    }

    @Test
    void deleteLast() {
        smartObject.deleteSmartFilter(2);

        checkNumFilterRuns(0, 0, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2")
            .isConsistent();
    }

    @Test
    void moveUp() {
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .isConsistent();

        SmartFilter filter1 = smartObject.getSmartFilter(0);
        smartObject.moveUp(filter1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 1", "Filter 3")
            .isConsistent();

        smartObject.moveUp(filter1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .isConsistent();

        smartObject.moveUp(filter1); // nothing should happen
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .isConsistent();
    }

    @Test
    void moveDown() {
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .isConsistent();

        SmartFilter filter3 = smartObject.getSmartFilter(2);
        smartObject.moveDown(filter3);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3", "Filter 2")
            .isConsistent();

        smartObject.moveDown(filter3);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .isConsistent();

        smartObject.moveDown(filter3); // nothing should happen
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .isConsistent();
    }

    private void checkNumFilterRuns(int expectedFirst, int expectedMiddle, int expectedLast) {
        MockFilter firstFilter = (MockFilter) first.getFilter();
        MockFilter middleFilter = (MockFilter) middle.getFilter();
        MockFilter lastFilter = (MockFilter) last.getFilter();

        // add one to the expected values because each
        // filter was called once during the test setup
        assertThat(firstFilter.getNumTransformCalls()).isEqualTo(expectedFirst + 1);
        assertThat(middleFilter.getNumTransformCalls()).isEqualTo(expectedMiddle + 1);
        assertThat(lastFilter.getNumTransformCalls()).isEqualTo(expectedLast + 1);
    }

    private void checkCaches(boolean expectedFirst, boolean expectedMiddle, boolean expectedLast) {
        assertThat(first).hasCachedImage(expectedFirst);
        assertThat(middle).hasCachedImage(expectedMiddle);
        assertThat(last).hasCachedImage(expectedLast);
    }
}
