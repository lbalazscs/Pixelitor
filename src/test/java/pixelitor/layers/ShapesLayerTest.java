/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.compactions.Resize;
import pixelitor.gui.View;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.shapes.StyledShape;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.Drag;

import static org.mockito.Mockito.mock;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("shape layer tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ShapesLayerTest {
    private Composition comp;
    private View view;
    private ShapesLayer layer;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
        Tools.SHAPES.setButton(mock(ToolButton.class));
    }

    @BeforeEach
    void beforeEachTest() {
        comp = TestHelper.createEmptyComp();
        layer = TestHelper.createEmptyShapesLayer(comp, "test");
        comp.addLayerInInitMode(layer);
        view = comp.getView();
        StyledShape styledShape = new StyledShape(Tools.SHAPES);
        Drag drag = new Drag(0, 0, 10, 10);
        drag.calcCoCoords(view);
        styledShape.updateFromDrag(drag, Tools.SHAPES);
        assert styledShape.hasOrigDrag();
        TransformBox box = styledShape.createBox(view);
        layer.setStyledShape(styledShape);
        layer.setTransformBox(box);

        checkOrigBoxPosition(box);
    }

    @Test
    void duplicate() {
        ShapesLayer duplicate = (ShapesLayer) layer.duplicate(false, true);
        TransformBox duplicateBox = duplicate.getTransformBox();
        checkOrigBoxPosition(duplicateBox);
    }

    @Test
    void resize() {
        comp.addLayerInInitMode(layer.duplicate(false, false));

        Composition smallComp = resizeComp(comp, 10, 5);
        ShapesLayer smallLayer1 = (ShapesLayer) smallComp.getLayer(0);
        ShapesLayer smallLayer2 = (ShapesLayer) smallComp.getLayer(1);
        checkHalfSizeBoxPosition(smallLayer1.getTransformBox());
        checkHalfSizeBoxPosition(smallLayer2.getTransformBox());

        // resize back to the original size
        Composition bigComp = resizeComp(smallComp, TestHelper.TEST_WIDTH, TestHelper.TEST_HEIGHT);
        ShapesLayer bigLayer1 = (ShapesLayer) bigComp.getLayer(0);
        ShapesLayer bigLayer2 = (ShapesLayer) bigComp.getLayer(1);
        checkOrigBoxPosition(bigLayer1.getTransformBox());
        checkOrigBoxPosition(bigLayer2.getTransformBox());
    }

    private Composition resizeComp(Composition comp, int testWidth, int testHeight) {
        return new Resize(testWidth, testHeight, false)
            .process(comp)
            .join();
    }

    private static void checkOrigBoxPosition(TransformBox box) {
        assertThat(box).handleImPosIs(TransformBox::getNW, 0, 0);
        assertThat(box).handleImPosIs(TransformBox::getNE, 10, 0);
        assertThat(box).handleImPosIs(TransformBox::getSW, 0, 10);
        assertThat(box).handleImPosIs(TransformBox::getSE, 10, 10);
    }

    private static void checkHalfSizeBoxPosition(TransformBox box) {
        assertThat(box).handleImPosIs(TransformBox::getNW, 0, 0);
        assertThat(box).handleImPosIs(TransformBox::getNE, 5, 0);
        assertThat(box).handleImPosIs(TransformBox::getSW, 0, 5);
        assertThat(box).handleImPosIs(TransformBox::getSE, 5, 5);
    }
}