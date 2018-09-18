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
import pixelitor.tools.Alt;
import pixelitor.tools.Ctrl;
import pixelitor.tools.Shift;
import pixelitor.tools.Tools;

import java.awt.Graphics2D;
import java.util.List;

import static org.mockito.Mockito.mock;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.BuildState.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.BuildState.DRAG_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.MOVE_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;
import static pixelitor.tools.pen.BuildState.NO_INTERACTION;
import static pixelitor.tools.pen.PenToolMode.BUILD;

public class PathBuilderTest {
    private ImageComponent ic;
    private Graphics2D g;
    private PathBuilder pb;

    enum CtrlOrAlt {CTRL, ALT}

    @BeforeClass
    public static void setupClass() {
        Build.setTestingMode();
        Tools.changeTo(Tools.PEN);
    }

    @Before
    public void setup() {
        // a real comp that can store paths
        Composition comp = TestHelper.createEmptyComposition();
        ic = comp.getIC(); // a mock IC
        g = mock(Graphics2D.class);
        pb = BUILD;

        // reset the state between the tests
        Tools.PEN.removePath();
        History.clear();

        assertThat(Tools.PEN)
                .isActive()
                .hasNoPath()
                .modeIs(BUILD);
    }

    @Test
    public void testClosedCurvedPath() {
        SubPath sp = build3PointSubPath(true, true, 100, 100);
        undoRedo3PointSubpath(sp, true);
    }

    @Test
    public void testClosedStraightPath() {
        SubPath sp = build3PointSubPath(true, false, 100, 100);
        undoRedo3PointSubpath(sp, true);
    }

    @Test
    public void testOpenCurvedPath() {
        SubPath sp = build3PointSubPath(false, true, 100, 100);
        undoRedo3PointSubpath(sp, false);
    }

    @Test
    public void testOpenStraightPath() {
        SubPath sp = build3PointSubPath(false, false, 100, 100);
        undoRedo3PointSubpath(sp, false);
    }

    @Test
    public void testMultipleSubPaths() {
        build3PointSubPath(false, false, 100, 100);
        build3PointSubPath(true, true, 300, 100);
        undoRedoMultipleSubpaths();
    }

    @Test
    public void testUndoAfterMousePressed() {
        press(100, 100, DRAGGING_THE_CONTROL_OF_LAST);
        Path path = PenTool.path;
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(1);

        undo("Subpath Start", null);
        assertThat(Tools.PEN).hasNoPath();

        drag(110, 100, null);

        // dragging state because the mouse is down
        redo("Subpath Start", DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(Tools.PEN).pathIs(path);

        undo("Subpath Start", null);
        assertThat(Tools.PEN).hasNoPath();

        // the difference is that this time the mouse is released after the undo
        release(100, 100, null);

        // moving state because the mouse is up
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(Tools.PEN).pathIs(path);
    }

    @Test
    public void testUndoAfterMouseClicked() {
        click(100, 100);

        Path path = PenTool.path;
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(1);

        undo("Subpath Start", null);
        assertThat(Tools.PEN).hasNoPath();

        move(110, 100, null);

        // moving state because the mouse is up
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(Tools.PEN).pathIs(path);

        undo("Subpath Start", null);
        assertThat(Tools.PEN).hasNoPath();

        // a mouse press would start a new path
    }

    /**
     * "The Bezier Game" at https://bezier.method.ac/ has this
     * challenge where a heart shape is built using only two anchor points.
     * The handles are broken twice with Alt while building the shape
     */
    @Test
    public void testBuildingTwoPointHeart() {
        int p1x = 100;
        int p1y = 100;
        // press at the first anchor point
        press(p1x, p1y, DRAGGING_THE_CONTROL_OF_LAST);
        Path path = PenTool.path;
        assertThat(path).isNotNull();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(1);
        AnchorPoint firstAnchor = sp.getAnchor(0);
        assertThat(firstAnchor)
                .anchorPointTypeIs(SYMMETRIC)
                .isAt(p1x, p1y);

        // drag the "out" handle in the opposite direction of the desired "in" control
        drag(p1x + 25, p1y + 25, DRAGGING_THE_CONTROL_OF_LAST);
        drag(p1x + 50, p1y + 50, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(firstAnchor.ctrlOut).isAt(p1x + 50, p1y + 50);
        assertThat(firstAnchor.ctrlIn).isAt(p1x - 50, p1y - 50);

        // alt-drag the "out" handle to its place
        altDrag(p1x + 50, p1y, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(firstAnchor).anchorPointTypeIs(CUSP);
        altDrag(p1x + 50, p1y - 50, DRAGGING_THE_CONTROL_OF_LAST);
        release(p1x + 50, p1y - 50, MOVING_TO_NEXT_ANCHOR);
        assertThat(firstAnchor.ctrlOut).isAt(p1x + 50, p1y - 50);
        // since alt breaks the handles, the "in" handle is not supposed to move
        assertThat(firstAnchor.ctrlIn).isAt(p1x - 50, p1y - 50);

        // move the mouse to the second anchor point
        int p2x = 100;
        int p2y = 200;
        move(p2x, p2y, MOVING_TO_NEXT_ANCHOR);
        // press to fix the second anchor point
        press(p2x, p2y, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp).numAnchorsIs(2);
        AnchorPoint secondAnchor = sp.getAnchor(1);
        assertThat(secondAnchor)
                .anchorPointTypeIs(SYMMETRIC)
                .isAt(p2x, p2y);

        // drag in the opposite direction of the desired "in" control
        drag(p2x - 25, p2y + 25, DRAGGING_THE_CONTROL_OF_LAST);
        drag(p2x - 50, p2y + 50, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(secondAnchor.ctrlOut).isAt(p2x - 50, p2y + 50);
        assertThat(secondAnchor.ctrlIn).isAt(p2x + 50, p2y - 50);

        // alt-drag the "out" handle to its place
        altDrag(p2x - 50, p2y - 25, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(secondAnchor).anchorPointTypeIs(CUSP);
        altDrag(p2x - 50, p2y - 50, DRAGGING_THE_CONTROL_OF_LAST);
        release(p2x - 50, p2y - 50, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchor.ctrlOut).isAt(p2x - 50, p2y - 50);
        // since alt breaks the handles, the "in" handle is not supposed to move
        assertThat(secondAnchor.ctrlIn).isAt(p2x + 50, p2y - 50);

        // move back to the first anchor point
        move(p1x, p1y, MOVING_TO_NEXT_ANCHOR);

        // press to close
        press(p1x, p1y, NO_INTERACTION);
        assertThat(sp)
                .numAnchorsIs(2)
                .isClosed()
                .isFinished();

        // releasing should not change anything
        release(p1x, p1y, NO_INTERACTION);
        assertThat(sp)
                .numAnchorsIs(2)
                .isClosed()
                .isFinished()
                .isConsistent();
        assertThat(path)
                .numSubPathsIs(1)
                .activeSubPathIs(sp)
                .isConsistent();
    }

    // The previous "heart with two anchors" test only
    // broke the currently dragged handles by alt-dragging.
    // This one tests breaking old handles.
    @Test
    public void testBreakingOldHandlesWithAlt() {
        // add the first anchor point
        press(100, 100, DRAGGING_THE_CONTROL_OF_LAST);
        Path path = PenTool.path;
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(1);
        AnchorPoint firstAnchor = sp.getAnchor(0);

        // drag out its handles
        drag(120, 100, DRAGGING_THE_CONTROL_OF_LAST);
        drag(140, 100, DRAGGING_THE_CONTROL_OF_LAST);
        release(150, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(firstAnchor.ctrlOut).isAt(150, 100);
        assertThat(firstAnchor.ctrlIn).isAt(50, 100);

        // create a second point by clicking
        move(175, 100, MOVING_TO_NEXT_ANCHOR);
        click(200, 100);
        assertThat(sp).numAnchorsIs(2);

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
    public void testAltDragOnPreviousAnchor() {
        testSpecialDragPrevious(CtrlOrAlt.ALT);
    }

    @Test
    public void testMovingPreviousAnchorsWithCtrl() {
        testSpecialDragPrevious(CtrlOrAlt.CTRL);
    }

    @Test
    public void testMovingPositionAfterUndoingCloseMustBeAtMouse() {
        SubPath sp = build3PointSubPath(true, true, 100, 100);

        // move the mouse away before undoing
        move(142, 42, NO_INTERACTION);

        undo("Close Subpath", MOVING_TO_NEXT_ANCHOR);

        assertThat(sp.getMoving()).isAt(142, 42);
    }

    @Test
    public void testConstrainedStraightPath() {
        click(100, 100);
        shiftClick(300, 110);

        assertThat(PenTool.path)
                .isNotNull()
                .numSubPathsIs(1)
                .isConsistent();
        SubPath sp = PenTool.path.getActiveSubpath();
        assertThat(sp)
                .isNotFinished()
                .numAnchorsIs(2);
        AnchorPoint secondAnchor = sp.getAnchor(1);

        // not at 300, 110 because it is constrained
        assertThat(secondAnchor).isAt(300, 100);
        // the controls should remain retracted because it was a click
        assertThat(secondAnchor.ctrlOut).isRetracted();
        assertThat(secondAnchor.ctrlIn).isRetracted();

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        undo("Subpath Start", null);
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);

        // expect the constrained position
        assertThat(secondAnchor).isAt(300, 100);
        assertThat(secondAnchor.ctrlOut).isRetracted();
        assertThat(secondAnchor.ctrlIn).isRetracted();

        // ctrl-click to finish
        ctrlClick(314, 314, false);
        assertThat(sp)
                .isFinished()
                .numAnchorsIs(2);

        // Start a new subpath with shift-click.
        // As this is the first point on the subpath, it should not be constrained.
        shiftClick(511, 111);
        assertThat(PenTool.path)
                .isNotNull()
                .numSubPathsIs(2)
                .isConsistent();
        SubPath sp2 = PenTool.path.getActiveSubpath();
        assertThat(sp2)
                .isNotFinished()
                .numAnchorsIs(1)
                .firstAnchorIsAt(511, 111);
    }

    @Test
    public void testStartingWithShiftClick() {
        // starting with shift-click should not cause any exceptions,
        // even tough the last relative coordinates are not yet initialized
        shiftClick(456, 654);

        assertThat(PenTool.path.getActiveSubpath())
                .isNotFinished()
                .numAnchorsIs(1)
                .firstAnchorIsAt(456, 654);
    }

    @Test
    public void testConstrainedCurvedPath() {
        // Start a new curve with shift-press.
        // As this is the first point on the subpath, it should not be constrained.
        shiftPress(314, 314, DRAGGING_THE_CONTROL_OF_LAST);

        // Shift-drag the out control to the right
        // The control should be constrained relative to the anchor.
        shiftDrag(330, 317, DRAGGING_THE_CONTROL_OF_LAST);
        shiftDrag(350, 318, DRAGGING_THE_CONTROL_OF_LAST);
        shiftRelease(350, 318, MOVING_TO_NEXT_ANCHOR);

        // Check the results so far
        assertThat(PenTool.path)
                .isNotNull()
                .numSubPathsIs(1)
                .isConsistent();
        SubPath sp = PenTool.path.getActiveSubpath();
        assertThat(sp)
                .isNotFinished()
                .numAnchorsIs(1);
        AnchorPoint firstAnchor = sp.getAnchor(0);
        assertThat(firstAnchor).isAt(314, 314);
        assertThat(firstAnchor.ctrlOut).isAt(350, 314); // constrained horizontally

        // shift-move the moving point and check that it is constrained
        // relative to the first anchor point (and not relative to its
        // out control, where the mouse was released)
        shiftMove(316, 500, MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .hasMoving()
                .movingIsAt(314, 500);

        // shift-press and check that the constraining is still OK
        shiftPress(316, 510, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp)
                .hasNoMoving()
                .isNotFinished()
                .numAnchorsIs(2);
        AnchorPoint secondAnchor = sp.getAnchor(1);
        assertThat(secondAnchor).isAt(314, 510);
    }

    private void testSpecialDragPrevious(CtrlOrAlt modifier) {
        // click to add the first anchor point
        click(100, 100);
        Path path = PenTool.path;
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(1);
        AnchorPoint firstAnchor = sp.getAnchor(0);

        move(125, 100, MOVING_TO_NEXT_ANCHOR);
        if (modifier == CtrlOrAlt.CTRL) {
            ctrlMove(150, 100, MOVE_EDITING_PREVIOUS);
        } else if (modifier == CtrlOrAlt.ALT) {
            altMove(150, 100, MOVE_EDITING_PREVIOUS);
        }
        move(175, 100, MOVING_TO_NEXT_ANCHOR);

        // click again to add the second anchor point
        click(200, 100);
        assertThat(sp).numAnchorsIs(2);
        AnchorPoint secondAnchor = sp.getAnchor(1);

        move(225, 100, MOVING_TO_NEXT_ANCHOR);
        move(250, 100, MOVING_TO_NEXT_ANCHOR);
        if (modifier == CtrlOrAlt.CTRL) {
            ctrlMove(275, 100, MOVE_EDITING_PREVIOUS);
        } else if (modifier == CtrlOrAlt.ALT) {
            altMove(275, 100, MOVE_EDITING_PREVIOUS);
        }

        // click again to add the third anchor point
        click(300, 100);
        assertThat(sp)
                .numAnchorsIs(3)
                .isNotClosed()
                .isNotFinished();

        // move back towards the second point
        move(275, 100, MOVING_TO_NEXT_ANCHOR);
        move(250, 100, MOVING_TO_NEXT_ANCHOR);

        // now we are close to the second point, it should become active
        if (modifier == CtrlOrAlt.CTRL) {
            ctrlMove(202, 100, MOVE_EDITING_PREVIOUS);
            // the anchor becomes active
            assertThat(secondAnchor).isActive();
        } else if (modifier == CtrlOrAlt.ALT) {
            altMove(202, 100, MOVE_EDITING_PREVIOUS);
            // the control out becomes active
            assertThat(secondAnchor.ctrlOut).isActive();
        }
        // now we are even closer, but release ctrl/alt,
        // so both of them should be inactive
        move(201, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchor).isNotActive();
        assertThat(secondAnchor.ctrlOut).isNotActive();

        if (modifier == CtrlOrAlt.CTRL) {
            // ctrl-press on the second point...
            ctrlPress(200, 100, DRAG_EDITING_PREVIOUS);
            assertThat(secondAnchor).isActive();
        } else if (modifier == CtrlOrAlt.ALT) {
            // alt-press on the second point...
            altPress(200, 100, DRAG_EDITING_PREVIOUS);
            assertThat(secondAnchor.ctrlOut).isActive();
            assertThat(secondAnchor).anchorPointTypeIs(SYMMETRIC);
        }
        // ...and drag it downwards
        drag(200, 150, DRAG_EDITING_PREVIOUS);
        if (modifier == CtrlOrAlt.CTRL) {
            assertThat(secondAnchor).isActive();
        } else if (modifier == CtrlOrAlt.ALT) {
            assertThat(secondAnchor.ctrlOut).isActive();
        }
        release(200, 200, MOVING_TO_NEXT_ANCHOR);
        if (modifier == CtrlOrAlt.CTRL) {
            assertThat(secondAnchor)
                    .isAt(200, 200)
                    .isActive();
        } else if (modifier == CtrlOrAlt.ALT) {
            assertThat(secondAnchor.ctrlOut)
                    .isAt(200, 200)
                    .isActive();
            // check that the in handle was moved up symmetrically...
            assertThat(secondAnchor.ctrlIn).isAt(200, 0);
            // ...and that the anchor didn't move at all
            assertThat(secondAnchor).isAt(200, 100);
        }
        // move away, the point should deactivate
        move(150, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchor).isNotActive();
        assertThat(secondAnchor.ctrlOut).isNotActive();

        // ctrl-click between the points for finish the curve without closing
        ctrlPress(150, 100, NO_INTERACTION);
        release(150, 100, NO_INTERACTION);
        assertThat(sp)
                .numAnchorsIs(3)
                .isNotClosed()
                .isFinished();

        move(120, 100, NO_INTERACTION);
        // ctrl-click on the first point...
        ctrlPress(100, 100, DRAG_EDITING_PREVIOUS);
        // ...and drag it downwards
        drag(100, 150, DRAG_EDITING_PREVIOUS);
        release(100, 200, NO_INTERACTION);
        assertThat(firstAnchor).isAt(100, 200);
    }

    private SubPath build3PointSubPath(boolean closed, boolean curved, int startX, int startY) {
        // first anchor point
        press(startX, startY, DRAGGING_THE_CONTROL_OF_LAST);
        Path path = PenTool.path;
        assertThat(path).isNotNull();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(1);
        AnchorPoint firstAnchor = sp.getAnchor(0);
        assertThat(firstAnchor)
                .isAt(startX, startY)
                .isAtIm(startX, startY);
        if (curved) {
            // curved: drag out the control points before releasing
            drag(startX + 10, startY - 10, DRAGGING_THE_CONTROL_OF_LAST);
            assertThat(firstAnchor.ctrlOut)
                    .isAt(startX + 10, startY - 10)
                    .isAtIm(startX, startY);
            drag(startX + 20, startY - 20, DRAGGING_THE_CONTROL_OF_LAST);
            drag(startX + 30, startY - 30, DRAGGING_THE_CONTROL_OF_LAST);
            assertThat(sp).numAnchorsIs(1);

            // release to set the final value of the control point
            release(startX + 40, startY - 40, MOVING_TO_NEXT_ANCHOR);
            assertThat(sp).numAnchorsIs(1);
            assertThat(firstAnchor.ctrlOut)
                    .isAt(startX + 40, startY - 40)
                    .isAtIm(startX + 40, startY - 40);
            assertThat(firstAnchor.ctrlIn).isNotRetracted();
            assertThat(firstAnchor.ctrlOut).isNotRetracted();
        } else {
            // not curved: release at the click location
            release(startX, startY, MOVING_TO_NEXT_ANCHOR);
            assertThat(firstAnchor.ctrlIn).isRetracted();
            assertThat(firstAnchor.ctrlOut).isRetracted();
        }

        // move towards the second anchor point
        move(startX + 80, startY, MOVING_TO_NEXT_ANCHOR);
        move(startX + 90, startY, MOVING_TO_NEXT_ANCHOR);

        // press to fix the second path point
        int p2x = startX + 100;
        int p2y = startY;
        press(p2x, startY, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp).numAnchorsIs(2);
        AnchorPoint secondAnchorPoint = sp.getAnchor(1);
        assertThat(secondAnchorPoint)
                .isAt(p2x, p2y)
                .isAtIm(p2x, p2y);
        if (curved) {
            // curved: drag out the control points before releasing
            drag(p2x + 10, p2y + 10, DRAGGING_THE_CONTROL_OF_LAST);
            drag(p2x + 20, p2y + 20, DRAGGING_THE_CONTROL_OF_LAST);
            drag(p2x + 30, p2y + 30, DRAGGING_THE_CONTROL_OF_LAST);
            // release to set the final value of the control point
            release(p2x + 30, p2y + 30, MOVING_TO_NEXT_ANCHOR);
            assertThat(secondAnchorPoint.ctrlIn).isNotRetracted();
            assertThat(secondAnchorPoint.ctrlOut).isNotRetracted();
        } else {
            // not curved: release at the click location
            release(p2x, p2y, MOVING_TO_NEXT_ANCHOR);
            assertThat(secondAnchorPoint.ctrlIn).isRetracted();
            assertThat(secondAnchorPoint.ctrlOut).isRetracted();
        }

        // move towards the third anchor point
        move(startX + 70, startY + 100, MOVING_TO_NEXT_ANCHOR);
        move(startX + 60, startY + 100, MOVING_TO_NEXT_ANCHOR);

        // third anchor point
        int p3x = startX + 50;
        int p3y = startY + 100;
        press(p3x, p3y, DRAGGING_THE_CONTROL_OF_LAST);

        if (!closed) {
            release(p3x + 1, p3y + 1, MOVING_TO_NEXT_ANCHOR);

            // if not closed, finish by ctrl-press somewhere
            ctrlPress(p3x + 42, p3y + 42, NO_INTERACTION);
            release(p3x + 42, p3y + 42, NO_INTERACTION);

            assertThat(sp)
                    .isConsistent()
                    .isNotClosed()
                    .isFinished()
                    .firstIsNotActive()
                    .numAnchorsIs(3);
            return sp;
        }

        // from here closed is true, and the curve is closed

        AnchorPoint thirdAnchor = sp.getAnchor(2);
        assertThat(thirdAnchor)
                .isAt(p3x, p3y)
                .isAtIm(p3x, p3y);
        if (curved) {
            // curved: drag out the control points before releasing
            drag(p3x - 10, p3y, DRAGGING_THE_CONTROL_OF_LAST);
            drag(p3x - 20, p3y, DRAGGING_THE_CONTROL_OF_LAST);
            drag(p3x - 30, p3y, DRAGGING_THE_CONTROL_OF_LAST);
            // release to set the final value of the control point
            release(p3x - 40, p3y, MOVING_TO_NEXT_ANCHOR);
        } else {
            // not curved: release at the click location
            release(p3x, p3y, MOVING_TO_NEXT_ANCHOR);
        }

        // move towards the first point
        move(startX, startY + 20, MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .isNotClosed()
                .firstIsNotActive();
        move(startX, startY + 3, MOVING_TO_NEXT_ANCHOR);
        // we are close to the first point, it should become active
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .firstIsActive();
        // click to close
        press(startX, startY, NO_INTERACTION);
        assertThat(sp)
                .isConsistent()
                .isClosed()
                .isFinished()
                .firstIsNotActive()
                .numAnchorsIs(3);
        release(startX, startY, NO_INTERACTION);
        // TODO drag if not curved? what about curved closing?

        return sp;
    }

    private void undoRedo3PointSubpath(SubPath sp, boolean closed) {
        List<String> edits = History.asStringList();
        if (closed) {
            assertThat(sp)
                    .isClosed()
                    .isFinished();
            assertThat(edits).containsExactly(
                    "Subpath Start",
                    "Add Anchor Point",
                    "Add Anchor Point",
                    "Close Subpath");
        } else {
            assertThat(sp)
                    .isNotClosed()
                    .isFinished();
            assertThat(edits).containsExactly(
                    "Subpath Start",
                    "Add Anchor Point",
                    "Add Anchor Point",
                    "Finish Subpath");
        }
        move(0, 0, NO_INTERACTION);

        // now undo until everything is undone
        if (closed) {
            undo("Close Subpath", MOVING_TO_NEXT_ANCHOR);
        } else {
            undo("Finish Subpath", MOVING_TO_NEXT_ANCHOR);
        }
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numAnchorsIs(3);
        // move the mouse to check that the undo left a good state
        move(1, 1, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .numAnchorsIs(2);
        move(2, 2, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numAnchorsIs(1);
        move(3, 3, MOVING_TO_NEXT_ANCHOR);

        undo("Subpath Start", null);
        assertThat(sp)
                .numAnchorsIs(1); // the edit removes the entire subpath
        assertThat(Tools.PEN).hasNoPath();
        move(4, 4, null);

        // now redo until everything is redone
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .numAnchorsIs(1);
        move(5, 5, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .numAnchorsIs(2);
        move(6, 6, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numAnchorsIs(3);
        move(7, 7, MOVING_TO_NEXT_ANCHOR);

        if (closed) {
            redo("Close Subpath", NO_INTERACTION);
            assertThat(sp)
                    .isClosed()
                    .isFinished()
                    .numAnchorsIs(3);
            move(8, 8, NO_INTERACTION);
        } else {
            redo("Finish Subpath", NO_INTERACTION);
            assertThat(sp)
                    .isNotClosed()
                    .isFinished()
                    .numAnchorsIs(3);
            move(8, 8, NO_INTERACTION);
        }
    }

    private void undoRedoMultipleSubpaths() {
        // undo and redo everything, adding mouse movements to
        // check that the state left bt undo/redo is OK
        assertThat(History.asStringList()).containsExactly(
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

        undo("Subpath Start", NO_INTERACTION);
        move(4, 4, NO_INTERACTION);

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

        redo("Finish Subpath", NO_INTERACTION);
        move(12, 12, NO_INTERACTION);

        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        move(13, 13, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(14, 14, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(15, 15, MOVING_TO_NEXT_ANCHOR);

        redo("Close Subpath", NO_INTERACTION);
        move(16, 16, NO_INTERACTION);
    }

    private void undo(String edit, BuildState state) {
        History.assertEditToBeUndoneNameIs(edit);
        History.undo();
        checkState(state);
        if (PenTool.path != null) {
            PenTool.path.checkConsistency();
        }
    }

    private void redo(String edit, BuildState state) {
        History.assertEditToBeRedoneNameIs(edit);
        History.redo();
        checkState(state);
        if (PenTool.path != null) {
            PenTool.path.checkConsistency();
        }
    }

    private void press(int x, int y, BuildState state) {
        press(x, y, Ctrl.NO, Alt.NO, Shift.NO, state);
    }

    private void ctrlPress(int x, int y, BuildState state) {
        press(x, y, Ctrl.YES, Alt.NO, Shift.NO, state);
    }

    private void altPress(int x, int y, BuildState state) {
        press(x, y, Ctrl.NO, Alt.YES, Shift.NO, state);
    }

    private void shiftPress(int x, int y, BuildState state) {
        press(x, y, Ctrl.NO, Alt.NO, Shift.YES, state);
    }

    private void press(int x, int y, Ctrl ctrl, Alt alt, Shift shift, BuildState state) {
        // go through the event dispatcher
        // because the undo uses its "mouseDown" state
        TestHelper.press(x, y, ctrl, alt, shift, ic);
        checkState(state);
        pb.paint(g);
    }

    private void click(int x, int y) {
        press(x, y, DRAGGING_THE_CONTROL_OF_LAST);
        release(x, y, MOVING_TO_NEXT_ANCHOR);
    }

    private void shiftClick(int x, int y) {
        shiftPress(x, y, DRAGGING_THE_CONTROL_OF_LAST);
        shiftRelease(x, y, MOVING_TO_NEXT_ANCHOR);
    }

    private void ctrlClick(int x, int y, boolean isFirstInSubPath) {
        if (isFirstInSubPath) {
            ctrlPress(x, y, DRAGGING_THE_CONTROL_OF_LAST);
            ctrlRelease(x, y, MOVING_TO_NEXT_ANCHOR);
        } else {
            ctrlPress(x, y, NO_INTERACTION);
            ctrlRelease(x, y, NO_INTERACTION);
        }
    }

    private void drag(int x, int y, BuildState state) {
        drag(x, y, Ctrl.NO, Alt.NO, Shift.NO, state);
    }

    private void altDrag(int x, int y, BuildState state) {
        drag(x, y, Ctrl.NO, Alt.YES, Shift.NO, state);
    }

    private void shiftDrag(int x, int y, BuildState state) {
        drag(x, y, Ctrl.NO, Alt.NO, Shift.YES, state);
    }

    private void drag(int x, int y, Ctrl ctrl, Alt alt, Shift shift, BuildState state) {
        TestHelper.drag(x, y, ctrl, alt, shift, ic);
        checkState(state);
        pb.paint(g);
    }

    private void release(int x, int y, BuildState state) {
        release(x, y, Ctrl.NO, Alt.NO, Shift.NO, state);
    }

    private void shiftRelease(int x, int y, BuildState state) {
        release(x, y, Ctrl.NO, Alt.NO, Shift.YES, state);
    }

    private void ctrlRelease(int x, int y, BuildState state) {
        release(x, y, Ctrl.YES, Alt.NO, Shift.NO, state);
    }

    private void release(int x, int y, Ctrl ctrl, Alt alt, Shift shift, BuildState state) {
        TestHelper.release(x, y, ctrl, alt, shift, ic);
        checkState(state);
        pb.paint(g);
    }

    private void move(int x, int y, BuildState state) {
        move(x, y, Ctrl.NO, Alt.NO, Shift.NO, state);
    }

    private void ctrlMove(int x, int y, BuildState state) {
        move(x, y, Ctrl.YES, Alt.NO, Shift.NO, state);
    }

    private void altMove(int x, int y, BuildState state) {
        move(x, y, Ctrl.NO, Alt.YES, Shift.NO, state);
    }

    private void shiftMove(int x, int y, BuildState state) {
        move(x, y, Ctrl.NO, Alt.NO, Shift.YES, state);
    }

    private void move(int x, int y, Ctrl ctrl, Alt alt, Shift shift, BuildState state) {
        TestHelper.move(x, y, ctrl, alt, shift, ic);
        checkState(state);
        pb.paint(g);
    }

    @SuppressWarnings("MethodMayBeStatic")
    private void checkState(BuildState expected) {
        if (expected == null) {
            assert PenTool.path == null;
        } else {
            PenTool.path.assertStateIs(expected);
        }
    }
}