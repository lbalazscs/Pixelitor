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

package pixelitor.tools.transform;

import org.junit.Before;
import org.junit.Test;
import pixelitor.TestHelper;
import pixelitor.gui.View;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class TransformBoxTest {
    private Rectangle originalRect = new Rectangle(200, 100, 200, 100);
    private View ic;

    @Before
    public void setUp() throws Exception {
        ic = TestHelper.createICWithoutComp();
    }

    @Test
    public void moveNWFromInitialState() {
        TransformBox box = new TransformBox(originalRect, ic, at -> {
        });
        TransformHandle nw = box.getNW();
        TransformHandle sw = box.getSW();
        TransformHandle ne = box.getNE();
        TransformHandle se = box.getSE();

        // check the handles original state
        assertHandleIsAt(nw, 200, 100);
        assertHandleIsAt(sw, 200, 200);
        assertHandleIsAt(ne, 400, 100);
        assertHandleIsAt(se, 400, 200);

        nw.mousePressed(202, 100);
        nw.mouseDragged(222, 100); // dragged 20 pixels horizontally to the right

        // check that both SW and SW moved
        assertHandleIsAt(nw, 220, 100);
        assertHandleIsAt(sw, 220, 200);
        // and the other two didn't move
        assertHandleIsAt(ne, 400, 100);
        assertHandleIsAt(se, 400, 200);

        // drag additional 20 pixels to the right, but also 10 pixels down
        nw.mouseDragged(242, 110);

        // check that NW followed the mouse
        assertHandleIsAt(nw, 240, 110);
        // and SW moved only horizontally
        assertHandleIsAt(sw, 240, 200);
        // and NE moved only vertically
        assertHandleIsAt(ne, 400, 110);
        // and SE didn't move at all
        assertHandleIsAt(se, 400, 200);

        // check that the transform behaves like the handles
// TODO
//        AffineTransform at = box.getCoTransform();
//        checkTransform(at,
//                new Rectangle(200, 100, 200, 100),
//                new Rectangle(240, 110, 160, 90));
    }

    @Test
    public void moveSEFromInitialState() {
        TransformBox box = new TransformBox(originalRect, ic, at -> {
        });
        TransformHandle nw = box.getNW();
        TransformHandle sw = box.getSW();
        TransformHandle ne = box.getNE();
        TransformHandle se = box.getSE();

        // check the handles original state
        assertHandleIsAt(nw, 200, 100);
        assertHandleIsAt(sw, 200, 200);
        assertHandleIsAt(ne, 400, 100);
        assertHandleIsAt(se, 400, 200);

        se.mousePressed(402, 202);
        se.mouseDragged(382, 202); // dragged 20 pixels horizontally to the left

        // check that both SE and NE moved
        assertHandleIsAt(se, 380, 200);
        assertHandleIsAt(ne, 380, 100);
        // and the other two didn't move
        assertHandleIsAt(nw, 200, 100);
        assertHandleIsAt(sw, 200, 200);

        // drag additional 20 pixels to the left, but also 10 pixels up
        se.mouseDragged(362, 192);

        // check that SE followed the mouse
        assertHandleIsAt(se, 360, 190);
        // and NE moved only horizontally
        assertHandleIsAt(ne, 360, 100);
        // and SW moved only vertically
        assertHandleIsAt(sw, 200, 190);
        // and NW didn't move at all
        assertHandleIsAt(nw, 200, 100);

        // check that the transform behaves like the handles
// TODO
//        AffineTransform at = box.getCoTransform();
//        checkTransform(at,
//                new Rectangle(200, 100, 200, 100),
//                new Rectangle(200, 100, 160, 90));
    }

    private void checkTransform(AffineTransform at, Rectangle start, Rectangle end) {
        Point2D.Double topLeftStart = new Point2D.Double(start.x, start.y);
        Point2D.Double topLeftExpected = new Point2D.Double(end.x, end.y);
        Point2D transformedTopLeft = at.transform(topLeftStart, null);
        if (!transformedTopLeft.equals(topLeftExpected)) {
            throw new AssertionError("Expected top left " + topLeftExpected
                    + ", found " + transformedTopLeft);
        }

        Point2D.Double bottomRightStart = new Point2D.Double(
                start.x + start.width,
                start.y + start.height);
        Point2D.Double bottomRightExpected = new Point2D.Double(
                end.x + end.width,
                end.y + end.height);
        Point2D transformedBottomRight = at.transform(bottomRightStart, null);
        if (!transformedBottomRight.equals(bottomRightExpected)) {
            throw new AssertionError("Expected bottom right " + bottomRightExpected
                    + ", found " + transformedBottomRight);
        }
    }

    private void assertHandleIsAt(TransformHandle th, double x, double y) {
        if (Math.abs(th.x - x) > 0.1 || Math.abs(th.y - y) > 0.1) {
            System.out.printf("TransformBoxTest::assertHandleIsAt: x = %.2f, y = %.2f%n", x, y);
            throw new AssertionError(String.format(
                    "For the handle %s expected pos (%.2f, %.2f), found (%.2f, %.2f)",
                    th.getName(), x, y, th.x, th.y));
        }
    }
}