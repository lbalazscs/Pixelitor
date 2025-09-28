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
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.layers.ColorFillLayer;
import pixelitor.tools.Tools;
import pixelitor.utils.input.Modifiers;

import java.awt.Graphics2D;

import static org.mockito.Mockito.mock;
import static pixelitor.TestHelper.assertHistoryEditsAre;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.BuildState.DRAGGING_LAST_CONTROL;
import static pixelitor.tools.pen.BuildState.DRAG_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.IDLE;
import static pixelitor.tools.pen.BuildState.MOVE_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;

@DisplayName("Pen Tool: path building tests")
@TestMethodOrder(MethodOrderer.Random.class)
class PathBuildingTest {
    private View view;
    private Composition comp;

    private Graphics2D g;
    private PenTool penTool;

    enum CtrlOrAlt {CTRL, ALT}

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
        Tools.setActiveTool(Tools.PEN);
    }

    @BeforeEach
    void beforeEachTest() {
        // A real composition that can store paths.
        // The layer type doesn't matter.
        comp = TestHelper.createRealComp("PathBuildingTest", ColorFillLayer.class);

        view = comp.getView(); // a mock view
        g = mock(Graphics2D.class);

        penTool = Tools.PEN;

        // reset the state between the tests
        Tools.PEN.removePath();
        History.clear();

        assertThat(Tools.PEN).isActive();
        assertThat(comp).hasNoPath();
    }

    @Test
    @DisplayName("closed, curved path")
    void closedCurvedPath() {
        SubPath subPath = build3PointSubPath(true, true, 100, 100);
        undoRedo3PointSubpath(subPath, true);
    }

    @Test
    @DisplayName("closed, straight path")
    void closedStraightPath() {
        SubPath subPath = build3PointSubPath(true, false, 100, 100);
        undoRedo3PointSubpath(subPath, true);
    }

    @Test
    @DisplayName("open, curved path")
    void openCurvedPath() {
        SubPath subPath = build3PointSubPath(false, true, 100, 100);
        undoRedo3PointSubpath(subPath, false);
    }

    @Test
    @DisplayName("open, straight path")
    void openStraightPath() {
        SubPath subPath = build3PointSubPath(false, false, 100, 100);
        undoRedo3PointSubpath(subPath, false);
    }

    @Test
    @DisplayName("multiple subpaths")
    void multipleSubPaths() {
        build3PointSubPath(false, false, 100, 100);
        build3PointSubPath(true, true, 300, 100);
        undoRedoMultipleSubpaths();
    }

    @Test
    @DisplayName("undo after mouse press")
    void undoAfterMousePressed() {
        press(100, 100, DRAGGING_LAST_CONTROL);
        Path path = comp.getActivePath();
        assertThat(path.getActiveSubpath()).numAnchorsIs(1);

        undo("Subpath Start", null);
        assertThat(comp).hasNoPath();

        drag(110, 100, null);

        // dragging state because the mouse is down
        redo("Subpath Start", DRAGGING_LAST_CONTROL);
        assertThat(comp).activePathIs(path);

        undo("Subpath Start", null);
        assertThat(comp).hasNoPath();

        // the difference is that this time the mouse is released after the undo
        release(100, 100, null);

        // moving state because the mouse is up
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(comp).activePathIs(path);
    }

    @Test
    @DisplayName("undo after mouse click")
    void undoAfterMouseClicked() {
        click(100, 100);

        Path path = comp.getActivePath();
        assertThat(path.getActiveSubpath()).numAnchorsIs(1);

        undo("Subpath Start", null);
        assertThat(comp).hasNoPath();

        move(110, 100, null);

        // moving state because the mouse is up
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(comp).activePathIs(path);

        undo("Subpath Start", null);
        assertThat(comp).hasNoPath();

        // a mouse press would start a new path
    }

    /**
     * "The Bezier Game" at https://bezier.method.ac/ has this
     * challenge where a heart shape is built using only two anchor points.
     * The handles are broken twice with Alt while building the shape
     */
    @Test
    @DisplayName("two-point heart")
    void buildingTwoPointHeart() {
        int p1x = 100;
        int p1y = 100;
        // press at the first anchor point
        press(p1x, p1y, DRAGGING_LAST_CONTROL);

        Path path = comp.getActivePath();
        assertThat(path).isNotNull();

        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath).numAnchorsIs(1);

        AnchorPoint firstAnchor = subpath.getAnchor(0);
        assertThat(firstAnchor)
            .typeIs(SYMMETRIC)
            .isAt(p1x, p1y);

        // drag the "out" handle in the opposite direction of the desired "in" control
        drag(p1x + 25, p1y + 25, DRAGGING_LAST_CONTROL);
        drag(p1x + 50, p1y + 50, DRAGGING_LAST_CONTROL);
        assertThat(firstAnchor.ctrlOut).isAt(p1x + 50, p1y + 50);
        assertThat(firstAnchor.ctrlIn).isAt(p1x - 50, p1y - 50);

        // alt-drag the "out" handle to its place
        altDrag(p1x + 50, p1y, DRAGGING_LAST_CONTROL);
        assertThat(firstAnchor).typeIs(CUSP);
        altDrag(p1x + 50, p1y - 50, DRAGGING_LAST_CONTROL);
        release(p1x + 50, p1y - 50, MOVING_TO_NEXT_ANCHOR);
        assertThat(firstAnchor.ctrlOut).isAt(p1x + 50, p1y - 50);
        // since alt breaks the handles, the "in" handle is not supposed to move
        assertThat(firstAnchor.ctrlIn).isAt(p1x - 50, p1y - 50);

        // move the mouse to the second anchor point
        int p2x = 100;
        int p2y = 200;
        move(p2x, p2y, MOVING_TO_NEXT_ANCHOR);
        // press to fix the second anchor point
        press(p2x, p2y, DRAGGING_LAST_CONTROL);
        assertThat(subpath).numAnchorsIs(2);
        AnchorPoint secondAnchor = subpath.getAnchor(1);
        assertThat(secondAnchor)
            .typeIs(SYMMETRIC)
            .isAt(p2x, p2y);

        // drag in the opposite direction of the desired "in" control
        drag(p2x - 25, p2y + 25, DRAGGING_LAST_CONTROL);
        drag(p2x - 50, p2y + 50, DRAGGING_LAST_CONTROL);
        assertThat(secondAnchor.ctrlOut).isAt(p2x - 50, p2y + 50);
        assertThat(secondAnchor.ctrlIn).isAt(p2x + 50, p2y - 50);

        // alt-drag the "out" handle to its place
        altDrag(p2x - 50, p2y - 25, DRAGGING_LAST_CONTROL);
        assertThat(secondAnchor).typeIs(CUSP);
        altDrag(p2x - 50, p2y - 50, DRAGGING_LAST_CONTROL);
        release(p2x - 50, p2y - 50, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchor.ctrlOut).isAt(p2x - 50, p2y - 50);
        // since alt breaks the handles, the "in" handle is not supposed to move
        assertThat(secondAnchor.ctrlIn).isAt(p2x + 50, p2y - 50);

        // move back to the first anchor point
        move(p1x, p1y, MOVING_TO_NEXT_ANCHOR);

        // press to close
        press(p1x, p1y, IDLE);
        assertThat(subpath)
            .numAnchorsIs(2)
            .isClosed()
            .isFinished();

        // releasing should not change anything
        release(p1x, p1y, IDLE);
        assertThat(subpath)
            .numAnchorsIs(2)
            .isClosed()
            .isFinished()
            .isConsistent();
        assertThat(path)
            .numSubPathsIs(1)
            .activeSubPathIs(subpath)
            .isConsistent();
    }

    // The previous "heart with two anchors" test only
    // broke the currently dragged handles by alt-dragging.
    // This one tests breaking old handles.
    @Test
    @DisplayName("breaking old handles with Alt")
    void breakingOldHandlesWithAlt() {
        // add the first anchor point
        press(100, 100, DRAGGING_LAST_CONTROL);
        Path path = comp.getActivePath();
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath).numAnchorsIs(1);
        AnchorPoint firstAnchor = subpath.getAnchor(0);

        // drag out its handles
        drag(120, 100, DRAGGING_LAST_CONTROL);
        drag(140, 100, DRAGGING_LAST_CONTROL);
        release(150, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(firstAnchor.ctrlOut).isAt(150, 100);
        assertThat(firstAnchor.ctrlIn).isAt(50, 100);

        // create a second point by clicking
        move(175, 100, MOVING_TO_NEXT_ANCHOR);
        click(200, 100);
        assertThat(subpath).numAnchorsIs(2);

        // alt-move back to the out control of the first point
        move(175, 100, MOVING_TO_NEXT_ANCHOR);
        altMove(165, 100, MOVE_EDITING_PREVIOUS);
        // getting close now, the out control should activate
        altMove(151, 100, MOVE_EDITING_PREVIOUS);
        assertThat(firstAnchor.ctrlOut)
            .isAt(150, 100)
            .isActive();

        // drag it downwards
        altPress(151, 100, DRAG_EDITING_PREVIOUS);
        drag(151, 150, DRAG_EDITING_PREVIOUS);
        release(151, 200, MOVING_TO_NEXT_ANCHOR);
        // check that the out handle was moved
        assertThat(firstAnchor.ctrlOut)
            .isAt(150, 200)
            .isActive();
        // check that the in handle was NOT moved
        assertThat(firstAnchor.ctrlIn)
            .isAt(50, 100)
            .isNotActive();

        // Move the mouse away. The out control should deactivate
        move(150, 150, MOVING_TO_NEXT_ANCHOR);
        assertThat(firstAnchor.ctrlOut)
            .isAt(150, 200)
            .isNotActive();
    }

    // This test drags on a previous anchor point, rather than on a control point.
    // Expected result: drag out new, symmetric control handles
    @Test
    @DisplayName("Alt-drag on a previous anchor")
    void altDragOnPreviousAnchor() {
        verifyModifierDragOnPreviousAnchor(CtrlOrAlt.ALT);
    }

    @Test
    @DisplayName("Ctrl-drag on a previous anchor")
    void movingPreviousAnchorsWithCtrl() {
        verifyModifierDragOnPreviousAnchor(CtrlOrAlt.CTRL);
    }

    @Test
    @DisplayName("moving position after undoing the closing of a subpath")
    void movingPositionAfterUndoingCloseMustBeAtMouse() {
        SubPath subPath = build3PointSubPath(true, true, 100, 100);

        // move the mouse away before undoing
        move(142, 42, IDLE);

        undo("Close Subpath", MOVING_TO_NEXT_ANCHOR);

        assertThat(subPath.getMovingPoint()).isAt(142, 42);
    }

    @Test
    @DisplayName("constrained straight path")
    void constrainedStraightPath() {
        click(100, 100);
        shiftClick(300, 110);

        Path path = comp.getActivePath();
        assertThat(path)
            .isNotNull()
            .numSubPathsIs(1)
            .isConsistent();
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath)
            .isNotFinished()
            .numAnchorsIs(2);
        AnchorPoint secondAnchor = subpath.getAnchor(1);

        // Not at 300, 110 because it is constrained.
        // The controls should remain retracted because it was a click.
        assertThat(secondAnchor)
            .isAt(300, 100)
            .bothControlsAreRetracted();

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        undo("Subpath Start", null);
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);

        // expect the constrained position
        assertThat(secondAnchor)
            .isAt(300, 100)
            .bothControlsAreRetracted();

        // ctrl-click to finish
        ctrlClick(314, 314, IDLE);
        assertThat(subpath)
            .isFinished()
            .numAnchorsIs(2);

        // Start a new subpath with shift-click.
        // As this is the first point on the subpath, it should not be constrained.
        shiftClick(511, 111);
        assertThat(path)
            .isNotNull()
            .numSubPathsIs(2)
            .isConsistent();
        SubPath newSubPath = path.getActiveSubpath();
        assertThat(newSubPath)
            .isNotFinished()
            .numAnchorsIs(1)
            .firstAnchorIsAt(511, 111);
    }

    @Test
    @DisplayName("constrained curved path")
    void constrainedCurvedPath() {
        // Start a new curve with shift-press.
        // As this is the first point on the subpath, it should not be constrained.
        shiftPress(314, 314, DRAGGING_LAST_CONTROL);

        // Shift-drag the out control to the right
        // The control should be constrained relative to the anchor.
        shiftDrag(330, 317, DRAGGING_LAST_CONTROL);
        shiftDrag(350, 318, DRAGGING_LAST_CONTROL);
        shiftRelease(350, 318, MOVING_TO_NEXT_ANCHOR);

        // Check the results so far
        Path path = comp.getActivePath();
        assertThat(path)
            .isNotNull()
            .numSubPathsIs(1)
            .isConsistent();
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath)
            .isNotFinished()
            .numAnchorsIs(1);
        AnchorPoint firstAnchor = subpath.getAnchor(0);
        assertThat(firstAnchor).isAt(314, 314);
        assertThat(firstAnchor.ctrlOut).isAt(350, 314); // constrained horizontally

        // shift-move the moving point and check that it is constrained
        // relative to the first anchor point (and not relative to its
        // out control, where the mouse was released)
        shiftMove(316, 500, MOVING_TO_NEXT_ANCHOR);
        assertThat(subpath).hasMovingPointAt(314, 500);

        // shift-press and check that the constraining is still OK
        shiftPress(316, 510, DRAGGING_LAST_CONTROL);
        assertThat(subpath)
            .hasNoMovingPoint()
            .isNotFinished()
            .numAnchorsIs(2);
        AnchorPoint secondAnchor = subpath.getAnchor(1);
        assertThat(secondAnchor).isAt(314, 510);
    }

    @Test
    @DisplayName("starting with Shift-click")
    void startingWithShiftClick() {
        // starting with shift-click should not cause any exceptions,
        // even tough the last relative coordinates are not yet initialized
        shiftClick(456, 654);

        Path path = comp.getActivePath();
        assertThat(path.getActiveSubpath())
            .isNotFinished()
            .numAnchorsIs(1)
            .firstAnchorIsAt(456, 654);
    }

    private void verifyModifierDragOnPreviousAnchor(CtrlOrAlt modifier) {
        // click to add the first anchor point
        click(100, 100);
        Path path = comp.getActivePath();
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath).numAnchorsIs(1);
        AnchorPoint firstAnchor = subpath.getAnchor(0);

        move(125, 100, MOVING_TO_NEXT_ANCHOR);
        switch (modifier) {
            case CTRL -> ctrlMove(150, 100, MOVE_EDITING_PREVIOUS);
            case ALT -> altMove(150, 100, MOVE_EDITING_PREVIOUS);
        }
        move(175, 100, MOVING_TO_NEXT_ANCHOR);

        // click again to add the second anchor point
        click(200, 100);
        assertThat(subpath).numAnchorsIs(2);
        AnchorPoint secondAnchor = subpath.getAnchor(1);

        move(225, 100, MOVING_TO_NEXT_ANCHOR);
        move(250, 100, MOVING_TO_NEXT_ANCHOR);
        switch (modifier) {
            case CTRL -> ctrlMove(275, 100, MOVE_EDITING_PREVIOUS);
            case ALT -> altMove(275, 100, MOVE_EDITING_PREVIOUS);
        }

        // click again to add the third anchor point
        click(300, 100);
        assertThat(subpath)
            .numAnchorsIs(3)
            .isNotClosed()
            .isNotFinished();

        // move back towards the second point
        move(275, 100, MOVING_TO_NEXT_ANCHOR);
        move(250, 100, MOVING_TO_NEXT_ANCHOR);

        // now we are close to the second point, it should become active
        switch (modifier) {
            case CTRL -> ctrlMove(202, 100, MOVE_EDITING_PREVIOUS);
            case ALT -> altMove(202, 100, MOVE_EDITING_PREVIOUS);
        }
        checkActive(secondAnchor, modifier, 200, 100);

        // now we are even closer, but release ctrl/alt,
        // so both of them should be inactive
        move(201, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchor).isNotActive();
        assertThat(secondAnchor.ctrlOut).isNotActive();

        // press on the second point...
        switch (modifier) {
            case CTRL -> ctrlPress(200, 100, DRAG_EDITING_PREVIOUS);
            case ALT -> altPress(200, 100, DRAG_EDITING_PREVIOUS);
        }
        checkActive(secondAnchor, modifier, 200, 100);
        // ...and drag it downwards
        drag(200, 150, DRAG_EDITING_PREVIOUS);
        checkActive(secondAnchor, modifier, 200, 150);
        release(200, 200, MOVING_TO_NEXT_ANCHOR);
        switch (modifier) {
            case CTRL -> assertThat(secondAnchor).isAt(200, 200).isActive();
            case ALT -> {
                assertThat(secondAnchor.ctrlOut).isAt(200, 200).isActive();
                // check that the in handle was moved up symmetrically...
                assertThat(secondAnchor.ctrlIn).isAt(200, 0);
                // ...and that the anchor didn't move at all
                assertThat(secondAnchor).isAt(200, 100);
            }
        }
        // move away, the point should deactivate
        move(150, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchor).isNotActive();
        assertThat(secondAnchor.ctrlOut).isNotActive();

        // ctrl-click between the points for finish the curve without closing
        ctrlPress(150, 100, IDLE);
        release(150, 100, IDLE);
        assertThat(subpath)
            .numAnchorsIs(3)
            .isNotClosed()
            .isFinished();

        move(120, 100, IDLE);
        // ctrl-click on the first point...
        ctrlPress(100, 100, DRAG_EDITING_PREVIOUS);
        // ...and drag it downwards
        drag(100, 150, DRAG_EDITING_PREVIOUS);
        release(100, 200, IDLE);
        assertThat(firstAnchor).isAt(100, 200);
    }

    private static void checkActive(AnchorPoint anchor, CtrlOrAlt modifier, double x, double y) {
        switch (modifier) {
            case CTRL -> assertThat(anchor).isActive().isAt(x, y);
            case ALT -> assertThat(anchor.ctrlOut).isActive().isAt(x, y);
        }
        assertThat(anchor).typeIs(SYMMETRIC);
    }

    private SubPath build3PointSubPath(boolean closed, boolean curved, int startX, int startY) {
        // first anchor point
        press(startX, startY, DRAGGING_LAST_CONTROL);
        Path path = comp.getActivePath();
        assertThat(path).isNotNull();
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath).numAnchorsIs(1);
        AnchorPoint firstAnchor = subpath.getAnchor(0);
        assertThat(firstAnchor)
            .isAt(startX, startY)
            .isAtIm(startX, startY);
        if (curved) {
            // curved: drag out the control points before releasing
            drag(startX + 10, startY - 10, DRAGGING_LAST_CONTROL);
            assertThat(firstAnchor.ctrlOut)
                .isAt(startX + 10, startY - 10)
                .isAtIm(startX + 10, startY - 10);
            drag(startX + 20, startY - 20, DRAGGING_LAST_CONTROL);
            drag(startX + 30, startY - 30, DRAGGING_LAST_CONTROL);
            assertThat(subpath).numAnchorsIs(1);

            // release to set the final value of the control point
            release(startX + 40, startY - 40, MOVING_TO_NEXT_ANCHOR);
            assertThat(subpath).numAnchorsIs(1);
            assertThat(firstAnchor.ctrlOut)
                .isAt(startX + 40, startY - 40)
                .isAtIm(startX + 40, startY - 40)
                .isNotRetracted();
            assertThat(firstAnchor.ctrlIn).isNotRetracted();
        } else {
            // not curved: release at the click location
            release(startX, startY, MOVING_TO_NEXT_ANCHOR);
            assertThat(firstAnchor).bothControlsAreRetracted();
        }

        // move towards the second anchor point
        move(startX + 80, startY, MOVING_TO_NEXT_ANCHOR);
        move(startX + 90, startY, MOVING_TO_NEXT_ANCHOR);

        // press to fix the second path point
        int p2x = startX + 100;
        int p2y = startY;
        press(p2x, startY, DRAGGING_LAST_CONTROL);
        assertThat(subpath).numAnchorsIs(2);
        AnchorPoint secondAnchor = subpath.getAnchor(1);
        assertThat(secondAnchor)
            .isAt(p2x, p2y)
            .isAtIm(p2x, p2y);
        if (curved) {
            // curved: drag out the control points before releasing
            drag(p2x + 10, p2y + 10, DRAGGING_LAST_CONTROL);
            drag(p2x + 20, p2y + 20, DRAGGING_LAST_CONTROL);
            drag(p2x + 30, p2y + 30, DRAGGING_LAST_CONTROL);
            // release to set the final value of the control point
            release(p2x + 30, p2y + 30, MOVING_TO_NEXT_ANCHOR);
            assertThat(secondAnchor.ctrlIn).isNotRetracted();
            assertThat(secondAnchor.ctrlOut).isNotRetracted();
        } else {
            // not curved: release at the click location
            release(p2x, p2y, MOVING_TO_NEXT_ANCHOR);
            assertThat(secondAnchor).bothControlsAreRetracted();
        }

        // move towards the third anchor point
        move(startX + 70, startY + 100, MOVING_TO_NEXT_ANCHOR);
        move(startX + 60, startY + 100, MOVING_TO_NEXT_ANCHOR);

        // third anchor point
        int p3x = startX + 50;
        int p3y = startY + 100;
        press(p3x, p3y, DRAGGING_LAST_CONTROL);

        if (!closed) {
            release(p3x + 1, p3y + 1, MOVING_TO_NEXT_ANCHOR);

            // if not closed, finish by ctrl-press somewhere
            ctrlPress(p3x + 42, p3y + 42, IDLE);
            release(p3x + 42, p3y + 42, IDLE);

            assertThat(subpath)
                .isConsistent()
                .isNotClosed()
                .isFinished()
                .firstAnchorIsNotActive()
                .numAnchorsIs(3);
            return subpath;
        }

        // from here closed is true, and the curve is closed

        AnchorPoint thirdAnchor = subpath.getAnchor(2);
        assertThat(thirdAnchor)
            .isAt(p3x, p3y)
            .isAtIm(p3x, p3y);
        if (curved) {
            // curved: drag out the control points before releasing
            drag(p3x - 10, p3y, DRAGGING_LAST_CONTROL);
            drag(p3x - 20, p3y, DRAGGING_LAST_CONTROL);
            drag(p3x - 30, p3y, DRAGGING_LAST_CONTROL);
            // release to set the final value of the control point
            release(p3x - 40, p3y, MOVING_TO_NEXT_ANCHOR);
        } else {
            // not curved: release at the click location
            release(p3x, p3y, MOVING_TO_NEXT_ANCHOR);
        }

        // move towards the first point
        move(startX, startY + 20, MOVING_TO_NEXT_ANCHOR);
        assertThat(subpath)
            .isNotClosed()
            .firstAnchorIsNotActive();
        move(startX, startY + 3, MOVING_TO_NEXT_ANCHOR);
        // we are close to the first point, it should become active
        assertThat(subpath)
            .isNotClosed()
            .isNotFinished()
            .firstAnchorIsActive();
        // click to close
        press(startX, startY, IDLE);
        assertThat(subpath)
            .isConsistent()
            .isClosed()
            .isFinished()
            .firstAnchorIsNotActive()
            .numAnchorsIs(3);
        release(startX, startY, IDLE);

        return subpath;
    }

    private void undoRedo3PointSubpath(SubPath subPath, boolean closed) {
        if (closed) {
            assertThat(subPath)
                .isClosed()
                .isFinished();
            assertHistoryEditsAre(
                "Subpath Start",
                "Add Anchor Point",
                "Add Anchor Point",
                "Close Subpath");
        } else {
            assertThat(subPath)
                .isNotClosed()
                .isFinished();
            assertHistoryEditsAre(
                "Subpath Start",
                "Add Anchor Point",
                "Add Anchor Point",
                "Finish Subpath");
        }
        move(0, 0, IDLE);

        // now undo until everything is undone
        if (closed) {
            undo("Close Subpath", MOVING_TO_NEXT_ANCHOR);
        } else {
            undo("Finish Subpath", MOVING_TO_NEXT_ANCHOR);
        }
        assertThat(subPath)
            .isNotClosed()
            .isNotFinished()
            .numAnchorsIs(3);
        // move the mouse to check that the undo left a good state
        move(1, 1, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(subPath)
            .numAnchorsIs(2);
        move(2, 2, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(subPath)
            .isNotClosed()
            .isNotFinished()
            .numAnchorsIs(1);
        move(3, 3, MOVING_TO_NEXT_ANCHOR);

        undo("Subpath Start", null);
        assertThat(subPath)
            .numAnchorsIs(1); // the edit removes the entire subpath
        assertThat(comp).hasNoPath();
        move(4, 4, null);

        // now redo until everything is redone
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(subPath)
            .numAnchorsIs(1);
        move(5, 5, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(subPath)
            .numAnchorsIs(2);
        move(6, 6, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(subPath)
            .isNotClosed()
            .isNotFinished()
            .numAnchorsIs(3);
        move(7, 7, MOVING_TO_NEXT_ANCHOR);

        if (closed) {
            redo("Close Subpath", IDLE);
            assertThat(subPath)
                .isClosed()
                .isFinished()
                .numAnchorsIs(3);
            move(8, 8, IDLE);
        } else {
            redo("Finish Subpath", IDLE);
            assertThat(subPath)
                .isNotClosed()
                .isFinished()
                .numAnchorsIs(3);
            move(8, 8, IDLE);
        }
    }

    private void undoRedoMultipleSubpaths() {
        // undo and redo everything, adding mouse movements to
        // check that the state left bt undo/redo is OK
        assertHistoryEditsAre(
            "Subpath Start",
            "Add Anchor Point",
            "Add Anchor Point",
            "Finish Subpath",
            "Subpath Start",
            "Add Anchor Point",
            "Add Anchor Point",
            "Close Subpath");

        undo("Close Subpath", MOVING_TO_NEXT_ANCHOR);
        move(1, 1, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(2, 2, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(3, 3, MOVING_TO_NEXT_ANCHOR);

        undo("Subpath Start", IDLE);
        move(4, 4, IDLE);

        undo("Finish Subpath", MOVING_TO_NEXT_ANCHOR);
        move(5, 5, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(6, 6, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(7, 7, MOVING_TO_NEXT_ANCHOR);

        undo("Subpath Start", null);
        move(8, 8, null);

        // now redo everything
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        move(9, 9, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(10, 10, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(11, 11, MOVING_TO_NEXT_ANCHOR);

        redo("Finish Subpath", IDLE);
        move(12, 12, IDLE);

        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        move(13, 13, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(14, 14, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(15, 15, MOVING_TO_NEXT_ANCHOR);

        redo("Close Subpath", IDLE);
        move(16, 16, IDLE);
    }

    private void undo(String edit, BuildState state) {
        History.undo(edit);
        checkState(state);
    }

    private void redo(String edit, BuildState state) {
        History.redo(edit);
        checkState(state);
    }

    private void press(int x, int y, BuildState state) {
        press(x, y, state, Modifiers.NONE);
    }

    private void ctrlPress(int x, int y, BuildState state) {
        press(x, y, state, Modifiers.CTRL);
    }

    private void altPress(int x, int y, BuildState state) {
        press(x, y, state, Modifiers.ALT);
    }

    private void shiftPress(int x, int y, BuildState state) {
        press(x, y, state, Modifiers.SHIFT);
    }

    private void press(int x, int y, BuildState state, Modifiers modifiers) {
        // go through the event dispatcher
        // because the undo uses its "mouseDown" state
        modifiers.dispatchPressedEvent(x, y, view);
        checkState(state);
        penTool.paintOverCanvas(g, comp);
    }

    private void click(int x, int y) {
        press(x, y, DRAGGING_LAST_CONTROL);
        release(x, y, MOVING_TO_NEXT_ANCHOR);
    }

    private void shiftClick(int x, int y) {
        shiftPress(x, y, DRAGGING_LAST_CONTROL);
        shiftRelease(x, y, MOVING_TO_NEXT_ANCHOR);
    }

    private void ctrlClick(int x, int y, BuildState state) {
        ctrlPress(x, y, state);
        ctrlRelease(x, y, state);
    }

    private void drag(int x, int y, BuildState state) {
        drag(x, y, state, Modifiers.NONE);
    }

    private void altDrag(int x, int y, BuildState state) {
        drag(x, y, state, Modifiers.ALT);
    }

    private void shiftDrag(int x, int y, BuildState state) {
        drag(x, y, state, Modifiers.SHIFT);
    }

    private void drag(int x, int y, BuildState state, Modifiers modifiers) {
        modifiers.dispatchDraggedEvent(x, y, view);
        checkState(state);
        penTool.paintOverCanvas(g, comp);
    }

    private void release(int x, int y, BuildState state) {
        release(x, y, state, Modifiers.NONE);
    }

    private void shiftRelease(int x, int y, BuildState state) {
        release(x, y, state, Modifiers.SHIFT);
    }

    private void ctrlRelease(int x, int y, BuildState state) {
        release(x, y, state, Modifiers.CTRL);
    }

    private void release(int x, int y, BuildState state, Modifiers modifiers) {
        modifiers.dispatchReleasedEvent(x, y, view);
        checkState(state);
        penTool.paintOverCanvas(g, comp);
    }

    private void move(int x, int y, BuildState state) {
        move(x, y, state, Modifiers.NONE);
    }

    private void ctrlMove(int x, int y, BuildState state) {
        move(x, y, state, Modifiers.CTRL);
    }

    private void altMove(int x, int y, BuildState state) {
        move(x, y, state, Modifiers.ALT);
    }

    private void shiftMove(int x, int y, BuildState state) {
        move(x, y, state, Modifiers.SHIFT);
    }

    private void move(int x, int y, BuildState state, Modifiers modifiers) {
        modifiers.dispatchMoveEvent(x, y, view);
        checkState(state);
        penTool.paintOverCanvas(g, comp);
    }

    private void checkState(BuildState expected) {
        Path path = comp.getActivePath();
        if (expected == null) {
            assertThat(path).isNull();
        } else {
            assertThat(penTool.getBuildState()).isEqualTo(expected);
        }
    }
}
