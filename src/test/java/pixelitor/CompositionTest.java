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
import org.junit.Test;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;

import java.awt.Rectangle;
import java.awt.Shape;

import static org.junit.Assert.assertSame;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

public class CompositionTest {
    private Composition comp;

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

        History.setUndoLevels(15);
        History.clear();
    }

    @Test
    public void testAddNewEmptyLayer() {
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

        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("New Empty Layer");

        // add new layer above the active layer
        comp.addNewEmptyLayer("newLayer 2", false);

        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "newLayer 1", "newLayer 2", "layer 2")
                .thirdLayerIsActive();
        History.assertNumEditsIs(2);
        History.assertLastEditNameIs("New Empty Layer");

        History.undo();
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "newLayer 1", "layer 2")
                .secondLayerIsActive();

        History.undo();
        assertThat(comp)
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "newLayer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "newLayer 1", "newLayer 2", "layer 2")
                .thirdLayerIsActive();
    }

    @Test
    public void testSetActiveLayer() {
        assertThat(comp)
                .isNotDirty()
                .secondLayerIsActive();

        Layer firstLayer = comp.getLayer(0);
        comp.setActiveLayer(firstLayer, true);

        assertSame(firstLayer, comp.getActiveLayer());
        assertThat(comp)
                .isDirty()
                .firstLayerIsActive();
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Layer Selection Change");

        History.undo();
        assertThat(comp).secondLayerIsActive();

        History.redo();
        assertThat(comp).firstLayerIsActive();
    }

    @Test
    public void testAddLayerNoGUI() {
        assertThat(comp)
                .isNotDirty()
                .numLayersIs(2);

        ImageLayer newLayer = TestHelper.createImageLayer("layer 3", comp);
        comp.addLayerNoGUI(newLayer);

        assertThat(comp)
                .isNotDirty()  // still not dirty!
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer 2", "layer 3")
                .thirdLayerIsActive();
    }

    @Test
    public void testAddLayer() {
        // add bellow active
        comp.addLayer(TestHelper.createImageLayer("layer A", comp),
                true, "New Test Layer", true, true);

        assertThat(comp)
                .isDirty()
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer A", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer A");

        // add above active
        comp.addLayer(TestHelper.createImageLayer("layer B", comp),
                true, "New Test Layer", true, false);

        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer B");

        // add to position 0
        comp.addLayer(TestHelper.createImageLayer("layer C", comp),
                true, "New Test Layer", true, 0);

        assertThat(comp)
                .numLayersIs(5)
                .layerNamesAre("layer C", "layer 1", "layer A", "layer B", "layer 2")
                .firstLayerIsActive()
                .activeLayerNameIs("layer C");

        // add to position 2
        comp.addLayer(TestHelper.createImageLayer("layer D", comp),
                true, "New Test Layer", true, 2);

        assertThat(comp)
                .numLayersIs(6)
                .layerNamesAre("layer C", "layer 1", "layer D", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer D");
        History.assertNumEditsIs(4);
        History.assertLastEditNameIs("New Test Layer");

        History.undo();
        assertThat(comp)
                .numLayersIs(5)
                .layerNamesAre("layer C", "layer 1", "layer A", "layer B", "layer 2")
                .firstLayerIsActive()
                .activeLayerNameIs("layer C");

        History.undo();
        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer B");

        History.undo();
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer A", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer A");

        History.undo();
        assertThat(comp)
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer 2");

        History.redo();
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer A", "layer 2")
                .secondLayerIsActive()
                .activeLayerNameIs("layer A");

        History.redo();
        assertThat(comp)
                .numLayersIs(4)
                .layerNamesAre("layer 1", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer B");

        History.redo();
        assertThat(comp)
                .numLayersIs(5)
                .layerNamesAre("layer C", "layer 1", "layer A", "layer B", "layer 2")
                .firstLayerIsActive()
                .activeLayerNameIs("layer C");

        History.redo();
        assertThat(comp)
                .numLayersIs(6)
                .layerNamesAre("layer C", "layer 1", "layer D", "layer A", "layer B", "layer 2")
                .thirdLayerIsActive()
                .activeLayerNameIs("layer D");
    }

    @Test
    public void testDuplicateLayer() {
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
        History.assertLastEditNameIs("Duplicate Layer");

        History.undo();
        assertThat(comp)
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .numLayersIs(3)
                .layerNamesAre("layer 1", "layer 2", "layer 2 copy")
                .thirdLayerIsActive();
    }

    @Test
    public void testGetCanvas() {
        Canvas canvas = comp.getCanvas();
        assertThat(canvas)
                .isNotNull()
                .hasImWidth(20)
                .hasImHeight(10);

        comp.checkInvariant();
    }

    @Test
    public void testIsEmpty() {
        boolean empty = comp.isEmpty();
        assertThat(empty).isFalse();

        comp.checkInvariant();
    }

    @Test
    public void testFlattenImage() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2");

        comp.flattenImage(false, true);

        assertThat(comp)
                .isDirty()
                .layerNamesAre("flattened");

        // there is no undo for flatten image
    }

    @Test
    public void testMergeDown() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.mergeActiveLayerDown(false);

        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1");
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Merge Down");

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1");
    }

    @Test
    public void testMovingActiveLayer() {
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

        comp.moveActiveLayerDown();
        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        comp.moveActiveLayerUp();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.moveActiveLayerToBottom();
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        comp.moveActiveLayerToTop();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.changeLayerOrder(0, 1, true);
        assertThat(comp)
                .layerNamesAre("layer 2", "layer 1")
                .firstLayerIsActive();

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
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

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .firstLayerIsActive();

        comp.moveLayerSelectionUp(); // make the second layer active
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .firstLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();
    }

    @Test
    public void testGenerateNewLayerName() {
        String newLayerName = comp.generateNewLayerName();
        assertThat(newLayerName).isEqualTo("layer 1");
        comp.checkInvariant();
    }

    @Test
    public void testGetCanvasBounds() {
        Rectangle bounds = comp.getCanvasImBounds();

        assertThat(bounds)
                .isNotNull()
                .isEqualToComparingFieldByField(
                        new Rectangle(0, 0, 20, 10));

        comp.checkInvariant();
    }

    @Test
    public void testDeleteActiveLayer() {
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
        History.assertLastEditNameIs("Delete Layer");

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1")
                .firstLayerIsActive();
    }

    @Test
    public void testDeleteLayer() {
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
        History.assertLastEditNameIs("Delete Layer");

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1");

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        // now delete layer 1
        Layer layer1 = comp.getLayer(0);
        comp.setActiveLayer(layer1, true);
        comp.deleteLayer(layer1, true, false);

        assertThat(comp)
                .layerNamesAre("layer 2");

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .firstLayerIsActive(); // the first because we have activated it

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 2");
    }

    @Test
    public void testAddNewLayerFromComposite() {
        assertThat(comp)
                .isNotDirty()
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        comp.addNewLayerFromComposite("composite layer");

        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1", "layer 2", "composite layer")
                .thirdLayerIsActive();
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("New Layer from Composite");

        History.undo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2")
                .secondLayerIsActive();

        History.redo();
        assertThat(comp)
                .layerNamesAre("layer 1", "layer 2", "composite layer")
                .thirdLayerIsActive();
    }

    @Test
    public void testIsActiveLayer() {
        Layer layer1 = comp.getLayer(0);
        Layer layer2 = comp.getLayer(1);

        assertThat(comp.isActiveLayer(layer1)).isFalse();
        assertThat(comp.isActiveLayer(layer2)).isTrue();
        comp.checkInvariant();

        comp.setActiveLayer(layer1, true);

        assertThat(comp.isActiveLayer(layer1)).isTrue();
        assertThat(comp.isActiveLayer(layer2)).isFalse();
        comp.checkInvariant();
    }

    @Test
    public void testInvertSelection() {
        assertThat(comp).doesNotHaveSelection();

        comp.invertSelection(); // nothing to invert yet

        comp.checkInvariant();
        History.assertNumEditsIs(0); // nothing happened

        // set a selection
        Rectangle originalSelectionRect = new Rectangle(3, 3, 4, 4);
        Selection selection = new Selection(originalSelectionRect, comp.getIC());
        comp.setNewSelection(selection);

        assertThat(comp).hasSelection();
        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(originalSelectionRect);

        comp.invertSelection();

        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(comp.getCanvasImBounds()); // the whole canvas!
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Invert Selection");

        History.undo();
        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(originalSelectionRect);

        History.redo();
        assertThat(comp.getSelection())
                .isNotNull()
                .hasShapeBounds(comp.getCanvasImBounds()); // the whole canvas!
    }

    @Test
    public void testCreateSelectionFromShape() {
        assertThat(comp).doesNotHaveSelection();

        Rectangle rect = new Rectangle(3, 3, 5, 5);
        comp.createSelectionFromShape(rect);

        comp.checkInvariant();
        assertThat(comp).hasSelection();

        Shape shape = comp.getSelection().getShape();
        assertThat(shape).isEqualTo(rect);
    }

    @Test
    public void testDragFinished() {
        Layer layer = comp.getLayer(0);
        comp.dragFinished(layer, 1);
        comp.checkInvariant();
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
        // delete one layer so that we have undo
        comp.deleteLayer(comp.getActiveLayer(), true, false);

        assertThat(comp)
                .isDirty()
                .layerNamesAre("layer 1")
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
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(2);
        }

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

        int numEdits = 6; // 1 initially + 5 movements
        if (makeDuplicateLayer) {
            numEdits = 11; // 1 + 2*5
        }
        History.assertNumEditsIs(numEdits);

        // undo everything
        for (int i = 0; i < numEdits; i++) {
            History.undo();
        }
        assertThat(comp)
                .activeLayerTranslationIs(0, 0)
                .activeLayerAndMaskImageSizeIs(20, 10);

        // redo everything
        for (int i = 0; i < numEdits; i++) {
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

        History.undo();
        assertThat(comp)
                .activeLayerTranslationIs(-4, -4)
                .activeLayerAndMaskImageSizeIs(24, 16);

        History.redo();
        assertThat(comp)
                .activeLayerTranslationIs(0, 0)
                .activeLayerAndMaskImageSizeIs(20, 10);
    }

    @Test
    public void testDeselect() {
        assertThat(comp).doesNotHaveSelection();

        TestHelper.setStandardTestSelection(comp);

        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(TestHelper.getStandardTestSelectionShape());

        comp.deselect(true);

        assertThat(comp).doesNotHaveSelection();
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Deselect");

        History.undo();
        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(TestHelper.getStandardTestSelectionShape());

        History.redo();
        assertThat(comp).doesNotHaveSelection();
    }

    @Test
    public void testCropSelection() {
        TestHelper.setStandardTestSelection(comp);
        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(TestHelper.getStandardTestSelectionShape());

        comp.cropSelection(new Rectangle(2, 2, 4, 4));

        assertThat(comp)
                .hasSelection()
                .selectionBoundsIs(new Rectangle(2, 2, 2, 2));
    }

    @Test
    public void testSimpleMethods() {
        assertThat(comp.getActiveLayerIndex()).isEqualTo(1);
        assertThat(comp.activeIsDrawable()).isTrue();

        assertThat(comp.getName()).isEqualTo("Test");
        comp.setName("New Name");
        assertThat(comp.getName()).isEqualTo("New Name");
    }
}