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

package pixelitor.tools.pen;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.layers.ColorFillLayer;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;
import pixelitor.utils.input.Modifiers;

import static pixelitor.TestHelper.assertHistoryEditsAre;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.history.History.redo;
import static pixelitor.history.History.undo;
import static pixelitor.selection.ShapeCombinator.REPLACE;

@DisplayName("Pen Tool tests")
@TestMethodOrder(MethodOrderer.Random.class)
class PathToolTest {
    private View view;
    private Composition comp;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        Tools.setActiveTool(Tools.PEN);

        // A real composition that can store paths.
        // The layer type doesn't matter.
        comp = TestHelper.createRealComp("PenToolTest", ColorFillLayer.class, 300, 300);

        view = comp.getView(); // a mock view
        Tools.PEN.reset();
        Tools.PEN.activate();
        Tools.PEN.toolActivated(view);

        assertThat(Tools.PEN)
            .isActive()
            .pathActionAreNotEnabled();
        assertThat(comp).hasNoPath();

        History.clear();
    }

    @Test
    @DisplayName("convert path to selection (build mode)")
    void convertBuiltPathToSelection() {
        createSimpleClosedPathInBuildMode();

        PathActions.convertToSelection();
        assertThat(Tools.LASSO_SELECTION).isActive();
        assertThat(comp).hasSelection();

        undo("Convert Path to Selection");
        assertThat(Tools.PEN).isActive();
        assertThat(comp)
            .hasPath()
            .doesNotHaveSelection();

        redo("Convert Path to Selection");
        assertThat(Tools.LASSO_SELECTION).isActive();
        assertThat(comp)
            .hasNoPath()
            .hasSelection();
    }

    @Test
    @DisplayName("convert path to selection (edit mode)")
    void convertEditPathToSelection() {
        createSimpleClosedPathInBuildMode();
        Tools.NODE.activate();
        assertThat(Tools.NODE).isActive();
        assertThat(comp)
            .hasPath()
            .doesNotHaveSelection();

        PathActions.convertToSelection();
        assertThat(Tools.LASSO_SELECTION).isActive();
        assertThat(comp)
            .hasNoPath()
            .hasSelection();

        undo("Convert Path to Selection");
        assertThat(Tools.NODE).isActive();
        assertThat(comp)
            .hasPath()
            .doesNotHaveSelection();

        redo("Convert Path to Selection");
        assertThat(Tools.LASSO_SELECTION).isActive();
        assertThat(comp)
            .hasNoPath()
            .hasSelection();
    }

    @Test
    @DisplayName("convert selection to path")
    void convertSelectionToPath() {
        Tools.setActiveTool(Tools.RECTANGLE_SELECTION);
        assertThat(Tools.RECTANGLE_SELECTION)
            .isActive()
            .combinatorIs(REPLACE);

        // build a quick rectangular selection by dragging
        press(100, 100);
        drag(150, 150);
        release(200, 200);

        assertThat(comp).hasSelection();

        SelectionActions.getConvertToPath().actionPerformed(null);
        assertThat(Tools.NODE)
            .isActive();
        assertThat(comp).doesNotHaveSelection();

        undo("Convert Selection to Path");
        assertThat(Tools.LASSO_SELECTION).isActive();
        assertThat(comp).hasSelection();

        redo("Convert Selection to Path");
        assertThat(Tools.NODE)
            .isActive();
        assertThat(comp).doesNotHaveSelection();
    }

    @Test
    @DisplayName("undo an edit-mode change in build-mode")
    void undoEditModeChangeInBuildMode() {
        // create a 2-point path in build mode
        Tools.PEN.activate();
        click(100, 100);
        click(200, 100);

        Path path = comp.getActivePath();
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath).numAnchorsIs(2).isNotClosed();
        AnchorPoint firstAnchor = subpath.getAnchor(0);
        assertThat(firstAnchor).isAt(100, 100);

        // switch to edit mode
        Tools.NODE.activate();
        assertThat(Tools.NODE).isActive();
        assertThat(comp).hasPath();

        // drag the first point downwards
        press(100, 100);
        release(100, 200);
        assertThat(firstAnchor).isAt(100, 200);

        // switch back to build mode
        Tools.PEN.activate();
        assertThat(comp).hasPath();

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
        SubPath subPath = createSimpleClosedPathInBuildMode();

        // switch to edit mode
        Tools.NODE.activate();
        assertThat(comp).hasPath();

        // move the first point upwards
        AnchorPoint ap1 = subPath.getAnchor(0);
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

        assertThat(Tools.NODE).isActive();
        assertThat(comp).hasNoPath();

        // redo everything
        redo("Subpath Start");
        assertThat(Tools.NODE).isActive();
        assertThat(comp).hasPath();

        redo("Add Anchor Point");
        redo("Add Anchor Point");

        assertThat(subPath).isNotClosed();
        redo("Close Subpath");
        assertThat(subPath).isClosed();

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
        SubPath subpath = path.getActiveSubpath();
        subpath.addPoint(20, 10);
        subpath.finish(comp, false);

        path.startNewSubpath(100, 20, view);
        SubPath newSubpath = path.getActiveSubpath();
        newSubpath.addPoint(100, 120);
        newSubpath.finish(comp, false);

        // there are no edits yet, because this was not created with the build API
        History.assertNumEditsIs(0);

        Tools.NODE.activate();

        assertThat(Tools.NODE).isActive();
        assertThat(comp).activePathIs(path);
        assertThat(path)
            .numSubPathsIs(2)
            .activeSubPathIs(newSubpath);

        newSubpath.delete();
        History.assertNumEditsIs(1);
        assertThat(path)
            .numSubPathsIs(1)
            .activeSubPathIs(subpath);
        assertThat(Tools.NODE)
            .isActive();

        // go to the pen tool and back to node - should have no effect
        Tools.PEN.activate();
        Tools.NODE.activate();

        undo("Delete Subpath");
        path = Views.getActivePath(); // path reference changed by undo

        assertThat(path).numSubPathsIs(2);

        redo("Delete Subpath");
        path = Views.getActivePath(); // path reference changed by redo
        assertThat(path)
            .numSubPathsIs(1)
            .activeSubPathIs(subpath);
        assertThat(Tools.NODE)
            .isActive();

        path.delete();
        History.assertNumEditsIs(2);
        assertThat(Tools.PEN).isActive();
        assertThat(comp).hasNoPath();

        undo("Delete Path");
        assertThat(Tools.NODE).isActive();
        assertThat(comp).activePathIs(path);

        redo("Delete Path");
        assertThat(Tools.PEN).isActive();
        assertThat(comp).hasNoPath();
    }

    private SubPath createSimpleClosedPathInBuildMode() {
        Tools.PEN.activate();
        assertThat(Tools.PEN)
            .isActive()
            .pathActionAreNotEnabled();
        assertThat(comp).hasNoPath();

        // add first anchor point
        click(100, 100);

        // add second anchor point
        click(200, 100);

        // add third anchor point
        click(100, 200);

        // close by clicking on the first point
        click(100, 100);

        assertThat(Tools.PEN).isActive().pathActionAreEnabled();
        assertThat(comp).hasPath();

        Path path = comp.getActivePath();
        assertThat(path).numSubPathsIs(1);

        SubPath subPath = path.getSubPath(0);
        assertThat(subPath)
            .isClosed()
            .isFinished()
            .numAnchorsIs(3);

        // undo everything
        undo("Close Subpath");
        undo("Add Anchor Point");
        undo("Add Anchor Point");
        undo("Subpath Start");

        assertThat(comp).hasNoPath();
        assertThat(Tools.PEN)
            .pathActionAreNotEnabled();

        // redo everything
        redo("Subpath Start");
        redo("Add Anchor Point");
        redo("Add Anchor Point");
        redo("Close Subpath");

        assertThat(Tools.PEN).isActive().pathActionAreEnabled();
        assertThat(comp).hasPath();
        assertThat(path).numSubPathsIs(1);

        Assertions.assertSame(subPath, path.getSubPath(0));
        assertThat(subPath)
            .isClosed()
            .isFinished()
            .numAnchorsIs(3);

        return subPath;
    }

    private void click(int x, int y) {
        press(x, y);
        release(x, y);
    }

    private void press(int x, int y) {
        Modifiers.NONE.dispatchPressedEvent(x, y, view);
    }

    private void drag(int x, int y) {
        Modifiers.NONE.dispatchDraggedEvent(x, y, view);
    }

    private void release(int x, int y) {
        Modifiers.NONE.dispatchReleasedEvent(x, y, view);
    }
}