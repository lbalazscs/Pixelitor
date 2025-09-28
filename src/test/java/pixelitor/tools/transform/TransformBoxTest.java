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

package pixelitor.tools.transform;

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.View;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.ColorFillLayer;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.input.Modifiers;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;
import static java.awt.event.MouseEvent.MOUSE_RELEASED;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.utils.AngleUnit.INTUITIVE_DEGREES;

@DisplayName("TransformBox tests")
@TestMethodOrder(MethodOrderer.Random.class)
class TransformBoxTest {
    private final Rectangle originalRect = new Rectangle(200, 100, 200, 100);
    private View view;
    private static final Transformable DUMMY_TRANSFORMABLE = new Transformable() {
        @Override
        public void imTransform(AffineTransform transform) {
            // do nothing
        }

        @Override
        public void updateUI(View view) {
            // do nothing
        }

        @Override
        public void prepareForTransform() {
            // do nothing
        }

        @Override
        public PixelitorEdit finalizeTransform() {
            // do nothing
            return null;
        }

        @Override
        public void cancelTransform() {
            // do nothing
        }

        @Override
        public DebugNode createDebugNode(String key) {
            return new DebugNode(key, null);
        }
    };

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        var comp = TestHelper.createRealComp("TransformBoxTest", ColorFillLayer.class);
        view = comp.getView();
    }

    @Test
    void moveNWFromInitialState() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        CornerHandle nw = box.getNW();
        CornerHandle sw = box.getSW();
        CornerHandle ne = box.getNE();
        CornerHandle se = box.getSE();

        checkOriginalHandleState(box);

        nw.mousePressed(202, 100);
        nw.mouseDragged(222, 100); // dragged 20 pixels horizontally to the right

        // check that both NW and SW moved
        assertThat(nw).isAt(220, 100);
        assertThat(sw).isAt(220, 200);
        // and the other two didn't move
        assertThat(ne).isAt(400, 100);
        assertThat(se).isAt(400, 200);

        // drag additional 20 pixels to the right, but also 10 pixels down
        nw.mouseDragged(242, 110);
        nw.mouseReleased(242, 110);

        // check that NW followed the mouse
        assertThat(nw).isAt(240, 110);
        // and SW moved only horizontally
        assertThat(sw).isAt(240, 200);
        // and NE moved only vertically
        assertThat(ne).isAt(400, 110);
        // and SE didn't move at all
        assertThat(se).isAt(400, 200);

        // check that the transform behaves like the handles
        checkRectangleTransform(box.calcImTransform(),
            new Rectangle(200, 100, 200, 100),
            new Rectangle(240, 110, 160, 90));
    }

    @Test
    void moveSEFromInitialState() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        CornerHandle nw = box.getNW();
        CornerHandle sw = box.getSW();
        CornerHandle ne = box.getNE();
        CornerHandle se = box.getSE();

        checkOriginalHandleState(box);

        se.mousePressed(402, 202);
        se.mouseDragged(382, 202); // dragged 20 pixels horizontally to the left

        // check that both SE and NE moved
        assertThat(se).isAt(380, 200);
        assertThat(ne).isAt(380, 100);
        // and the other two didn't move
        assertThat(nw).isAt(200, 100);
        assertThat(sw).isAt(200, 200);

        // drag additional 20 pixels to the left, but also 10 pixels up
        se.mouseDragged(362, 192);
        se.mouseReleased(362, 192);

        // check that SE followed the mouse
        assertThat(se).isAt(360, 190);
        // and NE moved only horizontally
        assertThat(ne).isAt(360, 100);
        // and SW moved only vertically
        assertThat(sw).isAt(200, 190);
        // and NW didn't move at all
        assertThat(nw).isAt(200, 100);

        // check that the transform behaves like the handles
        checkRectangleTransform(box.calcImTransform(),
            new Rectangle(200, 100, 200, 100),
            new Rectangle(200, 100, 160, 90));
    }

    @Test
    void pureTranslation() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        // translate NW by 30, 20
        press(box, 200, 100);
        drag(box, 210, 110);
        drag(box, 220, 115);
        release(box, 230, 120);

        // also translate SE by the same amount (30, 20)
        press(box, 400, 200);
        drag(box, 415, 210);
        drag(box, 430, 220);
        release(box, 430, 220);

        var at = box.calcImTransform();
        // should be a pure (30, 20) translation and nothing else
        Assertions.assertEquals(AffineTransform.TYPE_TRANSLATION, at.getType());
        checkTransform(at, 100, 100, 130, 120);
    }

    @Test
    void dragWholeBox() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        // press inside the box, e.g., at the center
        press(box, 300, 150);
        // drag by (30, 50)
        drag(box, 330, 200);
        release(box, 330, 200);

        // check that all handles have moved by the same delta
        assertThat(box.getNW()).isAt(230, 150);
        assertThat(box.getSW()).isAt(230, 250);
        assertThat(box.getNE()).isAt(430, 150);
        assertThat(box.getSE()).isAt(430, 250);

        // check that the resulting transform is a pure translation
        var at = box.calcImTransform();
        Assertions.assertEquals(AffineTransform.TYPE_TRANSLATION, at.getType());
        checkTransform(at, 100, 100, 130, 150);
    }

    @Test
    void cornerHandleScaling() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        // translate SE from 400, 200 to 600, 400
        // so that the width becomes 200->400, (xScale = 2)
        // and the height becomes 100->300 (yScale = 3)
        press(box, 400, 200);
        drag(box, 500, 250);
        drag(box, 600, 300);
        release(box, 600, 400);

        assertThat(box.getSE()).isAt(600, 400).isAtIm(600, 400);
        assertThat(box.getNE()).isAt(600, 100).isAtIm(600, 100);

        var at = box.calcImTransform();
        Assertions.assertEquals(at.getType(), AffineTransform.TYPE_GENERAL_SCALE | AffineTransform.TYPE_TRANSLATION);
        // check that a point at NW does not move...
        checkTransform(at, 200, 100, 200, 100);
        // ...and that a point at SE transforms like SE
        checkTransform(at, 400, 200, 600, 400);
    }

    @Test
    void scaleUsingEastEdgeHandle() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        assertThat(box).rotSizeIs(200, 100);

        // drag the east edge 50 pixels to the right
        press(box, 400, 150);
        drag(box, 450, 150);
        release(box, 450, 150);

        // check that the width has increased but the height has not
        assertThat(box).rotSizeIs(250, 100);

        // check that the west edge (and NW corner) has not moved
        assertThat(box.getNW()).isAt(200, 100);
        // check that the east edge (and SE corner) has moved
        assertThat(box.getSE()).isAt(450, 200);

        var at = box.calcImTransform();
        Assertions.assertEquals(at.getType(), AffineTransform.TYPE_GENERAL_SCALE | AffineTransform.TYPE_TRANSLATION);
    }

    @Test
    void pureRotation() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        RotationHandle rot = box.getRot();
        CornerHandle nw = box.getNW();
        CornerHandle sw = box.getSW();
        CornerHandle ne = box.getNE();
        CornerHandle se = box.getSE();

        int rotOrigY = 100 - TransformBox.ROT_HANDLE_DISTANCE;
        assertThat(rot).isAt(300, rotOrigY).isAtIm(300, rotOrigY);
        assertThat(box).angleDegreesIs(0);
        assertThat(nw).cursorNameIs("Northwest Resize Cursor");
        assertThat(sw).cursorNameIs("Southwest Resize Cursor");
        assertThat(ne).cursorNameIs("Northeast Resize Cursor");
        assertThat(se).cursorNameIs("Southeast Resize Cursor");

        // rotate by 90 degrees
        press(box, 300, rotOrigY);
        drag(box, 200, 120);
        release(box, 10, 150);
        assertThat(box).angleDegreesIs(90);

        assertThat(nw).isAt(250, 250).isAtIm(250, 250);
        assertThat(sw).isAt(350, 250).isAtIm(350, 250);
        assertThat(ne).isAt(250, 50).isAtIm(250, 50);
        assertThat(se).isAt(350, 50).isAtIm(350, 50);

        assertThat(nw).cursorNameIs("Southwest Resize Cursor");
        assertThat(sw).cursorNameIs("Southeast Resize Cursor");
        assertThat(ne).cursorNameIs("Northwest Resize Cursor");
        assertThat(se).cursorNameIs("Northeast Resize Cursor");

        // rotate by setting an angle
        box.rotateTo(180, INTUITIVE_DEGREES);
        box.updateDirections();
        assertThat(box).angleDegreesIs(180);

        assertThat(nw).isAt(400, 200).isAtIm(400, 200);
        assertThat(sw).isAt(400, 100).isAtIm(400, 100);
        assertThat(ne).isAt(200, 200).isAtIm(200, 200);
        assertThat(se).isAt(200, 100).isAtIm(200, 100);

        assertThat(nw).cursorNameIs("Southeast Resize Cursor");
        assertThat(sw).cursorNameIs("Northeast Resize Cursor");
        assertThat(ne).cursorNameIs("Southwest Resize Cursor");
        assertThat(se).cursorNameIs("Northwest Resize Cursor");
    }

    @Test
    void cursorAfterTurnedInsideOut() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        CornerHandle nw = box.getNW();
        CornerHandle sw = box.getSW();
        CornerHandle ne = box.getNE();
        CornerHandle se = box.getSE();

        // drag NW downwards
        press(box, 200, 100);
        drag(box, 200, 200);
        release(box, 200, 300);
        assertThat(box)
            .angleDegreesIs(180)
            .rotSizeIs(200, -100);

        assertThat(nw).cursorNameIs("Southwest Resize Cursor");
        assertThat(sw).cursorNameIs("Northwest Resize Cursor");
        assertThat(ne).cursorNameIs("Southeast Resize Cursor");
        assertThat(se).cursorNameIs("Northeast Resize Cursor");

        // drag NW downwards again
        press(box, 200, 300);
        drag(box, 200, 350);
        release(box, 200, 400);
        assertThat(box)
            .angleDegreesIs(180)
            .rotSizeIs(-200, 200);

        // expect no change
        assertThat(nw).cursorNameIs("Southwest Resize Cursor");
        assertThat(sw).cursorNameIs("Northwest Resize Cursor");
        assertThat(ne).cursorNameIs("Southeast Resize Cursor");
        assertThat(se).cursorNameIs("Northeast Resize Cursor");

        // drag back
        press(box, 200, 400);
        drag(box, 200, 350);
        release(box, 200, 300);
        assertThat(box)
            .angleDegreesIs(180)
            .rotSizeIs(-200, 100);

        // drag NE to the left
        press(box, 400, 300);
        drag(box, 200, 300);
        release(box, 100, 300);
        assertThat(box)
            .angleDegreesIs(180)
            .rotSizeIs(100, 100);

        assertThat(nw).cursorNameIs("Southeast Resize Cursor");
        assertThat(sw).cursorNameIs("Northeast Resize Cursor");
        assertThat(ne).cursorNameIs("Southwest Resize Cursor");
        assertThat(se).cursorNameIs("Northwest Resize Cursor");

        // drag NE upwards
        press(box, 100, 300);
        drag(box, 100, 200);
        release(box, 100, 100);
        assertThat(box)
            .angleDegreesIs(0)
            .rotSizeIs(-100, 100);

        assertThat(nw).cursorNameIs("Northeast Resize Cursor");
        assertThat(sw).cursorNameIs("Southeast Resize Cursor");
        assertThat(ne).cursorNameIs("Northwest Resize Cursor");
        assertThat(se).cursorNameIs("Southwest Resize Cursor");
    }

    @Test
    void imageSpaceRotation() throws NoninvertibleTransformException {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        RotationHandle rot = box.getRot();
        CornerHandle nw = box.getNW();
        CornerHandle sw = box.getSW();
        CornerHandle ne = box.getNE();
        CornerHandle se = box.getSE();

        int rotOrigY = 100 - TransformBox.ROT_HANDLE_DISTANCE;
        assertThat(rot)
            .isAt(300, rotOrigY)
            .isAtIm(300, rotOrigY);
        assertThat(box).angleDegreesIs(0);

        // rotate around NW 90 degrees
        var at = AffineTransform.getQuadrantRotateInstance(1, 200, 100);
        box.imCoordsChanged(at, view);

        assertThat(nw).isAt(200, 100).isAtIm(200, 100); // no change
        assertThat(sw).isAt(100, 100).isAtIm(100, 100);
        assertThat(ne).isAt(200, 300).isAtIm(200, 300);
        assertThat(se).isAt(100, 300).isAtIm(100, 300);
        assertThat(rot)
            .isAt(200 + TransformBox.ROT_HANDLE_DISTANCE, 200)
            .isAtIm(200 + TransformBox.ROT_HANDLE_DISTANCE, 200);
        assertThat(box).angleDegreesIs(270);

        // drag SE downwards
        press(box, 100, 300);
        drag(box, 100, 350);
        release(box, 100, 400);

        assertThat(nw).isAt(200, 100).isAtIm(200, 100); // no change
        assertThat(sw).isAt(100, 100).isAtIm(100, 100); // no change
        assertThat(ne).isAt(200, 400).isAtIm(200, 400);
        assertThat(se).isAt(100, 400).isAtIm(100, 400);
        assertThat(rot)
            .isAt(200 + TransformBox.ROT_HANDLE_DISTANCE, 250)
            .isAtIm(200 + TransformBox.ROT_HANDLE_DISTANCE, 250);
        assertThat(box).angleDegreesIs(270); // no change

        // rotate back
        at = at.createInverse();
        box.imCoordsChanged(at, view);

        assertThat(nw).isAt(200, 100).isAtIm(200, 100); // no change
        assertThat(sw).isAt(200, 200).isAtIm(200, 200);
        assertThat(ne).isAt(500, 100).isAtIm(500, 100);
        assertThat(se).isAt(500, 200).isAtIm(500, 200);
        assertThat(rot).isAt(350, rotOrigY).isAtIm(350, rotOrigY);
        assertThat(box).angleDegreesIs(0);
    }

    @Test
    void calcAngleCursorOffset() {
        checkOffset(0, 0);
        checkOffset(20, 0);
        checkOffset(40, 1);
        checkOffset(50, 1);
        checkOffset(190, 4);
        checkOffset(260, 6);
        checkOffset(350, 0);
    }

    @Test
    void nudgeWithArrowKeys() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        // nudge right by 1 pixel
        box.arrowKeyPressed(ArrowKey.RIGHT, view);
        assertThat(box.getNW()).isAtIm(201, 100);
        assertThat(box.getSE()).isAtIm(401, 200);

        // nudge down by 1 pixel
        box.arrowKeyPressed(ArrowKey.DOWN, view);
        assertThat(box.getNW()).isAtIm(201, 101);
        assertThat(box.getSE()).isAtIm(401, 201);

        // nudge left by 10 pixels (shift)
        box.arrowKeyPressed(ArrowKey.SHIFT_LEFT, view);
        assertThat(box.getNW()).isAtIm(191, 101);
        assertThat(box.getSE()).isAtIm(391, 201);

        // nudge up by 10 pixels (shift)
        box.arrowKeyPressed(ArrowKey.SHIFT_UP, view);
        assertThat(box.getNW()).isAtIm(191, 91);
        assertThat(box.getSE()).isAtIm(391, 191);
    }

    @Test
    void contextMenuFlipHorizontal() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        Point2D nwOrig = box.getNW().getImLocationCopy();
        Point2D neOrig = box.getNE().getImLocationCopy();
        Point2D swOrig = box.getSW().getImLocationCopy();
        Point2D seOrig = box.getSE().getImLocationCopy();

        box.flip(FlipDirection.HORIZONTAL);

        // check that the east and west corners have been swapped
        assertThat(box.getNW()).isAtIm(neOrig.getX(), neOrig.getY());
        assertThat(box.getNE()).isAtIm(nwOrig.getX(), nwOrig.getY());
        assertThat(box.getSW()).isAtIm(seOrig.getX(), seOrig.getY());
        assertThat(box.getSE()).isAtIm(swOrig.getX(), swOrig.getY());

        // check that the width is now negative
        assertThat(box).rotSizeIs(-200, 100);
    }

    @Test
    void contextMenuFlipVertical() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        Point2D nwOrig = box.getNW().getImLocationCopy();
        Point2D neOrig = box.getNE().getImLocationCopy();
        Point2D swOrig = box.getSW().getImLocationCopy();
        Point2D seOrig = box.getSE().getImLocationCopy();

        box.flip(FlipDirection.VERTICAL);

        // check that the north and south corners have been swapped
        assertThat(box.getNW()).isAtIm(swOrig.getX(), swOrig.getY());
        assertThat(box.getSW()).isAtIm(nwOrig.getX(), nwOrig.getY());
        assertThat(box.getNE()).isAtIm(seOrig.getX(), seOrig.getY());
        assertThat(box.getSE()).isAtIm(neOrig.getX(), neOrig.getY());

        // check that the height is now negative
        assertThat(box).rotSizeIs(200, -100);
    }

    @Test
    void contextMenuRotate() {
        var box = new TransformBox(originalRect, view, DUMMY_TRANSFORMABLE);
        checkOriginalHandleState(box);

        // rotate 90 degrees
        box.rotate(QuadrantAngle.ANGLE_90);
        assertThat(box).angleDegreesIs(270); // TODO is this a bug in TransformBox?
        // assert positions based on rotation around center (300, 150)
        assertThat(box.getNW()).isAtIm(350, 50);
        assertThat(box.getNE()).isAtIm(350, 250);
        assertThat(box.getSW()).isAtIm(250, 50);
        assertThat(box.getSE()).isAtIm(250, 250);

        // rotate another 90 degrees (total 180)
        box.rotate(QuadrantAngle.ANGLE_90);
        assertThat(box).angleDegreesIs(180);
        // assert positions based on 180 deg rotation from original
        assertThat(box.getNW()).isAtIm(400, 200);
        assertThat(box.getNE()).isAtIm(200, 200);
        assertThat(box.getSW()).isAtIm(400, 100);
        assertThat(box.getSE()).isAtIm(200, 100);

        // rotate another 90 degrees (total 270)
        box.rotate(QuadrantAngle.ANGLE_90);
        assertThat(box).angleDegreesIs(90); // TODO should we expect 270?
        // assert positions based on 270 deg rotation from original
        assertThat(box.getNW()).isAtIm(250, 250);
        assertThat(box.getNE()).isAtIm(250, 50);
        assertThat(box.getSW()).isAtIm(350, 250);
        assertThat(box.getSE()).isAtIm(350, 50);

        // rotate another 90 degrees (total 360 -> 0)
        box.rotate(QuadrantAngle.ANGLE_90);
        assertThat(box).angleDegreesIs(0);
        checkOriginalHandleState(box);
    }

    /**
     * Checks the cursor offset calculation for a given angle.
     */
    private static void checkOffset(int angleDegrees, int expectedOffset) {
        int offset = TransformBox.calcCursorOffset(angleDegrees);
        Assertions.assertEquals(expectedOffset, offset);
    }

    /**
     * Asserts that an affine transform correctly maps a start point to an expected end point.
     */
    private static void checkTransform(AffineTransform at, double startX, double startY,
                                       double expectedX, double expectedY) {
        checkPointTransform(at,
            new Point2D.Double(startX, startY),
            new Point2D.Double(expectedX, expectedY));
    }

    /**
     * Asserts that an affine transform maps a start point to an expected end point.
     */
    private static void checkPointTransform(AffineTransform at, Point2D start, Point2D expected) {
        Point2D found = at.transform(start, null);
        if (!found.equals(expected)) {
            throw new AssertionError(String.format(
                "Expected (%.1f, %.1f), found (%.1f, %.1f)",
                expected.getX(), expected.getY(), found.getX(), found.getY()));
        }
    }

    /**
     * Asserts that an affine transform maps the corners of a start rectangle to an end rectangle.
     */
    private static void checkRectangleTransform(AffineTransform at, Rectangle start, Rectangle end) {
        Point2D topLeftStart = new Point2D.Double(start.x, start.y);
        Point2D topLeftExpected = new Point2D.Double(end.x, end.y);
        checkPointTransform(at, topLeftStart, topLeftExpected);

        Point2D bottomRightStart = new Point2D.Double(
            start.x + start.width,
            start.y + start.height);
        Point2D bottomRightExpected = new Point2D.Double(
            end.x + end.width,
            end.y + end.height);
        checkPointTransform(at, bottomRightStart, bottomRightExpected);
    }

    /**
     * Asserts that the transform box handles are at their initial positions.
     */
    private static void checkOriginalHandleState(TransformBox box) {
        Point2D nw = new Point2D.Double(200, 100);
        Point2D sw = new Point2D.Double(200, 200);
        Point2D ne = new Point2D.Double(400, 100);
        Point2D se = new Point2D.Double(400, 200);
        assertThat(box).hasCornersAt(nw, sw, ne, se);
        assertThat(box).hasCornersAtIm(nw, sw, ne, se);
    }

    /**
     * Simulates a mouse press event on the transform box.
     */
    private void press(TransformBox box, int x, int y) {
        box.processMousePressed(Modifiers.NONE.createPEvent(x, y, MOUSE_PRESSED, view));
    }

    /**
     * Simulates a mouse drag event on the transform box.
     */
    private void drag(TransformBox box, int x, int y) {
        box.processMouseDragged(Modifiers.NONE.createPEvent(x, y, MOUSE_DRAGGED, view));
    }

    /**
     * Simulates a mouse release event on the transform box.
     */
    private void release(TransformBox box, int x, int y) {
        box.processMouseReleased(Modifiers.NONE.createPEvent(x, y, MOUSE_RELEASED, view));
    }
}
