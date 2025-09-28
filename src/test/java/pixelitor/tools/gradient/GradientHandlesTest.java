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

package pixelitor.tools.gradient;

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PPoint;

import java.awt.geom.AffineTransform;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("GradientHandles tests")
@TestMethodOrder(MethodOrderer.Random.class)
class GradientHandlesTest {
    // the initial positions for the start, end and middle handles
    private static final int START_X_INIT = 10;
    private static final int START_Y_INIT = 70;
    private static final int END_X_INIT = 30;
    private static final int END_Y_INIT = 50;
    private static final int MIDDLE_X_INIT = (START_X_INIT + END_X_INIT) / 2;
    private static final int MIDDLE_Y_INIT = (START_Y_INIT + END_Y_INIT) / 2;

    private GradientDefiningPoint start;
    private GradientDefiningPoint end;
    private GradientCenterPoint middle;
    private GradientHandles handles;
    private View view;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        view = TestHelper.createMockViewWithoutComp();
        PPoint startPos = new PPoint(START_X_INIT, START_Y_INIT, view);
        PPoint endPos = new PPoint(END_X_INIT, END_Y_INIT, view);
        handles = new GradientHandles(startPos, endPos, view);
        start = handles.getStart();
        end = handles.getEnd();
        middle = handles.getMiddle();

        assertThat(start)
            .isAt(START_X_INIT, START_Y_INIT)
            .isAtIm(START_X_INIT, START_Y_INIT);
        assertThat(end)
            .isAt(END_X_INIT, END_Y_INIT)
            .isAtIm(END_X_INIT, END_Y_INIT);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT, MIDDLE_Y_INIT)
            .isAtIm(MIDDLE_X_INIT, MIDDLE_Y_INIT);
    }

    @Test
    @DisplayName("dragging the center handle moves the start and end handles")
    void centerMovesStartEnd() {
        int dragStartX = MIDDLE_X_INIT - 1;
        int dragStartY = MIDDLE_Y_INIT + 1;
        int dx = -5;
        int dy = 10;

        middle.mousePressed(dragStartX, dragStartY);
        middle.mouseDragged(dragStartX + dx / 4.0, dragStartY + dy / 4.0);
        middle.mouseDragged(dragStartX + dx / 2.0, dragStartY + dy / 2.0);
        middle.mouseReleased(dragStartX + dx, dragStartY + dy);

        assertThat(start)
            .isAt(START_X_INIT + dx, START_Y_INIT + dy)
            .isAtIm(START_X_INIT + dx, START_Y_INIT + dy);
        assertThat(end)
            .isAt(END_X_INIT + dx, END_Y_INIT + dy)
            .isAtIm(END_X_INIT + dx, END_Y_INIT + dy);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT + dx, MIDDLE_Y_INIT + dy)
            .isAtIm(MIDDLE_X_INIT + dx, MIDDLE_Y_INIT + dy);
    }

    @Test
    @DisplayName("dragging the start handle moves the center handle")
    void startMovesCenter() {
        int dragStartX = START_X_INIT + 2;
        int dragStartY = START_Y_INIT - 3;
        int dx = -10;
        int dy = 15;

        start.mousePressed(dragStartX, dragStartY);
        start.mouseDragged(dragStartX + dx / 3.0, dragStartY + dy / 3.0);
        start.mouseDragged(dragStartX + dx / 1.5, dragStartY + dy / 1.5);
        start.mouseReleased(dragStartX + dx, dragStartY + dy);

        assertThat(start)
            .isAt(START_X_INIT + dx, START_Y_INIT + dy)
            .isAtIm(START_X_INIT + dx, START_Y_INIT + dy);
        assertThat(end)
            .isAt(END_X_INIT, END_Y_INIT)
            .isAtIm(END_X_INIT, END_Y_INIT);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT + dx / 2.0, MIDDLE_Y_INIT + dy / 2.0)
            .isAtIm(MIDDLE_X_INIT + dx / 2.0, MIDDLE_Y_INIT + dy / 2.0);
    }

    @Test
    @DisplayName("dragging the end handle moves the center handle")
    void endMovesCenter() {
        int dragStartX = END_X_INIT + 1;
        int dragStartY = END_Y_INIT + 2;
        int dx = 20;
        int dy = 10;

        end.mousePressed(dragStartX, dragStartY);
        end.mouseDragged(dragStartX + dx / 4.0, dragStartY + dy / 4.0);
        end.mouseDragged(dragStartX + dx / 2.0, dragStartY + dy / 2.0);
        end.mouseReleased(dragStartX + dx, dragStartY + dy);

        assertThat(end)
            .isAt(END_X_INIT + dx, END_Y_INIT + dy)
            .isAtIm(END_X_INIT + dx, END_Y_INIT + dy);
        assertThat(start)
            .isAt(START_X_INIT, START_Y_INIT)
            .isAtIm(START_X_INIT, START_Y_INIT);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT + dx / 2.0, MIDDLE_Y_INIT + dy / 2.0)
            .isAtIm(MIDDLE_X_INIT + dx / 2.0, MIDDLE_Y_INIT + dy / 2.0);
    }

    @Test
    @DisplayName("dragging with Shift key snaps the angle")
    void constrainedDragSnapsAngle() {
        int dragStartX = END_X_INIT;
        int dragStartY = END_Y_INIT;

        // drag to a point where the angle from the start handle is closer to 0 than 45 degrees
        // start is at (10, 70)
        int finalX = 70; // dx = 60
        int finalY = 55; // dy = -15
        // angle is atan2(-15, 60) which is ~-14 degrees, so it should snap to horizontal (0 degrees)

        end.mousePressed(dragStartX, dragStartY);
        end.mouseReleased(finalX, finalY, true); // shift is down

        // the new y should be the same as the start handle's y
        int expectedX = finalX;
        int expectedY = START_Y_INIT;

        assertThat(end)
            .isAt(expectedX, expectedY)
            .isAtIm(expectedX, expectedY);
        assertThat(start)
            .isAt(START_X_INIT, START_Y_INIT)
            .isAtIm(START_X_INIT, START_Y_INIT);
        assertThat(middle)
            .isAt((START_X_INIT + expectedX) / 2.0, (START_Y_INIT + expectedY) / 2.0)
            .isAtIm((START_X_INIT + expectedX) / 2.0, (START_Y_INIT + expectedY) / 2.0);
    }

    @Test
    @DisplayName("pressing arrow key moves the entire gradient")
    void arrowKeyMovesGradient() {
        handles.arrowKeyPressed(ArrowKey.RIGHT, view);

        int dx = ArrowKey.RIGHT.getDeltaX();
        int dy = ArrowKey.RIGHT.getDeltaY();

        assertThat(start)
            .isAt(START_X_INIT + dx, START_Y_INIT + dy)
            .isAtIm(START_X_INIT + dx, START_Y_INIT + dy);
        assertThat(end)
            .isAt(END_X_INIT + dx, END_Y_INIT + dy)
            .isAtIm(END_X_INIT + dx, END_Y_INIT + dy);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT + dx, MIDDLE_Y_INIT + dy)
            .isAtIm(MIDDLE_X_INIT + dx, MIDDLE_Y_INIT + dy);

        handles.arrowKeyPressed(ArrowKey.SHIFT_UP, view);

        int dx2 = ArrowKey.SHIFT_UP.getDeltaX();
        int dy2 = ArrowKey.SHIFT_UP.getDeltaY();

        assertThat(start)
            .isAt(START_X_INIT + dx + dx2, START_Y_INIT + dy + dy2)
            .isAtIm(START_X_INIT + dx + dx2, START_Y_INIT + dy + dy2);
        assertThat(end)
            .isAt(END_X_INIT + dx + dx2, END_Y_INIT + dy + dy2)
            .isAtIm(END_X_INIT + dx + dx2, END_Y_INIT + dy + dy2);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT + dx + dx2, MIDDLE_Y_INIT + dy + dy2)
            .isAtIm(MIDDLE_X_INIT + dx + dx2, MIDDLE_Y_INIT + dy + dy2);
    }

    @Test
    @DisplayName("translate handles after image coordinates change")
    void imCoordsChanged_translate() {
        int dx = 10;
        int dy = 20;
        var at = AffineTransform.getTranslateInstance(dx, dy);
        handles.imCoordsChanged(at, view);

        assertThat(start)
            .isAt(START_X_INIT + dx, START_Y_INIT + dy)
            .isAtIm(START_X_INIT + dx, START_Y_INIT + dy);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT + dx, MIDDLE_Y_INIT + dy)
            .isAtIm(MIDDLE_X_INIT + dx, MIDDLE_Y_INIT + dy);
        assertThat(end)
            .isAt(END_X_INIT + dx, END_Y_INIT + dy)
            .isAtIm(END_X_INIT + dx, END_Y_INIT + dy);
    }

    @Test
    @DisplayName("zoom handles after image coordinates change")
    void imCoordsChanged_scale() {
        var at = AffineTransform.getScaleInstance(0.5, 0.5);
        handles.imCoordsChanged(at, view);

        assertThat(start)
            .isAt(START_X_INIT * 0.5, START_Y_INIT * 0.5)
            .isAtIm(START_X_INIT * 0.5, START_Y_INIT * 0.5);
        assertThat(middle)
            .isAt(MIDDLE_X_INIT * 0.5, MIDDLE_Y_INIT * 0.5)
            .isAtIm(MIDDLE_X_INIT * 0.5, MIDDLE_Y_INIT * 0.5);
        assertThat(end)
            .isAt(END_X_INIT * 0.5, END_Y_INIT * 0.5)
            .isAtIm(END_X_INIT * 0.5, END_Y_INIT * 0.5);
    }
}
