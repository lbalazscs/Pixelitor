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

package pixelitor.utils;

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.testutils.SegmentCounter;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.SubPath;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

@DisplayName("Shapes tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ShapesTest {
    private View view;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        var comp = TestHelper.createEmptyComp("ShapesTest");
        view = TestHelper.setupMockViewFor(comp);
    }

    @Test
    void convertRectangle() {
        checkShapeToPathRoundtrip(
            new Rectangle2D.Double(2, 2, 10, 10),
            ShapesTest::checkRectangleShape,
            ShapesTest::checkRectanglePath
        );
    }

    @Test
    void convertEllipse() {
        checkShapeToPathRoundtrip(
            new Ellipse2D.Double(2, 2, 10, 10),
            ShapesTest::checkEllipseShape,
            ShapesTest::checkEllipsePath
        );
    }

    private void checkShapeToPathRoundtrip(Shape initialShape,
                                           Consumer<Shape> shapeChecker,
                                           Consumer<SubPath> pathChecker) {
        // check the initial shape
        shapeChecker.accept(initialShape);

        // convert to a path and check it
        Path path = Shapes.shapeToPath(initialShape, view);
        pathChecker.accept(path.getActiveSubpath());

        // convert back to a shape and check it again
        Shape finalShape = path.toImageSpaceShape();
        shapeChecker.accept(finalShape);

        // perform a second roundtrip to ensure stability
        path = Shapes.shapeToPath(finalShape, view);
        pathChecker.accept(path.getActiveSubpath());
    }

    private static void checkRectangleShape(Shape shape) {
        new SegmentCounter(shape)
            .assertMoveToCount(1)
            .assertLineToCount(4)
            .assertQuadToCount(0)
            .assertCubicToCount(0)
            .assertPathCloseCount(1);
    }

    private static void checkRectanglePath(SubPath subPath) {
        assertThat(subPath)
            .numAnchorsIs(4)
            .isClosed();

        assertThat(subPath.getAnchor(0))
            .isAt(2, 2)
            .bothControlsAreRetracted();

        assertThat(subPath.getAnchor(1))
            .isAt(12, 2)
            .bothControlsAreRetracted();

        assertThat(subPath.getAnchor(2))
            .isAt(12, 12)
            .bothControlsAreRetracted();

        assertThat(subPath.getAnchor(3))
            .isAt(2, 12)
            .bothControlsAreRetracted();
    }

    private static void checkEllipseShape(Shape shape) {
        new SegmentCounter(shape)
            .assertMoveToCount(1)
            .assertLineToCount(0)
            .assertQuadToCount(0)
            .assertCubicToCount(4)
            .assertPathCloseCount(1);
    }

    private static void checkEllipsePath(SubPath subPath) {
        assertThat(subPath)
            .numAnchorsIs(4)
            .isClosed();

        var p1 = subPath.getAnchor(0);
        assertThat(p1)
            .isAt(12, 7)
            .typeIs(SYMMETRIC);
        assertThat(p1.ctrlOut).isAt(12, 9.76);
        assertThat(p1.ctrlIn).isAt(12, 4.24);

        var p2 = subPath.getAnchor(1);
        assertThat(p2).isAt(7, 12);
        assertThat(p2.ctrlOut).isAt(4.24, 12.00);
        assertThat(p2.ctrlIn).isAt(9.76, 12);

        var p3 = subPath.getAnchor(2);
        assertThat(p3).isAt(2, 7);
        assertThat(p3.ctrlOut).isAt(2, 4.24);
        assertThat(p3.ctrlIn).isAt(2, 9.76);

        var p4 = subPath.getAnchor(3);
        assertThat(p4).isAt(7, 2);
        assertThat(p4.ctrlOut).isAt(9.76, 2);
        assertThat(p4.ctrlIn).isAt(4.24, 2);
    }

    @Test
    void toPositiveRect_fromRectangle_whenWidthHeightPositive() {
        Rectangle input = new Rectangle(30, 40, 10, 20);
        Rectangle output = Shapes.toPositiveRect(input);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void toPositiveRect_fromRectangle_whenWidthNegative() {
        Rectangle input = new Rectangle(30, 40, -10, 20);
        Rectangle expectedOutput = new Rectangle(20, 40, 10, 20);

        Rectangle output = Shapes.toPositiveRect(input);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void toPositiveRect_fromRectangle_whenHeightNegative() {
        Rectangle input = new Rectangle(30, 40, 10, -20);
        Rectangle expectedOutput = new Rectangle(30, 20, 10, 20);

        Rectangle output = Shapes.toPositiveRect(input);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void toPositiveRect_fromRectangle_whenWidthHeightNegative() {
        Rectangle input = new Rectangle(30, 40, -10, -20);
        Rectangle expectedOutput = new Rectangle(20, 20, 10, 20);

        Rectangle output = Shapes.toPositiveRect(input);
        assertThat(output).isEqualTo(expectedOutput);
    }
}
