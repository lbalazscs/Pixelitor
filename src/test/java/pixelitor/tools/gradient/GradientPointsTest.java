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

package pixelitor.tools.gradient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pixelitor.gui.ImageComponent;

public class GradientPointsTest {

    private static final int START_X_FOR_END = 30;
    private static final int START_Y_FOR_END = 50;
    private static final int START_X_FOR_START = 10;
    private static final int START_Y_FOR_START = 70;
    private static final int START_X_FOR_MIDDLE = (START_X_FOR_START + START_X_FOR_END) / 2;
    private static final int START_Y_FOR_MIDDLE = (START_Y_FOR_START + START_Y_FOR_END) / 2;

    private GradientDefiningPoint start;
    private GradientDefiningPoint end;
    private GradientCenterPoint middle;

    @Before
    public void setup() {
        ImageComponent ic = Mockito.mock(ImageComponent.class);
        GradientPoints gp = new GradientPoints(START_X_FOR_START, START_Y_FOR_START, START_X_FOR_END, START_Y_FOR_END, ic);
        start = gp.getStart();
        end = gp.getEnd();
        middle = gp.getMiddle();

        start.assertLocationIs(START_X_FOR_START, START_Y_FOR_START);
        end.assertLocationIs(START_X_FOR_END, START_Y_FOR_END);
        middle.assertLocationIs(START_X_FOR_MIDDLE, START_Y_FOR_MIDDLE);
    }

    @Test
    public void testCenterMovesTheOtherTwo() {
        int dragStartX = START_X_FOR_MIDDLE - 1;
        int dragStartY = START_Y_FOR_MIDDLE + 1;
        int dx = -5;
        int dy = 10;

        middle.mousePressed(dragStartX, dragStartY);
        middle.mouseDragged(dragStartX + dx / 4, dragStartY + dy / 4);
        middle.mouseDragged(dragStartX + dx / 2, dragStartY + dy / 2);
        middle.mouseReleased(dragStartX + dx, dragStartY + dy);

        start.assertLocationIs(START_X_FOR_START + dx, START_Y_FOR_START + dy);
        end.assertLocationIs(START_X_FOR_END + dx, START_Y_FOR_END + dy);
        middle.assertLocationIs(START_X_FOR_MIDDLE + dx, START_Y_FOR_MIDDLE + dy);
    }

    @Test
    public void testEndMovesTheCenter() {
        int dragStartX = START_X_FOR_END + 1;
        int dragStartY = START_Y_FOR_END + 2;
        int dx = 20;
        int dy = 10;

        end.mousePressed(dragStartX, dragStartY);
        end.mouseDragged(dragStartX + dx / 4, dragStartY + dy / 4);
        end.mouseDragged(dragStartX + dx / 2, dragStartY + dy / 2);
        end.mouseReleased(dragStartX + dx, dragStartY + dy);

        end.assertLocationIs(START_X_FOR_END + dx, START_Y_FOR_END + dy);
        start.assertLocationIs(START_X_FOR_START, START_Y_FOR_START);
        middle.assertLocationIs(START_X_FOR_MIDDLE + dx / 2, START_Y_FOR_MIDDLE + dy / 2);
    }

}