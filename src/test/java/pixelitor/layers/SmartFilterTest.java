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

    private IconUpdateChecker soIconChecker;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        Composition parent = TestHelper.createComp("SmartFilterTest parent", 1, false);
        Composition content = TestHelper.createComp("SmartFilterTest content", 1, false);

        smartObject = new SmartObject(parent, content);
        parent.addLayerWithoutUI(smartObject);

        MockFilter firstFilter = new MockFilter("Filter 1");
        MockFilter middleFilter = new MockFilter("Filter 2");
        MockFilter lastFilter = new MockFilter("Filter 3");

        first = new SmartFilter(firstFilter, content, smartObject);
        middle = new SmartFilter(middleFilter, content, smartObject);
        last = new SmartFilter(lastFilter, content, smartObject);

        smartObject.addSmartFilter(first, true, true);
        smartObject.addSmartFilter(middle, true, true);
        smartObject.addSmartFilter(last, true, true);

        smartObject.invalidateImageCache();
        forceRecalculatingImage();

        History.clear();

        checkCaches(true, true, true);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        smartObject.createUI();
        soIconChecker = new IconUpdateChecker(smartObject);

        firstFilter.resetNumTransformCalls();
        middleFilter.resetNumTransformCalls();
        lastFilter.resetNumTransformCalls();
    }

    @Test
    void resize() {
        smartObject.resize(new Dimension(10, 5)).join();
        forceRecalculatingImage();
        // resizing the smart object invalidates the base source, so all filters are re-run
        checkNumFilterRuns(1, 1, 1);

        smartObject.resize(new Dimension(4, 4)).join();
    }

    @Test
    void addSmartFilter() {
        Filter newFilter = new MockFilter("Filter 4");
        SmartFilter newSF = new SmartFilter(newFilter, smartObject.getContent(), smartObject);

        smartObject.addSmartFilter(newSF, true, true);

        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3", "Filter 4")
            .smartFilterVisibilitiesAre(true, true, true, true)
            .activeLayerNameIs("Filter 4")
            .invariantsAreOK();

        History.assertNumEditsIs(1);
        smartObject.getVisibleImage();
        soIconChecker.verifyUpdateCounts(1, 0);

        undo("Add Smart Filter 4");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        soIconChecker.verifyUpdateCounts(2, 0);

        redo("Add Smart Filter 4");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3", "Filter 4")
            .smartFilterVisibilitiesAre(true, true, true, true)
            .activeLayerNameIs("Filter 4")
            .invariantsAreOK();
        soIconChecker.verifyUpdateCounts(3, 0);
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

        // hiding the first filter invalidates the chain; the middle and last filters are re-run
        checkNumFilterRuns(0, 1, 1);
        soIconChecker.verifyUpdateCounts(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(false, true, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Hide Smart Filter");
        // unhiding the first filter also invalidates the chain
        checkNumFilterRuns(0, 2, 2);
        soIconChecker.verifyUpdateCounts(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Hide Smart Filter");
        // hiding it again re-runs the downstream filters
        checkNumFilterRuns(0, 3, 3);
        soIconChecker.verifyUpdateCounts(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(false, true, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
    }

    @Test
    void hideMiddle() {
        middle.setVisible(false, true, true);
        forceRecalculatingImage();

        // hiding the middle filter means only the last filter needs to be re-run
        checkNumFilterRuns(0, 0, 1);
        soIconChecker.verifyUpdateCounts(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, false, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Hide Smart Filter");
        // unhiding the middle filter re-runs the last filter
        checkNumFilterRuns(0, 0, 2);
        soIconChecker.verifyUpdateCounts(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Hide Smart Filter");
        // hiding it again re-runs the last filter
        checkNumFilterRuns(0, 0, 3);
        soIconChecker.verifyUpdateCounts(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, false, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
    }

    @Test
    void hideLast() {
        last.setVisible(false, true, true);
        forceRecalculatingImage();

        // hiding the last filter doesn't invalidate any previous caches, so no re-runs
        checkNumFilterRuns(0, 0, 0);
        soIconChecker.verifyUpdateCounts(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, false)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Hide Smart Filter");
        // unhiding the last filter doesn't require re-runs because its output isn't used
        checkNumFilterRuns(0, 0, 0);
        soIconChecker.verifyUpdateCounts(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, true)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Hide Smart Filter");
        // hiding it again doesn't require re-runs
        checkNumFilterRuns(0, 0, 0);
        soIconChecker.verifyUpdateCounts(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .smartFilterVisibilitiesAre(true, true, false)
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
    }

    @Test
    void editFirst() {
        // simulates the invalidation that occurs during an edit
        first.invalidateAll();
        forceRecalculatingImage();

        // editing the first filter invalidates the entire chain
        checkNumFilterRuns(1, 1, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void editMiddle() {
        // simulates the invalidation that occurs during an edit
        middle.invalidateAll();
        forceRecalculatingImage();

        // editing the middle filter invalidates it and the last filter
        checkNumFilterRuns(0, 1, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void editLast() {
        // simulates the invalidation that occurs during an edit
        last.invalidateAll();
        forceRecalculatingImage();

        // editing the last filter invalidates only itself
        checkNumFilterRuns(0, 0, 1);
        assertThat(smartObject).hasNumSmartFilters(3);
        assertThat(smartObject.getComp()).invariantsAreOK();
    }

    @Test
    void deleteFirst() {
        smartObject.deleteSmartFilter(first, true, true);
        forceRecalculatingImage();

        // deleting the first filter invalidates the chain; the middle and last filters are re-run
        checkNumFilterRuns(0, 1, 1);
        soIconChecker.verifyUpdateCounts(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Delete Filter 1");
        // undoing the deletion re-runs the chain
        checkNumFilterRuns(0, 2, 2);
        soIconChecker.verifyUpdateCounts(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        redo("Delete Filter 1");
        // redoing the deletion re-runs the chain again
        checkNumFilterRuns(0, 3, 3);
        soIconChecker.verifyUpdateCounts(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
    }

    @Test
    void deleteMiddle() {
        smartObject.deleteSmartFilter(middle, true, true);
        forceRecalculatingImage();

        // deleting the middle filter means only the last filter needs to be re-run
        checkNumFilterRuns(0, 0, 1);
        soIconChecker.verifyUpdateCounts(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Delete Filter 2");
        // undoing the deletion re-runs the last filter
        checkNumFilterRuns(0, 0, 2);
        soIconChecker.verifyUpdateCounts(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();

        redo("Delete Filter 2");
        // redoing the deletion re-runs the last filter again
        checkNumFilterRuns(0, 0, 3);
        soIconChecker.verifyUpdateCounts(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
    }

    @Test
    void deleteLast() {
        smartObject.deleteSmartFilter(last, true, true);
        forceRecalculatingImage();

        // deleting the last filter doesn't require any re-runs
        checkNumFilterRuns(0, 0, 0);
        soIconChecker.verifyUpdateCounts(1, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2")
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Delete Filter 3");
        // undoing the deletion doesn't require re-runs
        checkNumFilterRuns(0, 0, 0);
        soIconChecker.verifyUpdateCounts(2, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Delete Filter 3");
        // redoing the deletion doesn't require re-runs
        checkNumFilterRuns(0, 0, 0);
        soIconChecker.verifyUpdateCounts(3, 0);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2")
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();
    }

    @Test
    void replaceMiddleFilter() {
        smartObject.getComp().setActiveLayer(middle);

        MockFilter newFilter = new MockFilter("Filter 4");
        middle.replaceFilterWith(newFilter);

        // causes the new middle filter (Filter 4) and
        // the last filter (Filter 3) to run once
        forceRecalculatingImage();
        checkNumFilterRuns(0, 1, 1);
        soIconChecker.verifyUpdateCounts(1, 0);

        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 4", "Filter 3")
            .activeLayerNameIs("Filter 4")
            .invariantsAreOK();
        History.assertNumEditsIs(1);

        undo("Replace Filter");

        // this is correct, because the original middle filter (Filter 2) runs for
        // the first time, and the last filter (Filter 3) runs for the second time
        checkNumFilterRuns(0, 1, 2);

        soIconChecker.verifyUpdateCounts(2, 0);

        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 2")
            .invariantsAreOK();

        redo("Replace Filter");
        checkNumFilterRuns(0, 2, 3);

        soIconChecker.verifyUpdateCounts(3, 0);

        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 4", "Filter 3")
            .activeLayerNameIs("Filter 4")
            .invariantsAreOK();
    }

    @Test
    void moveUp() {
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        SmartFilter filter1 = smartObject.getSmartFilter(0);

        smartObject.moveUp(filter1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 1", "Filter 3")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        smartObject.moveUp(filter1);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        smartObject.moveUp(filter1);
        // can't be moved further, nothing happens
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        undo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 1", "Filter 3")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        undo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        redo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 1", "Filter 3")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();

        redo("Move Filter 1 Up");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 2", "Filter 3", "Filter 1")
            .activeLayerNameIs("Filter 1")
            .invariantsAreOK();
    }

    @Test
    void moveDown() {
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        SmartFilter filter3 = smartObject.getSmartFilter(2);

        smartObject.moveDown(filter3);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3", "Filter 2")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        smartObject.moveDown(filter3);
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        smartObject.moveDown(filter3);
        // can't be moved further, nothing happens
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
        History.assertNumEditsIs(2);

        undo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3", "Filter 2")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        undo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 2", "Filter 3")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 1", "Filter 3", "Filter 2")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();

        redo("Move Filter 3 Down");
        assertThat(smartObject)
            .smartFilterNamesAre("Filter 3", "Filter 1", "Filter 2")
            .activeLayerNameIs("Filter 3")
            .invariantsAreOK();
    }

    private void checkNumFilterRuns(int expectedFirst, int expectedMiddle, int expectedLast) {
        MockFilter firstFilter = (MockFilter) first.getFilter();
        MockFilter middleFilter = (MockFilter) middle.getFilter();
        MockFilter lastFilter = (MockFilter) last.getFilter();

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
