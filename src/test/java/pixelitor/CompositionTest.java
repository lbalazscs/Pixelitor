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

package pixelitor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pixelitor.Composition.LayerAdder;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;

import java.awt.Rectangle;

import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.Composition.LayerAdder.Position.BELLOW_ACTIVE;
import static pixelitor.TestHelper.assertHistoryEditsAre;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

public class CompositionTest {
    private Composition comp;

    @BeforeClass
    public static void setupClass() {
        Build.setTestingMode();
    }

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerComposition(true);
        assertThat(comp)
                .isNotDirty()
                .isNotEmpty()
                .hasName("Test")
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .activeLayerNameIs("layer 2")
                .doesNotHaveSelection()
                .firstLayerHasMask()
                .secondLayerHasMask();

        History.clear();
    }

    @Test
    public void test_addNewEmptyLayer() {
        assertThat(comp)
                .isNotDirty()
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        // add new layer bellow the active layer
        comp.addNewEmptyLayer("newLayer 1", true);

        assertThat(comp)
                .isDirty()
                .numLayersIs(3)
                .layerNamesAre("layer 1", "newLayer 1", "layer 2")
                .secondLayerIsActive();

        // add new layer above the active layer
        comp.addNewEmptyLayer("newLayer 2", false);

        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "newLayer 1", "newLayer 2", "layer 2")
                .thirdLayerIsActive();

        assertHistoryEditsAre("New Empty Layer", "New Empty Layer");

        // undo everything
        History.undo("New Empty Layer");
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "newLayer 1", "layer 2")
                .secondLayerIsActive();

        History.undo("New Empty Layer");
        assertThat(comp)
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        // redo everything
        History.redo("New Empty Layer");
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "newLayer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("New Empty Layer");
        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "newLayer 1", "newLayer 2", "layer 2")
                .thirdLayerIsActive();
    }

    @Test
    public void test_setActiveLayer() {
        assertThat(comp)
                .isNotDirty()
                .secondLayerIsActive();

        Layer firstLayer = comp.getLayer(0);
        comp.setActiveLayer(firstLayer, true, null);

        assertThat(comp)
                .isDirty()
                .activeLayerIs(firstLayer)
                .firstLayerIsActive();
        History.assertNumEditsIs(1);

        History.undo("Layer Selection Change");
        assertThat(comp).secondLayerIsActive();

        History.redo("Layer Selection Change");
        assertThat(comp).firstLayerIsActive();
    }

    @Test
    public void test_addLayerInInitMode() {
        assertThat(comp)
                .isNotDirty()
                .numLayersIs(2);

        ImageLayer newLayer = TestHelper.createImageLayer("layer 3", comp);
        comp.addLayerInInitMode(newLayer);

        assertThat(comp)
                .isNotDirty()  // still not dirty!
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer 2", "layer 3")
                .thirdLayerIsActive();
    }

    @Test
    public void testLayerAdder() {
        // add bellow active
        new LayerAdder(comp)
                .withHistory("bellow active")
                .atPosition(BELLOW_ACTIVE)
                .add(TestHelper.createImageLayer("layer A", comp));

        assertThat(comp)
                .isDirty()
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer A", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer A");

        // add above active
        new LayerAdder(comp)
                .withHistory("above active")
                .atPosition(ABOVE_ACTIVE)
                .add(TestHelper.createImageLayer("layer B", comp));

        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer B");

        // add to position 0
        new LayerAdder(comp)
                .withHistory("position 0")
                .atIndex(0)
                .add(TestHelper.createImageLayer("layer C", comp));

        assertThat(comp)
                .numLayersIs(5)
                .layerNamesAre("layer C", "layer 1", "layer A", "layer B", "layer 2")
                .firstLayerIsActive()
                .activeLayerNameIs("layer C");

        // add to position 2
        new LayerAdder(comp)
                .withHistory("position 2")
                .atIndex(2)
                .add(TestHelper.createImageLayer("layer D", comp));

        assertThat(comp)
                .numLayersIs(6)
                .layerNamesAre("layer C", "layer 1", "layer D", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer D");
        assertHistoryEditsAre(
                "bellow active", "above active", "position 0", "position 2");

        History.undo("position 2");
        assertThat(comp)
                .numLayersIs(5)
                .layerNamesAre("layer C", "layer 1", "layer A", "layer B", "layer 2")
                .firstLayerIsActive()
                .activeLayerNameIs("layer C");

        History.undo("position 0");
        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer B");

        History.undo("above active");
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer A", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer A");

        History.undo("bellow active");
        assertThat(comp)
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer 2");

        History.redo("bellow active");
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer A", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer A");

        History.redo("above active");
        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer B");

        History.redo("position 0");
        assertThat(comp)
                .numLayersIs(5)
                .layerNamesAre("layer C", "layer 1", "layer A", "layer B", "layer 2")
                .firstLayerIsActive()
                .activeLayerNameIs("layer C");

        History.redo("position 2");
        assertThat(comp)
                .numLayersIs(6)
                .layerNamesAre("layer C", "layer 1", "layer D", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer D");
    }

    @Test
    public void test_duplicateActiveLayer() {
        assertThat(comp)
                .isNotDirty()
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.duplicateActiveLayer();

        assertThat(comp)
                .isDirty()
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer 2", "layer 2 copy")
                .thirdLayerIsActive();
        History.assertNumEditsIs(1);

        History.undo("Duplicate Layer");
        assertThat(comp)
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Duplicate Layer");
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer 2", "layer 2 copy")
                .thirdLayerIsActive();
    }

    @Test
    public void test_flattenImage() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2");

        comp.flattenImage(false, true);

        assertThat(comp)
                .isDirty()
                .layerNamesAre("flattened");

        // there is no undo for flatten image
        History.assertNumEditsIs(0);
    }

    @Test
    public void test_mergeActiveLayerDown() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.mergeActiveLayerDown(false);

        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1");
        History.assertNumEditsIs(1);

        History.undo("Merge Down");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Merge Down");
        assertThat(comp)
                .layerNamesAre("layer 1");
    }

    @Test
    public void testMovingTheActiveLayer() {
        // check initial state
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.moveActiveLayerUp();
        // nothing changes as the active layer is already at the top
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();
        History.assertNumEditsIs(0);

        comp.moveActiveLayerDown();
        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.undo("Lower Layer");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Lower Layer");
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        comp.moveActiveLayerUp();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.undo("Raise Layer");
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.redo("Raise Layer");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.moveActiveLayerToBottom();
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.undo("Layer to Bottom");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Layer to Bottom");
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        comp.moveActiveLayerToTop();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.undo("Layer to Top");
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.redo("Layer to Top");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.changeLayerOrder(0, 1, true, null);
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.undo("Layer Order Change");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Layer Order Change");
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();
    }

    @Test
    public void testMoveLayerSelection() {
        // initial state
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.moveLayerSelectionDown(); // make the first layer active
        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1", "layer 2")
                .firstLayerIsActive();

        History.undo("Lower Layer Selection");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Lower Layer Selection");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .firstLayerIsActive();

        comp.moveLayerSelectionUp(); // make the second layer active
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.undo("Raise Layer Selection");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .firstLayerIsActive();

        History.redo("Raise Layer Selection");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();
    }

    @Test
    public void test_generateNewLayerName() {
        assertThat(comp.generateNewLayerName()).isEqualTo("layer 1");
        assertThat(comp.generateNewLayerName()).isEqualTo("layer 2");
        assertThat(comp.generateNewLayerName()).isEqualTo("layer 3");
        assertThat(comp)
                .numLayersIs(2) // didn't change
                .invariantIsOK();
    }

    @Test
    public void test_deleteActiveLayer() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.deleteActiveLayer(false, true);
        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1")
                .firstLayerIsActive();
        History.assertNumEditsIs(1);

        History.undo("Delete Layer");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Delete Layer");
        assertThat(comp)
                .layerNamesAre("layer 1")
                .firstLayerIsActive();
    }

    @Test
    public void test_deleteLayer() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        Layer layer2 = comp.getLayer(1);
        comp.deleteLayer(layer2, true, false);

        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1");
        History.assertNumEditsIs(1);

        History.undo("Delete Layer");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("Delete Layer");
        assertThat(comp)
                .layerNamesAre("layer 1");

        History.undo("Delete Layer");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        // now delete layer 1
        Layer layer1 = comp.getLayer(0);
        comp.setActiveLayer(layer1, true, null);
        comp.deleteLayer(layer1, true, false);

        assertThat(comp)
                .layerNamesAre("layer 2");

        History.undo("Delete Layer");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .firstLayerIsActive(); // the first because we have activated it

        History.redo("Delete Layer");
        assertThat(comp)
                .layerNamesAre("layer 2");
    }

    @Test
    public void test_addNewLayerFromComposite() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.addNewLayerFromComposite();

        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1", "layer 2", "Composite")
                .thirdLayerIsActive();
        History.assertNumEditsIs(1);

        History.undo("New Layer from Composite");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo("New Layer from Composite");
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2", "Composite")
                .thirdLayerIsActive();
    }

    @Test
    public void test_isActive() {
        Layer layer1 = comp.getLayer(0);
        Layer layer2 = comp.getLayer(1);

        assertThat(comp).activeLayerIs(layer2);
        assertThat(comp.isActive(layer1)).isFalse();
        assertThat(comp.isActive(layer2)).isTrue();

        comp.setActiveLayer(layer1);

        assertThat(comp).activeLayerIs(layer1).invariantIsOK();
        assertThat(comp.isActive(layer1)).isTrue();
        assertThat(comp.isActive(layer2)).isFalse();
    }

    @Test
    public void test_invertSelection() {
        assertThat(comp).doesNotHaveSelection();

        comp.invertSelection(); // nothing to invert yet
        History.assertNumEditsIs(0); // nothing happened

        // set a selection
        Rectangle originalSelectionRect = new Rectangle(3, 3, 4, 4);
        comp.setSelectionRef(new Selection(originalSelectionRect, comp.getIC()));

        assertThat(comp).hasSelection();
        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(originalSelectionRect);

        comp.invertSelection();

        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(comp.getCanvasImBounds()); // the whole canvas!
        History.assertNumEditsIs(1);

        History.undo("Invert Selection");
        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(originalSelectionRect);

        History.redo("Invert Selection");
        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(comp.getCanvasImBounds()); // the whole canvas!
    }

    @Test
    public void test_setSelectionFromShape() {
        assertThat(comp).doesNotHaveSelection();

        Rectangle rect = new Rectangle(3, 3, 5, 5);
        comp.setSelectionFromShape(rect);

        assertThat(comp)
                .hasSelection()
                .selectionShapeIs(rect)
                .invariantIsOK();
    }

    @Test
    public void test_dragFinished() {
        assertThat(comp).layerNamesAre("layer 1", "layer 2");

        Layer layer = comp.getLayer(0);
        comp.dragFinished(layer, 1);

        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .invariantIsOK();
    }

    @Test
    public void testTranslationWODuplicating() {
        testTranslation(false);
    }

    @Test
    public void testTranslationWithDuplicating() {
        testTranslation(true);
    }

    private void testTranslation(boolean makeDuplicateLayer) {
        // delete one layer in order to simplify
        comp.deleteLayer(comp.getActiveLayer(), true, false);

        assertThat(comp)
                .activeLayerTranslationIs(0, 0)
                .activeLayerAndMaskImageSizeIs(20, 10);
        History.assertNumEditsIs(1);

        // 1. direction south-east
        TestHelper.moveLayer(comp, makeDuplicateLayer, 2, 2);

        String[] expectedLayers = {"layer 1"};
        if (makeDuplicateLayer) {
            expectedLayers = new String[]{"layer 1", "layer 1 copy"};
        }

        assertThat(comp)
                .layerNamesAre(expectedLayers)
                // no translation because the image was
                // moved south-east, and enlarged
                .activeLayerTranslationIs(0, 0)
                .activeLayerAndMaskImageSizeIs(22, 12);
        History.assertNumEditsIs(makeDuplicateLayer ? 3 : 2);
        History.assertLastEditNameIs("Move Layer");

        // 2. direction north-west
        TestHelper.moveLayer(comp, makeDuplicateLayer, -2, -2);

        assertThat(comp)
                // this time we have a non-zero translation
                .activeLayerTranslationIs(-2, -2)
                // no need to enlarge the image again
                .activeLayerAndMaskImageSizeIs(22, 12);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(3);
        }

        // 3. direction north-west again
        TestHelper.moveLayer(comp, makeDuplicateLayer, -2, -2);

        assertThat(comp)
                // the translation increases
                .activeLayerTranslationIs(-4, -4)
                // the image needs to be enlarged now
                .activeLayerAndMaskImageSizeIs(24, 14);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(4);
        }

        // 4. direction north-east
        TestHelper.moveLayer(comp, makeDuplicateLayer, 2, -2);
        assertThat(comp)
                // the translation increases
                .activeLayerTranslationIs(-2, -6)
                // the image needs to be enlarged vertically
                .activeLayerAndMaskImageSizeIs(24, 16);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(5);
        }

        // 5. opposite movement: direction south-west
        TestHelper.moveLayer(comp, makeDuplicateLayer, -2, 2);

        if (makeDuplicateLayer) {
            expectedLayers = new String[]{"layer 1", "layer 1 copy",
                    "layer 1 copy 2", "layer 1 copy 3", "layer 1 copy 4", "layer 1 copy 5"};
        }
        assertThat(comp)
                .layerNamesAre(expectedLayers)
                // translation back to -4, -4
                .activeLayerTranslationIs(-4, -4)
                // no need to enlarge the image
                .activeLayerAndMaskImageSizeIs(24, 16);


        int numEdits;
        if (makeDuplicateLayer) {
            numEdits = 11; // 1 + 2*5
            assertHistoryEditsAre("Delete Layer",
                    "Duplicate Layer", "Move Layer",
                    "Duplicate Layer", "Move Layer",
                    "Duplicate Layer", "Move Layer",
                    "Duplicate Layer", "Move Layer",
                    "Duplicate Layer", "Move Layer");
        } else {
            numEdits = 6; // 1 initially + 5 movements
            assertHistoryEditsAre("Delete Layer",
                    "Move Layer", "Move Layer", "Move Layer", "Move Layer", "Move Layer");
        }
        History.assertNumEditsIs(numEdits);

        // undo everything except the first "Delete Layer"
        for (int i = 0; i < numEdits - 1; i++) {
            History.undo();
        }
        assertThat(comp)
                .activeLayerTranslationIs(0, 0)
                .activeLayerAndMaskImageSizeIs(20, 10);

        // redo everything
        for (int i = 0; i < numEdits - 1; i++) {
            History.redo();
        }
        assertThat(comp)
                .activeLayerTranslationIs(-4, -4)
                .activeLayerAndMaskImageSizeIs(24, 16);

        // now test "Layer to Canvas Size"
        comp.activeLayerToCanvasSize();
        assertThat(comp)
                .activeLayerTranslationIs(0, 0)
                .activeLayerAndMaskImageSizeIs(20, 10);

        History.undo("Layer to Canvas Size");
        assertThat(comp)
                .activeLayerTranslationIs(-4, -4)
                .activeLayerAndMaskImageSizeIs(24, 16);

        History.redo("Layer to Canvas Size");
        assertThat(comp)
                .activeLayerTranslationIs(0, 0)
                .activeLayerAndMaskImageSizeIs(20, 10);
    }

    @Test
    public void test_deselect() {
        assertThat(comp).doesNotHaveSelection();

        TestHelper.setStandardTestSelection(comp);

        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(TestHelper.getStandardTestSelectionShape());

        comp.deselect(true);

        assertThat(comp).doesNotHaveSelection();
        History.assertNumEditsIs(1);

        History.undo("Deselect");
        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(TestHelper.getStandardTestSelectionShape());

        History.redo("Deselect");
        assertThat(comp).doesNotHaveSelection();
    }

    @Test
    public void test_cropSelection() {
        TestHelper.setStandardTestSelection(comp);
        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(TestHelper.getStandardTestSelectionShape());

        comp.cropSelection(new Rectangle(2, 2, 4, 4));

        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(new Rectangle(2, 2, 2, 2));

        // There is no undo at this level
        History.assertNumEditsIs(0);
    }
}