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

package pixelitor.tools.shapes;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.tools.Tools;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.Drag;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("styled shape tests")
@TestMethodOrder(MethodOrderer.Random.class)
class StyledShapeTest {
    private View view;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
        Tools.SHAPES.reset();
    }

    @BeforeEach
    void beforeEachTest() {
        Composition comp = TestHelper.createEmptyComp("StyledShapeTest");
        view = comp.getView();
    }

    @ParameterizedTest
    @EnumSource(ShapeType.class)
    void testTransformBoxCreation(ShapeType shapeType) {
        Tools.SHAPES.setSelectedType(shapeType);
        StyledShape styledShape = new StyledShape(Tools.SHAPES);

        Drag drag;
        if (shapeType.isDirectional()) {
            drag = new Drag(0, 50, 100, 50);
        } else {
            drag = new Drag(0, 0, 100, 100);
        }

        styledShape.updateFromDrag(drag, false, false);
        TransformBox box = styledShape.createBox(view);

        if (shapeType.isDirectional()) {
            assertThat(box)
                .handleImPosIs(TransformBox::getNW, 0, 15)
                .handleImPosIs(TransformBox::getSE, 100, 85);
        } else {
            assertThat(box)
                .handleImPosIs(TransformBox::getNW, 0, 0)
                .handleImPosIs(TransformBox::getSE, 100, 100);
        }
    }

    @Test
    void clickingDoesntCreateBox() {
        Tools.SHAPES.setSelectedType(ShapeType.RECTANGLE);
        assert !Tools.SHAPES.hasBox();

        StyledShape styledShape = new StyledShape(Tools.SHAPES);
        // a drag that is just a click
        Drag drag = new Drag(50, 50, 50, 50);

        styledShape.updateFromDrag(drag, false, false);
        assert !Tools.SHAPES.hasBox();
    }

    @Test
    void altDragExpandsFromCenter() {
        Tools.SHAPES.setSelectedType(ShapeType.RECTANGLE);
        StyledShape styledShape = new StyledShape(Tools.SHAPES);
        // drag from center (50,50) to (75,75)
        Drag drag = new Drag(50, 50, 75, 75);

        // altDown is true
        styledShape.updateFromDrag(drag, true, false);

        // the shape's bounds should be as if dragged from (25,25) to (75,75)
        Rectangle2D bounds = styledShape.getShape().getBounds2D();
        assertThat(bounds.getX()).isEqualTo(25);
        assertThat(bounds.getY()).isEqualTo(25);
        assertThat(bounds.getWidth()).isEqualTo(50);
        assertThat(bounds.getHeight()).isEqualTo(50);
    }

    @Test
    void imTransformAppliesTransformToShape() {
        Tools.SHAPES.setSelectedType(ShapeType.ELLIPSE);
        StyledShape styledShape = new StyledShape(Tools.SHAPES);
        Drag drag = new Drag(0, 0, 100, 100);
        styledShape.updateFromDrag(drag, false, false);

        // create a simple translation transform
        AffineTransform translation = AffineTransform.getTranslateInstance(50, 25);
        styledShape.imTransform(translation);

        Rectangle2D bounds = styledShape.getShape().getBounds2D();
        assertThat(bounds.getX()).isEqualTo(50);
        assertThat(bounds.getY()).isEqualTo(25);
        assertThat(bounds.getWidth()).isEqualTo(100);
        assertThat(bounds.getHeight()).isEqualTo(100);
    }
}
