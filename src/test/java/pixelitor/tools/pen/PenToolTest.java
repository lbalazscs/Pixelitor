/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.pen;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;

import static pixelitor.TestHelper.assertHistoryEditsAre;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.history.History.redo;
import static pixelitor.history.History.undo;
import static pixelitor.selection.SelectionType.RECTANGLE;
import static pixelitor.selection.ShapeCombination.REPLACE;
import static pixelitor.tools.pen.PenToolMode.BUILD;
import static pixelitor.tools.pen.PenToolMode.EDIT;

@DisplayName("Pen Tool tests")
@TestMethodOrder(MethodOrderer.Random.class)
class PenToolTest {
    private View view;
    private Composition comp;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        Tools.setCurrentTool(Tools.PEN);

        // a real composition that can store paths
        comp = TestHelper.createEmptyComp(300, 300);
        view = comp.getView(); // a mock view
        PenTool.path = null;
        Tools.PEN.startBuilding(false);
        assertThat(Tools.PEN)
            .isActive()
            .hasNoPath()
            .pathActionAreNotEnabled()
            .modeIs(BUILD)
            .isConsistent();

        History.clear();
    }

    @Test
    @DisplayName("convert path to selection (build mode)")
    void convertBuiltPathToSelection() {
        createSimpleClosedPathInBuildMode();

        Tools.PEN.convertToSelection();
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();

        undo("Convert Path to Selection");
        assertThat(Tools.PEN)
            .isActive()
            .modeIs(BUILD) // return to BUILD mode!
            .hasPath()
            .isConsistent();
        assertThat(comp).doesNotHaveSelection();

        redo("Convert Path to Selection");
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();
    }

    @Test
    @DisplayName("convert path to selection (edit mode)")
    void convertEditPathToSelection() {
        createSimpleClosedPathInBuildMode();
        Tools.PEN.startRestrictedMode(EDIT, false);
        assertThat(Tools.PEN)
            .isActive()
            .isConsistent()
            .modeIs(EDIT);

        Tools.PEN.convertToSelection();
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();

        undo("Convert Path to Selection");
        assertThat(Tools.PEN)
            .isActive()
            .isConsistent()
            .modeIs(EDIT) // return to EDIT mode!
            .hasPath();
        assertThat(comp).doesNotHaveSelection();

        redo("Convert Path to Selection");
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();
    }

    @Test
    @DisplayName("convert selection to path")
    void convertSelectionToPath() {
        Tools.setCurrentTool(Tools.SELECTION);
        assertThat(Tools.SELECTION)
            .isActive()
            .selectionTypeIs(RECTANGLE)
            .interactionIs(REPLACE);

        // build a quick rectangular selection by dragging
        press(100, 100);
        drag(150, 150);
        release(200, 200);

        assertThat(comp).hasSelection();

        SelectionActions.getConvertToPath().actionPerformed(null);
        assertThat(Tools.PEN)
            .isActive()
            .isConsistent()
            .modeIs(EDIT)
            .hasPath();
        assertThat(comp).doesNotHaveSelection();

        undo("Convert Selection to Path");
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();

        redo("Convert Selection to Path");
        assertThat(Tools.PEN)
            .isActive()
            .isConsistent()
            .modeIs(EDIT)
            .hasPath();
        assertThat(comp).doesNotHaveSelection();
    }

    @Test
    @DisplayName("undo an edit-mode change in build-mode")
    void undoEditModeChangeInBuildMode() {
        // create a 2-point path in build mode
        Tools.PEN.startBuilding(false);
        click(100, 100);
        click(200, 100);

        Path path = PenTool.path;
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(2).isNotClosed();
        AnchorPoint firstAnchor = sp.getAnchor(0);
        assertThat(firstAnchor).isAt(100, 100);

        // switch to edit mode
        Tools.PEN.startRestrictedMode(EDIT, false);
        assertThat(Tools.PEN)
            .hasPath()
            .isConsistent()
            .modeIs(EDIT);

        // drag the first point downwards
        press(100, 100);
        release(100, 200);
        assertThat(firstAnchor).isAt(100, 200);

        // switch back to build mode
        Tools.PEN.startBuilding(false);
        assertThat(Tools.PEN)
            .hasPath()
            .isConsistent()
            .modeIs(BUILD);

        // undo
        assertThat(firstAnchor).isAt(100, 200);
        undo("Move Anchor Point");
        assertThat(firstAnchor).isAt(100, 100);

        // continue building
        click(300, 100);
        assertThat(firstAnchor).isAt(100, 100);
    }

    @Test
    @DisplayName("undo a build-mode change in edit-mode")
    void undoBuildModeChangeInEditMode() {
        SubPath sp = createSimpleClosedPathInBuildMode();

        // switch to edit mode
        Tools.PEN.startRestrictedMode(EDIT, false);
        assertThat(Tools.PEN)
            .hasPath()
            .isConsistent()
            .modeIs(EDIT);

        // move the first point upwards
        AnchorPoint ap1 = sp.getAnchor(0);
        assertThat(ap1).isAt(100, 100);
        press(100, 100);
        release(100, 50);
        assertThat(ap1).isAt(100, 50);

        assertHistoryEditsAre(
            "Subpath Start",
            "Add Anchor Point",
            "Add Anchor Point",
            "Close Subpath",
            "Move Anchor Point");

        // undo everything
        undo("Move Anchor Point");
        undo("Close Subpath");
        undo("Add Anchor Point");
        undo("Add Anchor Point");
        undo("Subpath Start");

        assertThat(Tools.PEN)
            .hasNoPath()
            .isConsistent()
            .modeIs(BUILD);

        // redo everything
        redo("Subpath Start");
        assertThat(Tools.PEN)
            .hasPath()
            .isConsistent();

        redo("Add Anchor Point");
        redo("Add Anchor Point");

        assertThat(sp).isNotClosed();
        redo("Close Subpath");
        assertThat(sp).isClosed();

        assertThat(ap1).isAt(100, 100);
        redo("Move Anchor Point");
        assertThat(ap1).isAt(100, 50);
    }

    @Test
    @DisplayName("delete subpath and path in edit mode")
    void deleteSubPathAndPathInEditMode() {
        // create a path with two subpaths
        Path path = new Path(comp, true);
        path.startNewSubpath(10, 20, view);
        SubPath sp1 = path.getActiveSubpath();
        sp1.addPoint(20, 10);
        sp1.finish(comp, false);
        path.startNewSubpath(100, 20, view);
        SubPath sp2 = path.getActiveSubpath();
        sp2.addPoint(100, 120);
        sp2.finish(comp, false);

        // there are no edits yet, because this was not created with the build API
        History.assertNumEditsIs(0);

        Tools.PEN.setPath(path);
        Tools.PEN.startRestrictedMode(EDIT, false);

        assertThat(Tools.PEN)
            .pathIs(path)
            .modeIs(EDIT)
            .isConsistent();
        assertThat(PenTool.path)
            .numSubPathsIs(2)
            .activeSubPathIs(sp2);

        sp2.delete();
        History.assertNumEditsIs(1);
        assertThat(PenTool.path)
            .numSubPathsIs(1)
            .activeSubPathIs(sp1);
        assertThat(Tools.PEN)
            .modeIs(EDIT)
            .isConsistent();

        // go to build mode and back to edit - should have no effect
        Tools.PEN.startBuilding(false);
        Tools.PEN.startRestrictedMode(EDIT, false);

        undo("Delete Subpath");
        assertThat(PenTool.path).numSubPathsIs(2);

        redo("Delete Subpath");
        assertThat(PenTool.path)
            .numSubPathsIs(1)
            .activeSubPathIs(sp1);
        assertThat(Tools.PEN)
            .modeIs(EDIT)
            .isConsistent();

        path = PenTool.path;
        PenTool.path.delete();
        History.assertNumEditsIs(2);
        assertThat(Tools.PEN)
            .hasNoPath()
            .isConsistent()
            .modeIs(BUILD);

        undo("Delete Path");
        assertThat(Tools.PEN)
            .pathIs(path)
            .isConsistent()
            .modeIs(EDIT);

        redo("Delete Path");
        assertThat(Tools.PEN)
            .hasNoPath()
            .isConsistent()
            .modeIs(BUILD);
    }

    private SubPath createSimpleClosedPathInBuildMode() {
        assertThat(Tools.PEN)
            .hasNoPath()
            .isConsistent()
            .pathActionAreNotEnabled();

        Tools.PEN.startBuilding(false);

        // add first anchor point
        click(100, 100);

        // add second anchor point
        click(200, 100);

        // add third anchor point
        click(100, 200);

        // close by clicking on the first point
        click(100, 100);

        assertThat(Tools.PEN)
            .hasPath()
            .isConsistent()
            .pathActionAreEnabled();
        assertThat(PenTool.path).numSubPathsIs(1);

        SubPath sp = PenTool.path.getSubPath(0);
        assertThat(sp)
            .isClosed()
            .isFinished()
            .numAnchorsIs(3);

        // undo everything
        undo("Close Subpath");
        undo("Add Anchor Point");
        undo("Add Anchor Point");
        undo("Subpath Start");

        assertThat(Tools.PEN)
            .hasNoPath()
            .isConsistent()
            .pathActionAreNotEnabled();

        // redo everything
        redo("Subpath Start");
        redo("Add Anchor Point");
        redo("Add Anchor Point");
        redo("Close Subpath");

        assertThat(Tools.PEN)
            .hasPath()
            .isConsistent()
            .pathActionAreEnabled();
        assertThat(PenTool.path).numSubPathsIs(1);

        Assertions.assertSame(sp, PenTool.path.getSubPath(0));
        assertThat(sp)
            .isClosed()
            .isFinished()
            .numAnchorsIs(3);

        return sp;
    }

    private void click(int x, int y) {
        press(x, y);
        release(x, y);
    }

    private void press(int x, int y) {
        TestHelper.press(x, y, view);
    }

    private void drag(int x, int y) {
        TestHelper.drag(x, y, view);
    }

    private void release(int x, int y) {
        TestHelper.release(x, y, view);
    }
}