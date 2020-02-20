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

package pixelitor.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.testutils.ShapeChecker;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.SubPath;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

public class ShapesTest {
    private View view;

    @BeforeEach
    void beforeEachTest() {
        var comp = TestHelper.createEmptyComposition();
        view = TestHelper.setupMockViewFor(comp);
    }

    @Test
    void convertRectangle() {
        Shape s = new Rectangle2D.Double(2, 2, 10, 10);
        checkRectangleShape(s);

        Path path = Shapes.shapeToPath(s, view);
        checkRectanglePath(path.getActiveSubpath());

        s = path.toImageSpaceShape();
        checkRectangleShape(s);

        path = Shapes.shapeToPath(s, view);
        checkRectanglePath(path.getActiveSubpath());
    }

    @Test
    void convertEllipse() {
        Shape s = new Ellipse2D.Double(2, 2, 10, 10);
        checkEllipseShape(s);

        Path path = Shapes.shapeToPath(s, view);
        checkEllipsePath(path.getActiveSubpath());

        s = path.toImageSpaceShape();
        checkEllipseShape(s);

        path = Shapes.shapeToPath(s, view);
        checkEllipsePath(path.getActiveSubpath());
    }

    private static void checkRectangleShape(Shape s) {
        ShapeChecker checker = new ShapeChecker(s);
        checker.assertNumMoveTosWas(1);
        checker.assertNumLineTosWas(4);
        checker.assertNumQuadTosWas(0);
        checker.assertNumCubicTosWas(0);
        checker.assertNumClosesWas(1);
    }

    private static void checkRectanglePath(SubPath sp) {
        assertThat(sp)
                .numAnchorsIs(4)
                .isClosed();

        var p1 = sp.getAnchor(0);
        assertThat(p1).isAt(2, 2);
        assertThat(p1.ctrlOut).isRetracted();
        assertThat(p1.ctrlIn).isRetracted();

        var p2 = sp.getAnchor(1);
        assertThat(p2).isAt(12, 2);
        assertThat(p2.ctrlOut).isRetracted();
        assertThat(p2.ctrlIn).isRetracted();

        var p3 = sp.getAnchor(2);
        assertThat(p3).isAt(12, 12);
        assertThat(p3.ctrlOut).isRetracted();
        assertThat(p3.ctrlIn).isRetracted();

        var p4 = sp.getAnchor(3);
        assertThat(p4).isAt(2, 12);
        assertThat(p4.ctrlOut).isRetracted();
        assertThat(p4.ctrlIn).isRetracted();
    }

    private static void checkEllipseShape(Shape s) {
        ShapeChecker checker = new ShapeChecker(s);
        checker.assertNumMoveTosWas(1);
        checker.assertNumLineTosWas(0);
        checker.assertNumQuadTosWas(0);
        checker.assertNumCubicTosWas(4);
        checker.assertNumClosesWas(1);
    }

    private static void checkEllipsePath(SubPath sp) {
        assertThat(sp)
                .numAnchorsIs(4)
                .isClosed();

        var p1 = sp.getAnchor(0);
        assertThat(p1)
                .isAt(12, 7)
                .anchorPointTypeIs(SYMMETRIC);
        assertThat(p1.ctrlOut).isAt(12, 9.76);
        assertThat(p1.ctrlIn).isAt(12, 4.24);

        var p2 = sp.getAnchor(1);
        assertThat(p2).isAt(7, 12);
        assertThat(p2.ctrlOut).isAt(4.24, 12.00);
        assertThat(p2.ctrlIn).isAt(9.76, 12);

        var p3 = sp.getAnchor(2);
        assertThat(p3).isAt(2, 7);
        assertThat(p3.ctrlOut).isAt(2, 4.24);
        assertThat(p3.ctrlIn).isAt(2, 9.76);

        var p4 = sp.getAnchor(3);
        assertThat(p4).isAt(7, 2);
        assertThat(p4.ctrlOut).isAt(9.76, 2);
        assertThat(p4.ctrlIn).isAt(4.24, 2);
    }

    @Test
    void toPositiveRect_fromRectangle_whenWidthHeightPositive() {
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectOut = Shapes.toPositiveRect(rect);

        assertThat(rectOut).isEqualTo(rect);
    }

    @Test
    void toPositiveRect_fromRectangle_whenWidthNegative() {
        Rectangle rect = new Rectangle(30, 40, -10, 20);
        Rectangle rectExcepted = new Rectangle(20, 40, 10, 20);

        Rectangle rectOut = Shapes.toPositiveRect(rect);
        assertThat(rectOut).isEqualTo(rectExcepted);
    }

    @Test
    void toPositiveRect_fromRectangle_whenHeightNegative() {
        Rectangle rect = new Rectangle(30, 40, 10, -20);
        Rectangle rectExcepted = new Rectangle(30, 20, 10, 20);

        Rectangle rectOut = Shapes.toPositiveRect(rect);
        assertThat(rectOut).isEqualTo(rectExcepted);
    }

    @Test
    void toPositiveRect_fromRectangle_whenWidthHeightNegative() {
        Rectangle rect = new Rectangle(30, 40, -10, -20);
        Rectangle rectExcepted = new Rectangle(20, 20, 10, 20);

        Rectangle rectOut = Shapes.toPositiveRect(rect);
        assertThat(rectOut).isEqualTo(rectExcepted);
    }
}