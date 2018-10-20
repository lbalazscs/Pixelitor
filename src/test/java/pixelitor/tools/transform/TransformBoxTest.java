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

import static org.junit.Assert.assertEquals;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

public class TransformBoxTest {
    private final Rectangle originalRect = new Rectangle(200, 100, 200, 100);
    private View view;

    @Before
    public void setUp() {
        view = TestHelper.createMockICWithoutComp();
    }

    @Test
    public void moveNWFromInitialState() {
        TransformBox box = new TransformBox(originalRect,
                view, at -> {});
        TransformHandle nw = box.getNW();
        TransformHandle sw = box.getSW();
        TransformHandle ne = box.getNE();
        TransformHandle se = box.getSE();

        // check the handles original state
        assertThat(nw).isAt(200, 100);
        assertThat(sw).isAt(200, 200);
        assertThat(ne).isAt(400, 100);
        assertThat(se).isAt(400, 200);

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
        AffineTransform at = box.calcImTransform();
        checkTransform(at,
                new Rectangle(200, 100, 200, 100),
                new Rectangle(240, 110, 160, 90));
    }

    @Test
    public void moveSEFromInitialState() {
        TransformBox box = new TransformBox(originalRect, view, at -> {});
        TransformHandle nw = box.getNW();
        TransformHandle sw = box.getSW();
        TransformHandle ne = box.getNE();
        TransformHandle se = box.getSE();

        // check the handles original state
        assertThat(nw).isAt(200, 100);
        assertThat(sw).isAt(200, 200);
        assertThat(ne).isAt(400, 100);
        assertThat(se).isAt(400, 200);

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
        AffineTransform at = box.calcImTransform();
        checkTransform(at,
                new Rectangle(200, 100, 200, 100),
                new Rectangle(200, 100, 160, 90));
    }

    @Test
    public void pureTranslation() {
        TransformBox box = new TransformBox(originalRect,
                view, at -> {});
        TransformHandle nw = box.getNW();
        TransformHandle sw = box.getSW();
        TransformHandle ne = box.getNE();
        TransformHandle se = box.getSE();

        // check the handles original state
        assertThat(nw).isAt(200, 100);
        assertThat(sw).isAt(200, 200);
        assertThat(ne).isAt(400, 100);
        assertThat(se).isAt(400, 200);

        // translate NW by 30, 20
        nw.mousePressed(200, 100);
        nw.mouseDragged(210, 110);
        nw.mouseDragged(220, 115);
        nw.mouseReleased(230, 120);

        // also translate SE by the same amount (30, 20)
        se.mousePressed(400, 200);
        se.mouseDragged(415, 210);
        se.mouseDragged(430, 220);
        se.mouseReleased(430, 220);

        AffineTransform at = box.calcImTransform();
        // should be a pure (30, 20) translation and nothing else
        assertEquals(AffineTransform.TYPE_TRANSLATION, at.getType());
        checkTransform(at, 100, 100, 130, 120);
    }

    @Test
    public void pureScaling() {
        TransformBox box = new TransformBox(originalRect,
                view, at -> {});
        TransformHandle nw = box.getNW();
        TransformHandle sw = box.getSW();
        TransformHandle ne = box.getNE();
        TransformHandle se = box.getSE();

        // check the handles original state
        assertThat(nw).isAt(200, 100).isAtIm(200, 100);
        assertThat(sw).isAt(200, 200).isAtIm(200, 200);
        assertThat(ne).isAt(400, 100).isAtIm(400, 100);
        assertThat(se).isAt(400, 200).isAtIm(400, 200);

        // translate SE from 400, 200 to 600, 400
        // so that the width becomes 200->400, (xScale = 2)
        // and the height becomes 100->300 (yScale = 3)
        se.mousePressed(400, 200);
        se.mouseDragged(500, 250);
        se.mouseDragged(600, 300);
        se.mouseReleased(600, 400);

        assertThat(se).isAt(600, 400).isAtIm(600, 400);
        assertThat(ne).isAt(600, 100).isAtIm(600, 100);

        AffineTransform at = box.calcImTransform();
        // check that a point at NE does not move...
        checkTransform(at, 200, 100, 200, 100);
        // ...and that a point at SE transforms like SE
        checkTransform(at, 400, 200, 600, 400);
    }

    @Test
    public void pureRotation() {
    }

    private static void checkTransform(AffineTransform at, double startX, double startY,
                                       double expectedX, double expectedY) {
        checkTransform(at,
                new Point2D.Double(startX, startY),
                new Point2D.Double(expectedX, expectedY));
    }

    private static void checkTransform(AffineTransform at, Point2D start, Point2D expected) {
        Point2D found = at.transform(start, null);
        if (!found.equals(expected)) {
            throw new AssertionError(String.format(
                    "Expected (%.1f, %.1f), found (%.1f, %.1f)",
                    expected.getX(), expected.getY(),
                    found.getX(), found.getY()));
        }
    }

    private static void checkTransform(AffineTransform at, Rectangle start, Rectangle end) {
        Point2D.Double topLeftStart = new Point2D.Double(start.x, start.y);
        Point2D.Double topLeftExpected = new Point2D.Double(end.x, end.y);
        checkTransform(at, topLeftStart, topLeftExpected);

        Point2D.Double bottomRightStart = new Point2D.Double(
                start.x + start.width,
                start.y + start.height);
        Point2D.Double bottomRightExpected = new Point2D.Double(
                end.x + end.width,
                end.y + end.height);
        checkTransform(at, bottomRightStart, bottomRightExpected);
    }
}