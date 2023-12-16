/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.history.History;
import pixelitor.utils.MockFilter;

import java.awt.Dimension;

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
        parent.addLayerNoUI(smartObject);

        MockFilter firstFilter = TestHelper.createMockFilter("Filter 1");
        MockFilter middleFilter = TestHelper.createMockFilter("Filter 2");
        MockFilter lastFilter = TestHelper.createMockFilter("Filter 3");

        History.clear();

        first = new SmartFilter(firstFilter, content, smartObject);
        middle = new SmartFilter(middleFilter, content, smartObject);
        last = new SmartFilter(lastFilter, content, smartObject);

        smartObject.addSmartFilter(first, true, true);
        smartObject.addSmartFilter(middle, true, true);
        smartObject.addSmartFilter(last, true, true);

        smartObject.invalidateImageCache();
        forceRecalculatingImage();

        History.assertNumEditsIs(3);
        History.clear();

        checkCaches(true, true, true);
        assertThat(smartObject)
            .hasNumSmartFilters(3)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();

        smartObject.createUI();
        soIconUpdates = new IconUpdateChecker(smartObject, null);

        firstFilter.resetNumTransformCalls();
        middleFilter.resetNumTransformCalls();
        lastFilter.resetNumTransformCalls();
    }

    @Test
    void resize() {
//        Composition resized = new Resize(10, 5).process(smartObject.getComp()).join();

        smartObject.resize(new Dimension(10, 5)).join();
        forceRecalculatingImage();
        checkNumFilterRuns(1, 1, 1);

        smartObject.resize(new Dimension(4, 4)).join();
    }

    @Test
    void addSmartFilter() {
        Filter newFilter = TestHelper.createMockFilter("Filter 4");
        SmartFilter newSF = new SmartFilter(newFilter, smartObject.getContent(), smartObject);

        smartObject.addSmartFilter(newSF, true, true);

        assertThat(smartObject)
            .hasNumSmartFilters(4)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3", "Filter 4")
            .smartFilterVisibilitiesAre(true, true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 4")
            .invariantsAreOK();

        History.assertNumEditsIs(1);
        smartObject.getVisibleImage();
        soIconUpdates.check(1, 0);

        undo("Add Smart Filter 4");
        assertThat(smartObject)
            .hasNumSmartFilters(3)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        soIconUpdates.check(2, 0);

        redo("Add Smart Filter 4");
        assertThat(smartObject)
            .hasNumSmartFilters(4)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3", "Filter 4")
            .smartFilterVisibilitiesAre(true, true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 4")
            .invariantsAreOK();
        soIconUpdates.check(3, 0);
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
        first.setVisible(false, true, true);
        forceRecalculatingImage();

        checkNumFilterRuns(0, 1, 1);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(false, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Hide Smart Filter");
        checkNumFilterRuns(0, 2, 2);
        soIconUpdates.check(2, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();

        redo("Hide Smart Filter");
        checkNumFilterRuns(0, 3, 3);
        soIconUpdates.check(3, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(false, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void hideMiddle() {
        middle.setVisible(false, true, true);
        forceRecalculatingImage();

        checkNumFilterRuns(0, 0, 1);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, false, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Hide Smart Filter");
        checkNumFilterRuns(0, 0, 2);
        soIconUpdates.check(2, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();

        redo("Hide Smart Filter");
        checkNumFilterRuns(0, 0, 3);
        soIconUpdates.check(3, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, false, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void hideLast() {
        last.setVisible(false, true, true);
        forceRecalculatingImage();

        checkNumFilterRuns(0, 0, 0);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, true, false)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Hide Smart Filter");
        checkNumFilterRuns(0, 0, 0);
        soIconUpdates.check(2, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();

        redo("Hide Smart Filter");
        checkNumFilterRuns(0, 0, 0);
        soIconUpdates.check(3, 0);
        assertThat(smartObject)
            .smartFilterVisibilitiesAre(true, true, false)
            .invariantsAreOK();
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void editFirst() {
        // doesn't simply call edit() on the smart filter,
        // because that would try to use a dialog.
        first.invalidateAll();
        forceRecalculatingImage();

        checkNumFilterRuns(1, 1, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void editMiddle() {
        middle.invalidateAll();
        forceRecalculatingImage();

        checkNumFilterRuns(0, 1, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void editLast() {
        last.invalidateAll();
        forceRecalculatingImage();

        checkNumFilterRuns(0, 0, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void deleteFirst() {
        smartObject.deleteSmartFilter(first, true, true);
        forceRecalculatingImage();

        checkNumFilterRuns(0, 1, 1);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Delete Filter 1");
        checkNumFilterRuns(0, 2, 2);
        soIconUpdates.check(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        redo("Delete Filter 1");
        checkNumFilterRuns(0, 3, 3);
        soIconUpdates.check(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
    }

    @Test
    void deleteMiddle() {
        smartObject.deleteSmartFilter(middle, true, true);
        forceRecalculatingImage();

        checkNumFilterRuns(0, 0, 1);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Delete Filter 2");
        checkNumFilterRuns(0, 0, 2);
        soIconUpdates.check(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();

        redo("Delete Filter 2");
        checkNumFilterRuns(0, 0, 3);
        soIconUpdates.check(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3")
            .smartFilterVisibilitiesAre(true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
    }

    @Test
    void deleteLast() {
        smartObject.deleteSmartFilter(last, true, true);
        forceRecalculatingImage();

        checkNumFilterRuns(0, 0, 0);
        soIconUpdates.check(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Delete Filter 3");
        checkNumFilterRuns(0, 0, 0);
        soIconUpdates.check(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Delete Filter 3");
        checkNumFilterRuns(0, 0, 0);
        soIconUpdates.check(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2")
            .smartFilterVisibilitiesAre(true, true)
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
    }

    @Test
    void moveUp() {
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .invariantsAreOK();
        SmartFilter filter1 = smartObject.getSmartFilter(0);

        smartObject.moveUp(filter1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 1", "Filter 3")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        smartObject.moveUp(filter1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        smartObject.moveUp(filter1); // nothing should happen
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        undo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 1", "Filter 3")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        undo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        redo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 1", "Filter 3")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        redo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();
    }

    @Test
    void moveDown() {
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .invariantsAreOK();
        SmartFilter filter3 = smartObject.getSmartFilter(2);

        smartObject.moveDown(filter3);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3", "Filter 2")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        smartObject.moveDown(filter3);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        smartObject.moveDown(filter3); // nothing should happen
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        undo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3", "Filter 2")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        undo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3", "Filter 2")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .invariantsAreOK();
        assertThat(smartObject.getComp())
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
    }

    private void checkNumFilterRuns(int expectedFirst, int expectedMiddle, int expectedLast) {
        MockFilter firstFilter = (MockFilter) first.getFilter();
        MockFilter middleFilter = (MockFilter) middle.getFilter();
        MockFilter lastFilter = (MockFilter) last.getFilter();

        // add one to the expected values because each
        // filter was called once during the test setup
        assertThat(firstFilter.getNumTransformCalls()).isEqualTo(expectedFirst);
        assertThat(middleFilter.getNumTransformCalls()).isEqualTo(expectedMiddle);
        assertThat(lastFilter.getNumTransformCalls()).isEqualTo(expectedLast);
    }

    private void checkCaches(boolean expectedFirst, boolean expectedMiddle, boolean expectedLast) {
        assertThat(first).hasCachedImage(expectedFirst);
        assertThat(middle).hasCachedImage(expectedMiddle);
        assertThat(last).hasCachedImage(expectedLast);
    }

    private void undo(String editName) {
        History.undo(editName);
        forceRecalculatingImage();
    }

    private void redo(String editName) {
        History.redo(editName);
        forceRecalculatingImage();
    }

    private void forceRecalculatingImage() {
        smartObject.getVisibleImage();
    }
}
