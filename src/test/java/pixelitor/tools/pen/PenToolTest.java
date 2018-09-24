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

package pixelitor.tools.pen;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.gui.ImageComponent;
import pixelitor.history.History;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tools;

import static org.junit.Assert.assertTrue;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.history.History.redo;
import static pixelitor.history.History.undo;
import static pixelitor.selection.SelectionInteraction.REPLACE;
import static pixelitor.selection.SelectionType.RECTANGLE;
import static pixelitor.tools.pen.PenToolMode.BUILD;
import static pixelitor.tools.pen.PenToolMode.EDIT;

public class PenToolTest {
    private ImageComponent ic;
    private Composition comp;

    @BeforeClass
    public static void setupClass() {
        Build.setTestingMode();
    }

    @Before
    public void setup() {
        Tools.changeTo(Tools.PEN);

        // a real comp that can store paths
        comp = TestHelper.createEmptyComposition(300, 300);
        ic = comp.getIC(); // a mock IC
        PenTool.path = null;
        Tools.PEN.startBuilding(false);
        assertThat(Tools.PEN)
                .isActive()
                .hasNoPath()
                .pathActionAreNotEnabled()
                .modeIs(BUILD);

        History.clear();
    }

    @Test
    public void testConvertBuildPathToSelection() {
        createSimpleClosedPathInBuildMode();

        Tools.PEN.convertToSelection(true);
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();

        undo("Convert Path to Selection");
        assertThat(Tools.PEN)
                .isActive()
                .modeIs(BUILD) // return to BUILD mode!
                .hasPath();
        assertThat(comp).doesNotHaveSelection();

        redo("Convert Path to Selection");
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();
    }

    @Test
    public void testConvertEditPathToSelection() {
        createSimpleClosedPathInBuildMode();
        Tools.PEN.startEditing(false);
        assertThat(Tools.PEN).isActive().modeIs(EDIT);

        Tools.PEN.convertToSelection(true);
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();

        undo("Convert Path to Selection");
        assertThat(Tools.PEN)
                .isActive()
                .modeIs(EDIT) // return to EDIT mode!
                .hasPath();
        assertThat(comp).doesNotHaveSelection();

        redo("Convert Path to Selection");
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();
    }

    @Test
    public void testConvertSelectionToPath() {
        Tools.changeTo(Tools.SELECTION);
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
                .modeIs(EDIT)
                .hasPath();
        assertThat(comp).doesNotHaveSelection();

        undo("Convert Selection to Path");
        assertThat(Tools.SELECTION).isActive();
        assertThat(comp).hasSelection();

        redo("Convert Selection to Path");
        assertThat(Tools.PEN)
                .isActive()
                .modeIs(EDIT)
                .hasPath();
        assertThat(comp).doesNotHaveSelection();
    }

    @Test
    public void testUndoEditModeChangeInBuildMode() {
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
        Tools.PEN.startEditing(false);
        assertThat(Tools.PEN)
                .hasPath()
                .modeIs(EDIT);

        // drag the first point downwards
        press(100, 100);
        release(100, 200);
        assertThat(firstAnchor).isAt(100, 200);

        // switch back to build mode
        Tools.PEN.startBuilding(false);
        assertThat(Tools.PEN)
                .hasPath()
                .modeIs(BUILD);

        // undo
        assertThat(firstAnchor).isAt(100, 200);
        undo("Move Handle");
        assertThat(firstAnchor).isAt(100, 100);

        // continue building
        click(300, 100);
        assertThat(firstAnchor).isAt(100, 100);
    }

    @Test
    public void testUndoBuildModeChangeInEditMode() {
        SubPath sp = createSimpleClosedPathInBuildMode();

        // switch to edit mode
        Tools.PEN.startEditing(false);
        assertThat(Tools.PEN)
                .hasPath()
                .modeIs(EDIT);

        // move the first point upwards
        AnchorPoint ap1 = sp.getAnchor(0);
        assertThat(ap1).isAt(100, 100);
        press(100, 100);
        release(100, 50);
        assertThat(ap1).isAt(100, 50);

        assertThat(History.asStringList()).containsExactly(
                "Subpath Start",
                "Add Anchor Point",
                "Add Anchor Point",
                "Close Subpath",
                "Move Handle");

        // undo everything
        undo("Move Handle");
        undo("Close Subpath");
        undo("Add Anchor Point");
        undo("Add Anchor Point");
        undo("Subpath Start");

        assertThat(Tools.PEN)
                .hasNoPath()
                .modeIs(BUILD);

        // redo everything
        redo("Subpath Start");
        assertThat(Tools.PEN).hasPath();

        redo("Add Anchor Point");
        redo("Add Anchor Point");

        assertThat(sp).isNotClosed();
        redo("Close Subpath");
        assertThat(sp).isClosed();

        assertThat(ap1).isAt(100, 100);
        redo("Move Handle");
        assertThat(ap1).isAt(100, 50);
    }

    @Test
    public void testDeleteSubPathAndPathInEditMode() {
        // create a path with two subpaths
        Path path = new Path(comp);
        path.startNewSubpath(10, 20, ic);
        SubPath sp1 = path.getActiveSubpath();
        sp1.addPoint(20, 10);
        sp1.finish(comp, "test", false);
        path.startNewSubpath(100, 20, ic);
        SubPath sp2 = path.getActiveSubpath();
        sp2.addPoint(100, 120);
        sp2.finish(comp, "test", false);

        // there are no edits yet, because this was not created with the build API
        History.assertNumEditsIs(0);

        Tools.PEN.setPath(path);
        Tools.PEN.startEditing(false);

        assertThat(Tools.PEN)
                .pathIs(path)
                .modeIs(EDIT);
        assertThat(PenTool.path)
                .numSubPathsIs(2)
                .activeSubPathIs(sp2);

        sp2.delete();
        History.assertNumEditsIs(1);
        assertThat(PenTool.path)
                .numSubPathsIs(1)
                .activeSubPathIs(sp1);
        assertThat(Tools.PEN).modeIs(EDIT);

        undo("Delete Subpath");
        assertThat(PenTool.path).numSubPathsIs(2);

        redo("Delete Subpath");
        assertThat(PenTool.path)
                .numSubPathsIs(1)
                .activeSubPathIs(sp1);
        assertThat(Tools.PEN).modeIs(EDIT);

        path = PenTool.path;
        PenTool.path.delete();
        History.assertNumEditsIs(2);
        assertThat(Tools.PEN)
                .hasNoPath()
                .modeIs(BUILD);

        undo("Delete Path");
        assertThat(Tools.PEN)
                .pathIs(path)
                .modeIs(EDIT);

        redo("Delete Path");
        assertThat(Tools.PEN)
                .hasNoPath()
                .modeIs(BUILD);
    }

    private SubPath createSimpleClosedPathInBuildMode() {
        assertThat(Tools.PEN)
                .hasNoPath()
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
                .pathActionAreEnabled();
        assertThat(PenTool.path).numSubPathsIs(1);

        SubPath sp = PenTool.path.getSubPath(0);
        assertThat(sp)
                .isClosed()
                .isFinished()
                .numAnchorsIs(3);

        // undo everything
        History.undo("Close Subpath");
        History.undo("Add Anchor Point");
        History.undo("Add Anchor Point");
        History.undo("Subpath Start");

        assertThat(Tools.PEN)
                .hasNoPath()
                .pathActionAreNotEnabled();

        // redo everything
        History.redo("Subpath Start");
        History.redo("Add Anchor Point");
        History.redo("Add Anchor Point");
        History.redo("Close Subpath");

        assertThat(Tools.PEN)
                .hasPath()
                .pathActionAreEnabled();
        assertThat(PenTool.path).numSubPathsIs(1);

        assertTrue(sp == PenTool.path.getSubPath(0));
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
        TestHelper.press(x, y, ic);
    }

    private void drag(int x, int y) {
        TestHelper.drag(x, y, ic);
    }

    private void release(int x, int y) {
        TestHelper.release(x, y, ic);
    }
}