/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
        PPoint startPos = PPoint.eagerFromCo(START_X_INIT, START_Y_INIT, view);
        PPoint endPos = PPoint.eagerFromCo(END_X_INIT, END_Y_INIT, view);
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