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
import pixelitor.tools.util.PMouseEvent;

import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static org.mockito.Mockito.mock;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.tools.pen.PathBuilder.State.BEFORE_SUBPATH;
import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_ANCHOR;

public class PathBuilderTest {
    private ImageComponent ic;
    private Composition comp;

    @BeforeClass
    public static void setupClass() {
        Build.setTestingMode();
    }

    @Before
    public void setup() {
        comp = mock(Composition.class);
        ic = TestHelper.setupAMockICFor(comp);

        History.clear();
    }

    @Test
    public void testBuilding3PointClosedPath() {
        Graphics2D g = mock(Graphics2D.class);

        PathBuilder pb = PenToolMode.BUILD;
        pb.assertStateIs(BEFORE_SUBPATH);

        // start the curve
        pb.mousePressed(createPMouseEvent(10, 10));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        Path path = pb.getPath();
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numPointsIs(1);
        AnchorPoint firstAnchorPoint = sp.getPoint(0);
        assertThat(firstAnchorPoint)
                .isAt(10, 10)
                .isAtIm(10, 10);
        pb.paint(g);

        // drag towards right
        pb.mouseDragged(createPMouseEvent(20, 10));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(firstAnchorPoint.ctrlOut)
                .isAt(20, 10)
                .isAtIm(10, 10);

        pb.paint(g);
        pb.mouseDragged(createPMouseEvent(30, 10));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        pb.paint(g);
        pb.mouseDragged(createPMouseEvent(40, 10));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp).numPointsIs(1);
        pb.paint(g);

        // release to set the final value of the control point
        pb.mouseReleased(createPMouseEvent(50, 10));
        pb.assertStateIs(MOVING_TO_NEXT_ANCHOR);
        assertThat(sp).numPointsIs(1);
        pb.paint(g);
        assertThat(firstAnchorPoint.ctrlOut)
                .isAt(50, 10)
                .isAtIm(50, 10);

        // move down towards the second path point
        pb.mouseMoved(createMouseEvent(50, 20), ic);
        pb.assertStateIs(MOVING_TO_NEXT_ANCHOR);
        pb.paint(g);
        pb.mouseMoved(createMouseEvent(50, 30), ic);
        pb.assertStateIs(MOVING_TO_NEXT_ANCHOR);
        pb.paint(g);
        pb.mouseMoved(createMouseEvent(50, 40), ic);
        pb.assertStateIs(MOVING_TO_NEXT_ANCHOR);
        assertThat(sp).numPointsIs(1);
        pb.paint(g);

        // press to fix the second path point
        pb.mousePressed(createPMouseEvent(50, 50));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp).numPointsIs(2);
        pb.paint(g);
        AnchorPoint secondPoint = sp.getPoint(1);
        assertThat(secondPoint)
                .isAt(50, 50)
                .isAtIm(50, 50);

        // drag towards right to drag out the control points
        pb.mouseDragged(createPMouseEvent(60, 50));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        pb.paint(g);
        pb.mouseDragged(createPMouseEvent(70, 50));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        pb.paint(g);
        pb.mouseDragged(createPMouseEvent(80, 50));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        assertThat(sp).numPointsIs(2);
        pb.paint(g);

        // release to fix the control points
        pb.mouseReleased(createPMouseEvent(90, 50));
        pb.assertStateIs(MOVING_TO_NEXT_ANCHOR);
        assertThat(sp).numPointsIs(2);
        pb.paint(g);
        assertThat(secondPoint.ctrlOut)
                .isAt(90, 50)
                .isAtIm(90, 50);

        // move up towards the next path point
        pb.mouseMoved(createMouseEvent(90, 40), ic);
        pb.paint(g);
        pb.mouseMoved(createMouseEvent(90, 30), ic);
        pb.paint(g);
        pb.mouseMoved(createMouseEvent(90, 20), ic);
        assertThat(sp).numPointsIs(2);
        pb.paint(g);

        // press to finalize the third path point at 90, 10
        pb.mousePressed(createPMouseEvent(90, 10));
        assertThat(sp).numPointsIs(3);
        AnchorPoint thirdPoint = sp.getPoint(2);
        assertThat(thirdPoint)
                .isAt(90, 10)
                .isAtIm(90, 10);

        // drag to the right to finalize the control points of the third point
        pb.mouseDragged(createPMouseEvent(100, 10));
        pb.mouseDragged(createPMouseEvent(110, 10));
        pb.mouseDragged(createPMouseEvent(120, 10));
        pb.mouseReleased(createPMouseEvent(120, 10)); // finalized
        assertThat(sp).numPointsIs(3);
        assertThat(thirdPoint.ctrlOut)
                .isAt(120, 10)
                .isAtIm(120, 10);

        // move towards the starting point (10, 10)
        // in order to close
        assertThat(sp)
                .isNotClosed()
                .firstIsNotActive();
        pb.mouseMoved(createMouseEvent(100, 10), ic);
        pb.mouseMoved(createMouseEvent(50, 10), ic);
        assertThat(sp)
                .isNotClosed()
                .firstIsNotActive();
        pb.mouseMoved(createMouseEvent(12, 10), ic);
        // we are close, the first should become active
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .firstIsActive();
        // now close it
        pb.mousePressed(createPMouseEvent(11, 10));
        assertThat(sp)
                .isClosed()
                .isFinished()
                .firstIsNotActive()
                .numPointsIs(3);

        assertThat(History.asStringList())
                .containsExactly(
                        "Subpath Start",
                        "Add Anchor Point",
                        "Add Anchor Point",
                        "Close Subpath");

        // now undo until everything is undone
        History.assertEditToBeUndoneNameIs("Close Subpath");
        History.undo();
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numPointsIs(3);
        // move the mouse to check that the undo left a good state
        pb.mouseMoved(createMouseEvent(1, 1), ic);

        History.assertEditToBeUndoneNameIs("Add Anchor Point");
        History.undo();
        assertThat(sp)
                .numPointsIs(2);
        pb.mouseMoved(createMouseEvent(2, 2), ic);

        History.assertEditToBeUndoneNameIs("Add Anchor Point");
        History.undo();
        assertThat(sp)
                .numPointsIs(1);
        pb.mouseMoved(createMouseEvent(3, 3), ic);

        History.assertEditToBeUndoneNameIs("Subpath Start");
        History.undo();
        assertThat(sp)
                .numPointsIs(1); // the edit removes the entire subpath
        pb.mouseMoved(createMouseEvent(4, 4), ic);

        // now redo until everything is redone
        History.assertEditToBeRedoneNameIs("Subpath Start");
        History.redo();
        assertThat(sp)
                .numPointsIs(1);
        pb.mouseMoved(createMouseEvent(5, 5), ic);

        History.assertEditToBeRedoneNameIs("Add Anchor Point");
        History.redo();
        assertThat(sp)
                .numPointsIs(2);
        pb.mouseMoved(createMouseEvent(6, 6), ic);

        History.assertEditToBeRedoneNameIs("Add Anchor Point");
        History.redo();
        assertThat(sp)
                .isNotClosed()
                .isNotFinished()
                .numPointsIs(3);
        pb.mouseMoved(createMouseEvent(7, 7), ic);

        History.assertEditToBeRedoneNameIs("Close Subpath");
        History.redo();
        assertThat(sp)
                .isClosed()
                .isFinished()
                .numPointsIs(3);
        pb.mouseMoved(createMouseEvent(8, 8), ic);
    }

    @Test
    public void testBuildingMultipleSubPaths() {
        Graphics2D g = mock(Graphics2D.class);

        PathBuilder pb = PenToolMode.BUILD;
        pb.assertStateIs(BEFORE_SUBPATH);

        // start the curve with a click
        pb.mousePressed(createPMouseEvent(10, 10));
        pb.assertStateIs(DRAGGING_THE_CONTROL_OF_LAST);
        Path path = pb.getPath();
        assertThat(path).isNotNull();
        pb.mouseReleased(createPMouseEvent(10, 10));

        pb.mouseMoved(createMouseEvent(15, 10), ic);
        pb.mouseMoved(createMouseEvent(25, 10), ic);

        // click again to create a straight line
        pb.mousePressed(createPMouseEvent(30, 10));
        pb.mouseReleased(createPMouseEvent(30, 10));

        pb.mouseMoved(createMouseEvent(40, 10), ic);
        pb.mouseMoved(createMouseEvent(50, 10), ic);

        // ctrl-click to finish with another straight line
        pb.mousePressed(createCtrlPMouseEvent(60, 10));
        pb.mouseReleased(createPMouseEvent(60, 10));

        // start new subpath with another click
        pb.mousePressed(createCtrlPMouseEvent(60, 20));
        pb.mouseReleased(createPMouseEvent(60, 20));

        pb.mouseMoved(createMouseEvent(50, 20), ic);
        pb.mouseMoved(createMouseEvent(40, 20), ic);

        // ctrl-click to finish with another straight line
        pb.mousePressed(createCtrlPMouseEvent(30, 20));
        pb.mouseReleased(createPMouseEvent(30, 20));

        // undo and redo everything, adding mouse movements to
        // check that the state left bt undo/redo is OK
        assertThat(History.asStringList()).containsExactly(
                "Subpath Start",
                "Add Anchor Point",
                "Add Anchor Point",
                "Subpath Start",
                "Add Anchor Point");

        History.assertEditToBeUndoneNameIs("Add Anchor Point");
        History.undo();
        pb.mouseMoved(createMouseEvent(1, 1), ic);

        History.assertEditToBeUndoneNameIs("Subpath Start");
        History.undo();
        pb.mouseMoved(createMouseEvent(2, 2), ic);

        History.assertEditToBeUndoneNameIs("Add Anchor Point");
        History.undo();
        pb.mouseMoved(createMouseEvent(3, 3), ic);

        History.assertEditToBeUndoneNameIs("Add Anchor Point");
        History.undo();
        pb.mouseMoved(createMouseEvent(4, 4), ic);

        History.assertEditToBeUndoneNameIs("Subpath Start");
        History.undo();
        pb.mouseMoved(createMouseEvent(5, 5), ic);

        // now redo everything
        History.assertEditToBeRedoneNameIs("Subpath Start");
        History.redo();
        pb.mouseMoved(createMouseEvent(6, 6), ic);

        History.assertEditToBeRedoneNameIs("Add Anchor Point");
        History.redo();
        pb.mouseMoved(createMouseEvent(7, 7), ic);

        History.assertEditToBeRedoneNameIs("Add Anchor Point");
        History.redo();
        pb.mouseMoved(createMouseEvent(8, 8), ic);

        History.assertEditToBeRedoneNameIs("Subpath Start");
        History.redo();
        pb.mouseMoved(createMouseEvent(9, 9), ic);

        History.assertEditToBeRedoneNameIs("Add Anchor Point");
        History.redo();
        pb.mouseMoved(createMouseEvent(10, 10), ic);
    }

    private PMouseEvent createPMouseEvent(int x, int y) {
        MouseEvent me = createMouseEvent(x, y, false);
        PMouseEvent pe = new PMouseEvent(me, ic);
        return pe;
    }

    private PMouseEvent createCtrlPMouseEvent(int x, int y) {
        MouseEvent me = createMouseEvent(x, y, true);
        PMouseEvent pe = new PMouseEvent(me, ic);
        return pe;
    }

    private MouseEvent createMouseEvent(int x, int y) {
        return createMouseEvent(x, y, false);
    }

    private MouseEvent createMouseEvent(int x, int y, boolean ctrl) {
        int modifiers = 0;
        if (ctrl) {
            modifiers |= InputEvent.CTRL_MASK;
        }
        return new MouseEvent(ic, 0, 0, modifiers, x, y, 1, false);
    }
}