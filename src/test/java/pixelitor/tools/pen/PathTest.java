/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.pen;

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.utils.Shapes;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Path tests")
@TestMethodOrder(MethodOrderer.Random.class)
class PathTest {
    private View view;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        var comp = TestHelper.createMockComp("PathTest");
        view = comp.getView();
    }

    @Test
    void deletingSubPathPoints() {
        History.clear();

        var shape = new Rectangle(10, 10, 100, 100);
        Path path = Shapes.shapeToPath(shape, view);
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath).numAnchorsIs(4);
        subpath.getAnchor(3).delete(); // delete last
        subpath.getAnchor(2).delete(); // delete last
        subpath.getAnchor(0).delete(); // delete first
        subpath.getAnchor(0).delete(); // delete first (and last)
        assertThat(path.getActiveSubpath()).numAnchorsIs(0);

        History.assertNumEditsIs(4);
        History.undo("Delete Anchor Point");
        History.undo("Delete Anchor Point");
        // check the active subpath because the undo replaced
        // the subpath reference in the path
        assertThat(path.getActiveSubpath()).numAnchorsIs(2);

        History.redo("Delete Anchor Point");
        History.redo("Delete Anchor Point");
        assertThat(path.getActiveSubpath()).numAnchorsIs(0);
    }

    @Test
    void conversionsForRectangle() {
        testConversionsFor(
            new Rectangle(20, 20, 40, 10));
    }

    @Test
    void conversionsForEllipse() {
        testConversionsFor(
            new Ellipse2D.Double(20, 20, 40, 10));
    }

    @Test
    void transform() {
        Rectangle shape = new Rectangle(10, 10, 100, 100);
        Path path = Shapes.shapeToPath(shape, view);
        SubPath subpath = path.getActiveSubpath();
        assertThat(subpath).firstAnchorIsAt(10, 10);

        subpath.saveImTransformRefPoints(); // the ref point for the first anchor is 10, 10

        var at = AffineTransform.getTranslateInstance(20, 10);
        subpath.imTransform(at);
        assertThat(subpath).firstAnchorIsAt(30, 20);

        at = AffineTransform.getTranslateInstance(10, 20);
        subpath.imTransform(at);
        assertThat(subpath).firstAnchorIsAt(20, 30);
    }

    private void testConversionsFor(Shape shape) {
        Path path = Shapes.shapeToPath(shape, view);
        Path copy = path.deepCopy(view.getComp());
        Shape convertedShape = copy.toImageSpaceShape();
        assertThat(Shapes.pathIteratorIsEqual(shape, convertedShape, 0.01)).isTrue();
    }
}