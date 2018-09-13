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
import pixelitor.tools.Tools;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.List;

import static org.mockito.Mockito.mock;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.PathBuilder.State.BEFORE_SUBPATH;
import static pixelitor.tools.pen.PathBuilder.State.CTRL_DRAGGING_PREVIOUS;
import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_ANCHOR;

public class PathBuilderTest {
    private ImageComponent ic;
    private Graphics2D g;
    private PathBuilder pb;

    @BeforeClass
    public static void setupClass() {
        Build.setTestingMode();
        Tools.changeTo(Tools.PEN);
    }

    @Before
    public void setup() {
        Composition comp = mock(Composition.class);
        ic = TestHelper.setupAMockICFor(comp);
        g = mock(Graphics2D.class);
        pb = PenToolMode.BUILD;

        // reset the state between the tests
        pb.setPath(null, "test setup");
        pb.setState(BEFORE_SUBPATH, "test setup");
        History.clear();
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
        Path path = pb.getPath();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numPointsIs(1);

        undo("Subpath Start", BEFORE_SUBPATH);
        assertThat(pb.getPath()).isNull();

        drag(110, 100, BEFORE_SUBPATH);

        // dragging state because the mouse is down
        redo("Subpath Start", DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(pb.getPath() == path).isTrue();

        undo("Subpath Start", BEFORE_SUBPATH);
        assertThat(pb.getPath()).isNull();

        // the difference is that this time the mouse is released after the undo
        release(100, 100, BEFORE_SUBPATH);

        // moving state because the mouse is up
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(pb.getPath() == path).isTrue();
    }

    @Test
    public void testUndoAfterMouseClicked() {
        press(100, 100, DRAGGING_THE_CONTROL_OF_LAST);
        release(100, 100, MOVING_TO_NEXT_ANCHOR);

        Path path = pb.getPath();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numPointsIs(1);

        undo("Subpath Start", BEFORE_SUBPATH);
        assertThat(pb.getPath()).isNull();

        move(110, 100, BEFORE_SUBPATH);

        // moving state because the mouse is up
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(pb.getPath() == path).isTrue();

        undo("Subpath Start", BEFORE_SUBPATH);
        assertThat(pb.getPath()).isNull();

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
        Path path = pb.getPath();
        assertThat(path).isNotNull();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numPointsIs(1);
        AnchorPoint firstAnchorPoint = sp.getPoint(0);
        assertThat(firstAnchorPoint)
                .anchorPointTypeIs(SYMMETRIC)
                .isAt(p1x, p1y);

        // drag the "out" handle in the opposite direction of the desired "in" control
        drag(p1x + 25, p1y + 25, DRAGGING_THE_CONTROL_OF_LAST);
        drag(p1x + 50, p1y + 50, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(firstAnchorPoint.ctrlOut).isAt(p1x + 50, p1y + 50);
        assertThat(firstAnchorPoint.ctrlIn).isAt(p1x - 50, p1y - 50);

        // alt-drag the "out" handle to its place
        altDrag(p1x + 50, p1y, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(firstAnchorPoint).anchorPointTypeIs(CUSP);
        altDrag(p1x + 50, p1y - 50, DRAGGING_THE_CONTROL_OF_LAST);
        release(p1x + 50, p1y - 50, MOVING_TO_NEXT_ANCHOR);
        assertThat(firstAnchorPoint.ctrlOut).isAt(p1x + 50, p1y - 50);
        // since alt breaks the handles, the "in" handle is not supposed to move
        assertThat(firstAnchorPoint.ctrlIn).isAt(p1x - 50, p1y - 50);

        // move the mouse to the second anchor point
        int p2x = 100;
        int p2y = 200;
        move(p2x, p2y, MOVING_TO_NEXT_ANCHOR);
        // press to fix the second anchor point
        press(p2x, p2y, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp).numPointsIs(2);
        AnchorPoint secondAnchorPoint = sp.getPoint(1);
        assertThat(secondAnchorPoint)
                .anchorPointTypeIs(SYMMETRIC)
                .isAt(p2x, p2y);

        // drag in the opposite direction of the desired "in" control
        drag(p2x - 25, p2y + 25, DRAGGING_THE_CONTROL_OF_LAST);
        drag(p2x - 50, p2y + 50, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(secondAnchorPoint.ctrlOut).isAt(p2x - 50, p2y + 50);
        assertThat(secondAnchorPoint.ctrlIn).isAt(p2x + 50, p2y - 50);

        // alt-drag the "out" handle to its place
        altDrag(p2x - 50, p2y - 25, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(secondAnchorPoint).anchorPointTypeIs(CUSP);
        altDrag(p2x - 50, p2y - 50, DRAGGING_THE_CONTROL_OF_LAST);
        release(p2x - 50, p2y - 50, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchorPoint.ctrlOut).isAt(p2x - 50, p2y - 50);
        // since alt breaks the handles, the "in" handle is not supposed to move
        assertThat(secondAnchorPoint.ctrlIn).isAt(p2x + 50, p2y - 50);

        // move back to the first anchor point
        move(p1x, p1y, MOVING_TO_NEXT_ANCHOR);

        // press to close
        press(p1x, p1y, BEFORE_SUBPATH);
        assertThat(sp)
                .numPointsIs(2)
                .isClosed()
                .isFinished();

        // releasing should not change anything
        release(p1x, p1y, BEFORE_SUBPATH);
        assertThat(sp)
                .numPointsIs(2)
                .isClosed()
                .isFinished();
        assertThat(path)
                .numSubPathsIs(1)
                .activeSubPathIs(sp);
    }

    @Test
    public void testMovingPreviousAnchorsWithCtrl() {
        // click
        press(100, 100, DRAGGING_THE_CONTROL_OF_LAST);
        release(100, 100, MOVING_TO_NEXT_ANCHOR);
        Path path = pb.getPath();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numPointsIs(1);
        AnchorPoint firstAnchorPoint = sp.getPoint(0);

        move(150, 100, MOVING_TO_NEXT_ANCHOR);

        // click again
        press(200, 100, DRAGGING_THE_CONTROL_OF_LAST);
        release(200, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(sp).numPointsIs(2);
        AnchorPoint secondAnchorPoint = sp.getPoint(1);

        move(250, 100, MOVING_TO_NEXT_ANCHOR);

        // click a third time
        press(300, 100, DRAGGING_THE_CONTROL_OF_LAST);
        release(300, 100, MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .numPointsIs(3)
                .isNotClosed()
                .isNotFinished();

        move(250, 100, MOVING_TO_NEXT_ANCHOR);
        move(200, 100, MOVING_TO_NEXT_ANCHOR);
        // ctrl-click on the second point...
        ctrlPress(200, 100, CTRL_DRAGGING_PREVIOUS);
        // ...and drag it downwards
        drag(200, 150, CTRL_DRAGGING_PREVIOUS);
        release(200, 200, MOVING_TO_NEXT_ANCHOR);
        assertThat(secondAnchorPoint).isAt(200, 200);

        move(150, 100, MOVING_TO_NEXT_ANCHOR);
        // ctrl-click between the points for finish the curve without closing
        ctrlPress(150, 100, BEFORE_SUBPATH);
        release(150, 100, BEFORE_SUBPATH);
        assertThat(sp)
                .numPointsIs(3)
                .isNotClosed()
                .isFinished();

        move(120, 100, BEFORE_SUBPATH);
        // ctrl-click on the first point...
        ctrlPress(100, 100, CTRL_DRAGGING_PREVIOUS);
        // ...and drag it downwards
        drag(100, 150, CTRL_DRAGGING_PREVIOUS);
        // this time expect the BEFORE_SUBPATH state
        release(100, 200, BEFORE_SUBPATH);
        assertThat(firstAnchorPoint).isAt(100, 200);
    }

    private SubPath build3PointSubPath(boolean closed, boolean curved, int startX, int startY) {
        // first anchor point
        press(startX, startY, DRAGGING_THE_CONTROL_OF_LAST);
        Path path = pb.getPath();
        assertThat(path).isNotNull();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numPointsIs(1);
        AnchorPoint firstAnchorPoint = sp.getPoint(0);
        assertThat(firstAnchorPoint)
                .isAt(startX, startY)
                .isAtIm(startX, startY);
        if (curved) {
            // curved: drag out the control points before releasing
            drag(startX + 10, startY - 10, DRAGGING_THE_CONTROL_OF_LAST);
            assertThat(firstAnchorPoint.ctrlOut)
                    .isAt(startX + 10, startY - 10)
                    .isAtIm(startX, startY);
            drag(startX + 20, startY - 20, DRAGGING_THE_CONTROL_OF_LAST);
            drag(startX + 30, startY - 30, DRAGGING_THE_CONTROL_OF_LAST);
            assertThat(sp).numPointsIs(1);

            // release to set the final value of the control point
            release(startX + 40, startY - 40, MOVING_TO_NEXT_ANCHOR);
            assertThat(sp).numPointsIs(1);
            assertThat(firstAnchorPoint.ctrlOut)
                    .isAt(startX + 40, startY - 40)
                    .isAtIm(startX + 40, startY - 40);
            assertThat(firstAnchorPoint.ctrlIn).isNotRetracted();
            assertThat(firstAnchorPoint.ctrlOut).isNotRetracted();
        } else {
            // not curved: release at the click location
            release(startX, startY, MOVING_TO_NEXT_ANCHOR);
            assertThat(firstAnchorPoint.ctrlIn).isRetracted();
            assertThat(firstAnchorPoint.ctrlOut).isRetracted();
        }

        // move towards the second anchor point
        move(startX + 80, startY, MOVING_TO_NEXT_ANCHOR);
        move(startX + 90, startY, MOVING_TO_NEXT_ANCHOR);

        // press to fix the second path point
        int p2x = startX + 100;
        int p2y = startY;
        press(p2x, startY, DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp).numPointsIs(2);
        AnchorPoint secondAnchorPoint = sp.getPoint(1);
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
            ctrlPress(p3x + 42, p3y + 42, BEFORE_SUBPATH);
        }
        AnchorPoint thirdAnchorPoint = sp.getPoint(2);
        assertThat(thirdAnchorPoint)
                .isAt(p3x, p3y)
                .isAtIm(p3x, p3y);
        if (curved && closed) {
            // curved: drag out the control points before releasing
            drag(p3x - 10, p3y, DRAGGING_THE_CONTROL_OF_LAST);
            drag(p3x - 20, p3y, DRAGGING_THE_CONTROL_OF_LAST);
            drag(p3x - 30, p3y, DRAGGING_THE_CONTROL_OF_LAST);
            // release to set the final value of the control point
            release(p3x - 40, p3y, MOVING_TO_NEXT_ANCHOR);
        } else {
            // not curved: release at the click location
            release(p3x, p3y,
                    closed ? MOVING_TO_NEXT_ANCHOR : BEFORE_SUBPATH);
        }

        if (closed) {
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
            press(startX, startY, BEFORE_SUBPATH);
            assertThat(sp)
                    .isClosed()
                    .isFinished()
                    .firstIsNotActive()
                    .numPointsIs(3);
            release(startX, startY, BEFORE_SUBPATH);
            // TODO drag if not curved? what about curved closing?
        }

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
        move(0, 0, BEFORE_SUBPATH);

        // now undo until everything is undone
        if (closed) {
            undo("Close Subpath", MOVING_TO_NEXT_ANCHOR);
        } else {
            undo("Finish Subpath", MOVING_TO_NEXT_ANCHOR);
        }
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numPointsIs(3);
        // move the mouse to check that the undo left a good state
        move(1, 1, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .numPointsIs(2);
        move(2, 2, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numPointsIs(1);
        move(3, 3, MOVING_TO_NEXT_ANCHOR);

        undo("Subpath Start", BEFORE_SUBPATH);
        assertThat(sp)
                .numPointsIs(1); // the edit removes the entire subpath
        move(4, 4, BEFORE_SUBPATH);

        // now redo until everything is redone
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .numPointsIs(1);
        move(5, 5, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .numPointsIs(2);
        move(6, 6, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numPointsIs(3);
        move(7, 7, MOVING_TO_NEXT_ANCHOR);

        if (closed) {
            redo("Close Subpath", BEFORE_SUBPATH);
            assertThat(sp)
                    .isClosed()
                    .isFinished()
                    .numPointsIs(3);
            move(8, 8, BEFORE_SUBPATH);
        } else {
            redo("Finish Subpath", BEFORE_SUBPATH);
            assertThat(sp)
                    .isNotClosed()
                    .isFinished()
                    .numPointsIs(3);
            move(8, 8, BEFORE_SUBPATH);
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

        undo("Subpath Start", BEFORE_SUBPATH);
        move(4, 4, BEFORE_SUBPATH);

        undo("Finish Subpath", MOVING_TO_NEXT_ANCHOR);
        move(5, 5, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(6, 6, MOVING_TO_NEXT_ANCHOR);

        undo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(7, 7, MOVING_TO_NEXT_ANCHOR);

        undo("Subpath Start", BEFORE_SUBPATH);
        move(8, 8, BEFORE_SUBPATH);

        // now redo everything
        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        move(9, 9, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(10, 10, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(11, 11, MOVING_TO_NEXT_ANCHOR);

        redo("Finish Subpath", BEFORE_SUBPATH);
        move(12, 12, BEFORE_SUBPATH);

        redo("Subpath Start", MOVING_TO_NEXT_ANCHOR);
        move(13, 13, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(14, 14, MOVING_TO_NEXT_ANCHOR);

        redo("Add Anchor Point", MOVING_TO_NEXT_ANCHOR);
        move(15, 15, MOVING_TO_NEXT_ANCHOR);

        redo("Close Subpath", BEFORE_SUBPATH);
        move(16, 16, BEFORE_SUBPATH);
    }

    private void undo(String edit, PathBuilder.State state) {
        History.assertEditToBeUndoneNameIs(edit);
        History.undo();
        pb.assertStateIs(state);
    }

    private void redo(String edit, PathBuilder.State state) {
        History.assertEditToBeRedoneNameIs(edit);
        History.redo();
        pb.assertStateIs(state);
    }

    private void press(int x, int y, PathBuilder.State state) {
        // go through the event dispatcher
        // because the undo uses its "mouseDown" state
        Tools.EventDispatcher.mousePressed(createMouseEvent(x, y, Ctrl.NO, Alt.NO), ic);
        pb.assertStateIs(state);
        pb.paint(g);
    }

    private void ctrlPress(int x, int y, PathBuilder.State state) {
        Tools.EventDispatcher.mousePressed(createMouseEvent(x, y, Ctrl.YES, Alt.NO), ic);
        pb.assertStateIs(state);
        pb.paint(g);
    }

    private void drag(int x, int y, PathBuilder.State state) {
        Tools.EventDispatcher.mouseDragged(createMouseEvent(x, y, Ctrl.NO, Alt.NO), ic);
        pb.assertStateIs(state);
        pb.paint(g);
    }

    private void altDrag(int x, int y, PathBuilder.State state) {
        Tools.EventDispatcher.mouseDragged(createMouseEvent(x, y, Ctrl.NO, Alt.YES), ic);
        pb.assertStateIs(state);
        pb.paint(g);
    }

    private void release(int x, int y, PathBuilder.State state) {
        Tools.EventDispatcher.mouseReleased(createMouseEvent(x, y, Ctrl.NO, Alt.NO), ic);
        pb.assertStateIs(state);
        pb.paint(g);
    }

    private void move(int x, int y, PathBuilder.State state) {
        Tools.EventDispatcher.mouseMoved(createMouseEvent(x, y, Ctrl.NO, Alt.NO), ic);
        pb.assertStateIs(state);
        pb.paint(g);
    }

    private MouseEvent createMouseEvent(int x, int y, Ctrl ctrl, Alt alt) {
        int modifiers = 0;
        modifiers = ctrl.modify(modifiers);
        modifiers = alt.modify(modifiers);
        //noinspection MagicConstant
        return new MouseEvent(ic, 0, 0, modifiers, x, y, 1, false);
    }
}