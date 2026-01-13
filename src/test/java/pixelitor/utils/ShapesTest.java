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

package pixelitor.utils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.testutils.SegmentCounter;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.SubPath;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

/**
 * Unit tests for the {@link Shapes} utility class.
 */
@DisplayName("Shapes tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ShapesTest {
    // constant for approximating a circle with cubic Bézier curves
    private static final double KAPPA = 0.55228474983;

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

        double x = 2.0, y = 2.0, w = 10.0, h = 10.0;
        double radiusX = w / 2.0;
        double radiusY = h / 2.0;
        double centerX = x + radiusX;
        double centerY = y + radiusY;
        double ctrlOffsetX = radiusX * KAPPA;
        double ctrlOffsetY = radiusY * KAPPA;

        assertThat(subPath.getAnchor(0)) // right
            .isAt(centerX + radiusX, centerY)
            .typeIs(SYMMETRIC)
            .hasCtrlOutAt(centerX + radiusX, centerY + ctrlOffsetY)
            .hasCtrlInAt(centerX + radiusX, centerY - ctrlOffsetY);

        assertThat(subPath.getAnchor(1)) // bottom
            .isAt(centerX, centerY + radiusY)
            .hasCtrlOutAt(centerX - ctrlOffsetX, centerY + radiusY)
            .hasCtrlInAt(centerX + ctrlOffsetX, centerY + radiusY);

        assertThat(subPath.getAnchor(2)) // left
            .isAt(centerX - radiusX, centerY)
            .hasCtrlOutAt(centerX - radiusX, centerY - ctrlOffsetY)
            .hasCtrlInAt(centerX - radiusX, centerY + ctrlOffsetY);

        assertThat(subPath.getAnchor(3)) // top
            .isAt(centerX, centerY - radiusY)
            .hasCtrlOutAt(centerX + ctrlOffsetX, centerY - radiusY)
            .hasCtrlInAt(centerX - ctrlOffsetX, centerY - radiusY);
    }

    @Test
    void convertQuadCurve() {
        // create a shape with a quadratic curve
        Path2D input = new Path2D.Double();
        input.moveTo(0, 0);
        input.quadTo(10, 0, 10, 10);
        new SegmentCounter(input)
            .assertMoveToCount(1)
            .assertQuadToCount(1)
            .assertCubicToCount(0);

        Path path = Shapes.shapeToPath(input, view);
        SubPath subPath = path.getActiveSubpath();

        assertThat(subPath).numAnchorsIs(2);
        assertThat(subPath.getAnchor(0)).typeIs(CUSP);
        assertThat(subPath.getAnchor(1)).typeIs(CUSP);

        // the resulting shape is now cubic, not quadratic
        Shape finalShape = path.toImageSpaceShape();
        new SegmentCounter(finalShape)
            .assertMoveToCount(1)
            .assertQuadToCount(0)
            .assertCubicToCount(1);
    }

    @ParameterizedTest(name = "toPositiveRect: {0}")
    @MethodSource("provideRectanglesForNormalization")
    void toPositiveRect_normalizesRectangles(String caseName,
                                             int inX, int inY, int inW, int inH,
                                             int expX, int expY, int expW, int expH) {
        // test rectangle
        Rectangle inputRect = new Rectangle(inX, inY, inW, inH);
        Rectangle expectedRect = new Rectangle(expX, expY, expW, expH);

        // test the overload that takes a Rectangle
        assertThat(Shapes.toPositiveRect(inputRect)).isEqualTo(expectedRect);

        // test the overload that takes coordinates
        assertThat(Shapes.toPositiveRect(inX, inX + inW, inY, inY + inH)).isEqualTo(expectedRect);

        // test the overload that takes a Rectangle2D
        Rectangle2D inputRect2D = new Rectangle2D.Double(inX, inY, inW, inH);
        Rectangle2D expectedRect2D = new Rectangle2D.Double(expX, expY, expW, expH);

        assertThat(Shapes.toPositiveRect(inputRect2D)).isEqualTo(expectedRect2D);
    }

    private static Stream<Arguments> provideRectanglesForNormalization() {
        return Stream.of(
            arguments("positive w/h", 30, 40, 10, 20, 30, 40, 10, 20),
            arguments("negative width", 30, 40, -10, 20, 20, 40, 10, 20),
            arguments("negative height", 30, 40, 10, -20, 30, 20, 10, 20),
            arguments("negative w/h", 30, 40, -10, -20, 20, 20, 10, 20)
        );
    }

    @Test
    void toSvgPath_convertsRectangleCorrectly() {
        Shape rect = new Rectangle2D.Double(10, 20, 30, 40);
        String svgPath = Shapes.toSvgPath(rect);
        String expected = """
            M 10.000 20.000
            L 40.000 20.000
            L 40.000 60.000
            L 10.000 60.000
            L 10.000 20.000
            Z
            """;

        assertThat(svgPath).isEqualTo(expected);
    }

    @Test
    void pathsAreEqual() {
        Shape r1 = new Rectangle2D.Double(10, 10, 20, 20);
        Shape r2 = new Rectangle2D.Double(10, 10, 20, 20);
        Shape r3 = new Rectangle2D.Double(10, 10, 20, 21);
        Shape r4 = new Rectangle2D.Double(10.001, 10.001, 19.998, 19.998);

        assertThat(Shapes.pathsAreEqual(r1, r2, 0.0)).isTrue();
        assertThat(Shapes.pathsAreEqual(r1, r3, 0.0)).isFalse();
        assertThat(Shapes.pathsAreEqual(r1, r4, 0.0)).isFalse();
        assertThat(Shapes.pathsAreEqual(r1, r4, 0.01)).isTrue();
    }

    @Test
    void resizeToFit() {
        // 10x10 square
        Rectangle2D input = new Rectangle2D.Double(0, 0, 10, 10);

        // fit into 100x50 area with 0 margin
        Shape result = Shapes.resizeToFit(input, 100, 50, 0, 0, 0);
        Rectangle2D bounds = result.getBounds2D();

        // should be 50x50 (limited by height) and centered horizontally
        assertThat(bounds.getWidth()).isEqualTo(50.0);
        assertThat(bounds.getHeight()).isEqualTo(50.0);
        assertThat(bounds.getX()).isEqualTo(25.0); // (100 - 50) / 2
        assertThat(bounds.getY()).isEqualTo(0.0);
    }
}
