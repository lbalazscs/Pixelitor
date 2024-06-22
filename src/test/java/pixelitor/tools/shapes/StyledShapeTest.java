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

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("styled shape tests")
@TestMethodOrder(MethodOrderer.Random.class)
class StyledShapeTest {
    private View view;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
        Tools.SHAPES.resetInitialState();
    }

    @BeforeEach
    void beforeEachTest() {
        Composition comp = TestHelper.createEmptyComp("StyledShapeTest");
        view = comp.getView();
    }

    @ParameterizedTest
    @EnumSource(ShapeType.class)
    void createBox(ShapeType shapeType) {
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
}