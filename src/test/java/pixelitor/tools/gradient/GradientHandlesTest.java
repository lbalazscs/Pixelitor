/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;

import java.awt.geom.AffineTransform;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("GradientHandles tests")
@TestMethodOrder(MethodOrderer.Random.class)
class GradientHandlesTest {
    // the initial positions for the start, end and center handles
    private static final double START_X_INIT = 10.0;
    private static final double START_Y_INIT = 70.0;
    private static final double END_X_INIT = 30.0;
    private static final double END_Y_INIT = 50.0;
    private static final double CENTER_X_INIT = (START_X_INIT + END_X_INIT) / 2;
    private static final double CENTER_Y_INIT = (START_Y_INIT + END_Y_INIT) / 2;

    private GradientHandles handles;
    private GradientDefiningPoint start;
    private GradientDefiningPoint end;
    private GradientCenterPoint center;
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
        center = handles.getCenter();

        assertGradientPosition(START_X_INIT, START_Y_INIT, END_X_INIT, END_Y_INIT);
    }

    @Test
    @DisplayName("dragging the center handle moves the start and end handles")
    void centerMovesStartEnd() {
        int dx = -5;
        int dy = 10;

        simulateDrag(center, dx, dy);

        assertGradientPosition(
            START_X_INIT + dx, START_Y_INIT + dy,
            END_X_INIT + dx, END_Y_INIT + dy);
    }

    @Test
    @DisplayName("dragging the start handle moves the center handle")
    void startMovesCenter() {
        int dx = -10;
        int dy = 15;

        simulateDrag(start, dx, dy);

        assertGradientPosition(
            START_X_INIT + dx, START_Y_INIT + dy,
            END_X_INIT, END_Y_INIT); // end handle acts as anchor
    }

    @Test
    @DisplayName("dragging the end handle moves the center handle")
    void endMovesCenter() {
        int dx = 20;
        int dy = 10;

        simulateDrag(end, dx, dy);

        assertGradientPosition(
            START_X_INIT, START_Y_INIT, // start handle acts as anchor
            END_X_INIT + dx, END_Y_INIT + dy);
    }

    @Test
    @DisplayName("dragging start handle with Shift key snaps the angle")
    void constrainedDragStartSnapsAngle() {
        // move start handle to be almost horizontal to the end handle
        double dragDist = 50;
        double slightOffset = -15;

        simulateDrag(start, dragDist, slightOffset, true);

        // start should snap to same Y as end
        assertGradientPosition(
            START_X_INIT + dragDist, END_Y_INIT,
            END_X_INIT, END_Y_INIT);
    }

    @Test
    @DisplayName("dragging end handle with Shift key snaps the angle")
    void constrainedDragEndSnapsAngle() {
        // drag end handle to a position slightly off the horizontal axis relative to start
        double dragDist = 60;
        double slightOffset = -15;

        simulateDrag(end, dragDist, slightOffset, true);

        // end should snap to same Y as start
        assertGradientPosition(
            START_X_INIT, START_Y_INIT,
            END_X_INIT + dragDist, START_Y_INIT);
    }

    @Test
    @DisplayName("gradient handles can overlap")
    void handlesCanOverlap() {
        // drag start to exactly match end
        double dx = END_X_INIT - START_X_INIT;
        double dy = END_Y_INIT - START_Y_INIT;

        simulateDrag(start, dx, dy);

        assertGradientPosition(END_X_INIT, END_Y_INIT, END_X_INIT, END_Y_INIT);
    }

    @ParameterizedTest
    @EnumSource(ArrowKey.class)
    @DisplayName("pressing arrow key moves the entire gradient")
    void arrowKeyMovesGradient(ArrowKey key) {
        handles.arrowKeyPressed(key, view);

        int dx = key.getDeltaX();
        int dy = key.getDeltaY();

        assertGradientPosition(
            START_X_INIT + dx, START_Y_INIT + dy,
            END_X_INIT + dx, END_Y_INIT + dy);
    }

    @Test
    @DisplayName("translate handles after image coordinates change")
    void imCoordsChanged_translate() {
        int dx = 10;
        int dy = 20;
        var at = AffineTransform.getTranslateInstance(dx, dy);
        handles.imCoordsChanged(at, view);

        assertGradientPosition(
            START_X_INIT + dx, START_Y_INIT + dy,
            END_X_INIT + dx, END_Y_INIT + dy);
    }

    @Test
    @DisplayName("zoom handles after image coordinates change")
    void imCoordsChanged_scale() {
        var at = AffineTransform.getScaleInstance(0.5, 0.5);
        handles.imCoordsChanged(at, view);

        assertGradientPosition(
            START_X_INIT * 0.5, START_Y_INIT * 0.5,
            END_X_INIT * 0.5, END_Y_INIT * 0.5);
    }

    @Test
    @DisplayName("findHandleAt returns the correct handle near coordinates")
    void testFindHandleAt() {
        // check exact positions
        assertThat(handles.findHandleAt(START_X_INIT, START_Y_INIT)).isSameAs(start);
        assertThat(handles.findHandleAt(END_X_INIT, END_Y_INIT)).isSameAs(end);
        assertThat(handles.findHandleAt(CENTER_X_INIT, CENTER_Y_INIT)).isSameAs(center);

        // check near positions (within handle radius)
        double offset = DraggablePoint.HANDLE_RADIUS - 1.0;
        assertThat(handles.findHandleAt(START_X_INIT + offset, START_Y_INIT)).isSameAs(start);

        // check empty space
        assertThat(handles.findHandleAt(0, 0)).isNull();
    }

    /**
     * Simulates a mouse drag sequence on a specific handle.
     */
    private static void simulateDrag(DraggablePoint handle, double dx, double dy) {
        simulateDrag(handle, dx, dy, false);
    }

    /**
     * Simulates a mouse drag sequence on a specific handle with optional constraint.
     */
    private static void simulateDrag(DraggablePoint handle, double dx, double dy, boolean constrained) {
        double startX = handle.getX();
        double startY = handle.getY();

        handle.mousePressed(startX, startY);
        // simulate an intermediate drag event
        handle.mouseDragged(startX + dx / 2.0, startY + dy / 2.0, constrained);
        handle.mouseReleased(startX + dx, startY + dy, constrained);
    }

    private void assertGradientPosition(double startX, double startY,
                                        double endX, double endY) {
        double centerX = (startX + endX) / 2.0;
        double centerY = (startY + endY) / 2.0;

        assertThat(start)
            .isAt(startX, startY)
            .isSyncedWithView(view);
        assertThat(center)
            .isAt(centerX, centerY)
            .isSyncedWithView(view);
        assertThat(end)
            .isAt(endX, endY)
            .isSyncedWithView(view);
    }
}
