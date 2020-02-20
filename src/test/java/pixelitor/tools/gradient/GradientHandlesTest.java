/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;
import pixelitor.gui.View;

import java.awt.geom.AffineTransform;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

public class GradientHandlesTest {

    private static final int START_X_FOR_END = 30;
    private static final int START_Y_FOR_END = 50;
    private static final int START_X_FOR_START = 10;
    private static final int START_Y_FOR_START = 70;
    private static final int START_X_FOR_MIDDLE = (START_X_FOR_START + START_X_FOR_END) / 2;
    private static final int START_Y_FOR_MIDDLE = (START_Y_FOR_START + START_Y_FOR_END) / 2;

    private GradientDefiningPoint start;
    private GradientDefiningPoint end;
    private GradientCenterPoint middle;
    private GradientHandles handles;

    @BeforeEach
    void beforeEachTest() {
        View view = TestHelper.createMockViewWithoutComp();
        handles = new GradientHandles(
                START_X_FOR_START, START_Y_FOR_START,
                START_X_FOR_END, START_Y_FOR_END, view);
        start = handles.getStart();
        end = handles.getEnd();
        middle = handles.getMiddle();

        assertThat(start)
                .isAt(START_X_FOR_START, START_Y_FOR_START)
                .isAtIm(START_X_FOR_START, START_Y_FOR_START);
        assertThat(end)
                .isAt(START_X_FOR_END, START_Y_FOR_END)
                .isAtIm(START_X_FOR_END, START_Y_FOR_END);
        assertThat(middle)
                .isAt(START_X_FOR_MIDDLE, START_Y_FOR_MIDDLE)
                .isAtIm(START_X_FOR_MIDDLE, START_Y_FOR_MIDDLE);
    }

    @Test
    void testCenterMovesTheOtherTwo() {
        int dragStartX = START_X_FOR_MIDDLE - 1;
        int dragStartY = START_Y_FOR_MIDDLE + 1;
        int dx = -5;
        int dy = 10;

        middle.mousePressed(dragStartX, dragStartY);
        middle.mouseDragged(dragStartX + dx / 4.0, dragStartY + dy / 4.0);
        middle.mouseDragged(dragStartX + dx / 2.0, dragStartY + dy / 2.0);
        middle.mouseReleased(dragStartX + dx, dragStartY + dy);

        assertThat(start)
                .isAt(START_X_FOR_START + dx, START_Y_FOR_START + dy)
                .isAtIm(START_X_FOR_START + dx, START_Y_FOR_START + dy);
        assertThat(end)
                .isAt(START_X_FOR_END + dx, START_Y_FOR_END + dy)
                .isAtIm(START_X_FOR_END + dx, START_Y_FOR_END + dy);
        assertThat(middle)
                .isAt(START_X_FOR_MIDDLE + dx, START_Y_FOR_MIDDLE + dy)
                .isAtIm(START_X_FOR_MIDDLE + dx, START_Y_FOR_MIDDLE + dy);
    }

    @Test
    void testEndMovesTheCenter() {
        int dragStartX = START_X_FOR_END + 1;
        int dragStartY = START_Y_FOR_END + 2;
        int dx = 20;
        int dy = 10;

        end.mousePressed(dragStartX, dragStartY);
        end.mouseDragged(dragStartX + dx / 4.0, dragStartY + dy / 4.0);
        end.mouseDragged(dragStartX + dx / 2.0, dragStartY + dy / 2.0);
        end.mouseReleased(dragStartX + dx, dragStartY + dy);

        assertThat(end)
                .isAt(START_X_FOR_END + dx, START_Y_FOR_END + dy)
                .isAtIm(START_X_FOR_END + dx, START_Y_FOR_END + dy);
        assertThat(start)
                .isAt(START_X_FOR_START, START_Y_FOR_START)
                .isAtIm(START_X_FOR_START, START_Y_FOR_START);
        assertThat(middle)
                .isAt(START_X_FOR_MIDDLE + dx / 2.0, START_Y_FOR_MIDDLE + dy / 2.0)
                .isAtIm(START_X_FOR_MIDDLE + dx / 2.0, START_Y_FOR_MIDDLE + dy / 2.0);
    }

    @Test
    void imCoordsChanged_translate() {
        int dx = 10;
        int dy = 20;
        var at = AffineTransform.getTranslateInstance(dx, dy);
        handles.imCoordsChanged(at);

        assertThat(start)
                .isAt(START_X_FOR_START + dx, START_Y_FOR_START + dy)
                .isAtIm(START_X_FOR_START + dx, START_Y_FOR_START + dy);
        assertThat(middle)
                .isAt(START_X_FOR_MIDDLE + dx, START_Y_FOR_MIDDLE + dy)
                .isAtIm(START_X_FOR_MIDDLE + dx, START_Y_FOR_MIDDLE + dy);
        assertThat(end)
                .isAt(START_X_FOR_END + dx, START_Y_FOR_END + dy)
                .isAtIm(START_X_FOR_END + dx, START_Y_FOR_END + dy);
    }

    @Test
    void imCoordsChanged_scale() {
        var at = AffineTransform.getScaleInstance(0.5, 0.5);
        handles.imCoordsChanged(at);

        assertThat(start)
                .isAt(START_X_FOR_START * 0.5, START_Y_FOR_START * 0.5)
                .isAtIm(START_X_FOR_START * 0.5, START_Y_FOR_START * 0.5);
        assertThat(middle)
                .isAt(START_X_FOR_MIDDLE * 0.5, START_Y_FOR_MIDDLE * 0.5)
                .isAtIm(START_X_FOR_MIDDLE * 0.5, START_Y_FOR_MIDDLE * 0.5);
        assertThat(end)
                .isAt(START_X_FOR_END * 0.5, START_Y_FOR_END * 0.5)
                .isAtIm(START_X_FOR_END * 0.5, START_Y_FOR_END * 0.5);
    }
}