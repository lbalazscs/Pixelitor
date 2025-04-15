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

package pixelitor;

import org.junit.jupiter.api.*;
import pixelitor.compactions.Crop;
import pixelitor.history.History;
import pixelitor.layers.Layer;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import static pixelitor.TestHelper.assertHistoryEditsAre;
import static pixelitor.TestHelper.createEmptyImageLayer;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.layers.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerAdder.Position.BELOW_ACTIVE;

@DisplayName("Composition tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CompositionTest {
    private Composition comp;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        comp = TestHelper.createComp("CompositionTest", 2, true);
        assertThat(comp)
            .isNotDirty()
            .hasName("CompositionTest")
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .activeLayerNameIs("layer 2")
            .doesNotHaveSelection()
            .firstLayerHasMask()
            .secondLayerHasMask()
            .allLayerUIsAreOK();

        History.clear();
    }

    @Test
    void addNewEmptyImageLayer() {
        assertThat(comp)
            .isNotDirty()
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        // add new layer below the active layer
        comp.addNewEmptyImageLayer("newLayer 1", true);

        comp.getActiveLayer().createUI();
        assertThat(comp)
            .isDirty()
            .numLayersIs(3)
            .layerNamesAre("layer 1", "newLayer 1", "layer 2")
            .secondLayerIsActive();

        // add new layer above the active layer
        comp.addNewEmptyImageLayer("newLayer 2", false);

        comp.getActiveLayer().createUI();
        assertThat(comp)
            .numLayersIs(4)
            .layerNamesAre("layer 1", "newLayer 1", "newLayer 2", "layer 2")
            .thirdLayerIsActive()
            .allLayerUIsAreOK();

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
    void setActiveLayer() {
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
    void addLayerWithoutUI() {
        assertThat(comp)
            .isNotDirty()
            .numLayersIs(2);

        comp.addLayerWithoutUI(createEmptyImageLayer(comp, "layer 3"));

        assertThat(comp)
            .isNotDirty()  // still not dirty!
            .numLayersIs(3)
            .layerNamesAre("layer 1", "layer 2", "layer 3")
            .thirdLayerIsActive();
    }

    @Test
    void layerAdder() {
        // add below active
        comp.adder()
            .withHistory("below active")
            .atPosition(BELOW_ACTIVE)
            .add(createEmptyImageLayer(comp, "layer A"));

        assertThat(comp)
            .isDirty()
            .numLayersIs(3)
            .layerNamesAre("layer 1", "layer A", "layer 2")
            .secondLayerIsActive()
            .activeLayerNameIs("layer A");

        // add above active
        comp.adder()
            .withHistory("above active")
            .atPosition(ABOVE_ACTIVE)
            .add(createEmptyImageLayer(comp, "layer B"));

        assertThat(comp)
            .numLayersIs(4)
            .layerNamesAre("layer 1", "layer A", "layer B", "layer 2")
            .thirdLayerIsActive()
            .activeLayerNameIs("layer B");

        // add to position 0
        comp.adder()
            .withHistory("position 0")
            .atIndex(0)
            .add(createEmptyImageLayer(comp, "layer C"));

        assertThat(comp)
            .numLayersIs(5)
            .layerNamesAre("layer C", "layer 1", "layer A", "layer B", "layer 2")
            .firstLayerIsActive()
            .activeLayerNameIs("layer C");

        // add to position 2
        comp.adder()
            .withHistory("position 2")
            .atIndex(2)
            .add(createEmptyImageLayer(comp, "layer D"));

        assertThat(comp)
            .numLayersIs(6)
            .layerNamesAre("layer C", "layer 1", "layer D", "layer A", "layer B", "layer 2")
            .thirdLayerIsActive()
            .activeLayerNameIs("layer D");
        assertHistoryEditsAre(
            "below active", "above active", "position 0", "position 2");

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

        History.undo("below active");
        assertThat(comp)
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive()
            .activeLayerNameIs("layer 2");

        History.redo("below active");
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
    void duplicateActiveLayer() {
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
    void flattenImage() {
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2");

        comp.flattenImage();

        assertThat(comp)
            .isDirty()
            .layerNamesAre("flattened");

        // there is no undo for flatten image
        History.assertNumEditsIs(0);
    }

    @Test
    void mergeActiveLayerDown() {
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        comp.mergeActiveLayerDown();

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
    @DisplayName("move the active layer")
    void moveActiveLayer() {
        // check initial state
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        comp.getActiveHolder().reorderActiveLayer(true);
        // nothing changes as the active layer is already at the top
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();
        History.assertNumEditsIs(0);

        comp.getActiveHolder().reorderActiveLayer(false);
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

        comp.getActiveHolder().reorderActiveLayer(true);
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

        comp.getActiveHolder().moveActiveLayerToBottom();
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

        comp.getActiveHolder().moveActiveLayerToTop();
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

        comp.reorderLayer(0, 1, true, null);
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
    @DisplayName("lower/raise layer selection")
    void moveLayerSelection() {
        // initial state
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        comp.getActiveHolder().selectLayerBelow(); // make the first layer active
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

        comp.getActiveHolder().selectLayerAbove(); // make the second layer active
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
    void generateNewLayerName() {
        assertThat(comp.generateNewLayerName()).isEqualTo("layer 1");
        assertThat(comp.generateNewLayerName()).isEqualTo("layer 2");
        assertThat(comp.generateNewLayerName()).isEqualTo("layer 3");
        assertThat(comp)
            .numLayersIs(2) // didn't change
            .invariantsAreOK();
    }

    @Test
    void deleteActiveLayer() {
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        comp.deleteActiveLayer(true);
        assertThat(comp)
            .isDirty()
            .layerNamesAre("layer 1")
            .firstLayerIsActive();
        History.assertNumEditsIs(1);

        History.undo("Delete layer 2");
        assertThat(comp)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        History.redo("Delete layer 2");
        assertThat(comp)
            .layerNamesAre("layer 1")
            .firstLayerIsActive();
    }

    @Test
    void deleteLayer() {
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        Layer layer2 = comp.getLayer(1);
        comp.deleteLayer(layer2, true);

        assertThat(comp)
            .isDirty()
            .layerNamesAre("layer 1");
        History.assertNumEditsIs(1);

        History.undo("Delete layer 2");
        assertThat(comp)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        History.redo("Delete layer 2");
        assertThat(comp)
            .layerNamesAre("layer 1");

        History.undo("Delete layer 2");
        assertThat(comp)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        // now delete layer 1
        Layer layer1 = comp.getLayer(0);
        comp.setActiveLayer(layer1, true, null);
        comp.deleteLayer(layer1, true);

        assertThat(comp)
            .layerNamesAre("layer 2");

        History.undo("Delete layer 1");
        assertThat(comp)
            .layerNamesAre("layer 1", "layer 2")
            .firstLayerIsActive(); // the first because we have activated it

        History.redo("Delete layer 1");
        assertThat(comp)
            .layerNamesAre("layer 2");
    }

    @Test
    void addNewLayerFromVisible() {
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        comp.addNewLayerFromVisible();

        assertThat(comp)
            .isDirty()
            .layerNamesAre("layer 1", "layer 2", "Composite")
            .thirdLayerIsActive();
        History.assertNumEditsIs(1);

        History.undo("New Layer from Visible");
        assertThat(comp)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        History.redo("New Layer from Visible");
        assertThat(comp)
            .layerNamesAre("layer 1", "layer 2", "Composite")
            .thirdLayerIsActive();
    }

    @Test
    void isActive() {
        Layer layer1 = comp.getLayer(0);
        Layer layer2 = comp.getLayer(1);

        assertThat(comp).activeLayerIs(layer2);
        assertThat(comp.isActiveLayer(layer1)).isFalse();
        assertThat(comp.isActiveLayer(layer2)).isTrue();

        comp.setActiveLayer(layer1);

        assertThat(comp).activeLayerIs(layer1).invariantsAreOK();
        assertThat(comp.isActiveLayer(layer1)).isTrue();
        assertThat(comp.isActiveLayer(layer2)).isFalse();
    }

    @Test
    void invertSelection() {
        assertThat(comp).doesNotHaveSelection();

        // set a selection
        Rectangle2D origSelRect = new Rectangle2D.Double(3, 3, 4, 4);
        comp.createSelectionFrom(origSelRect);

        assertThat(comp).hasSelection();
        assertThat(comp.getSelection())
            .isNotNull()
            .hasShapeBounds(origSelRect);

        comp.invertSelection();

        assertThat(comp.getSelection())
            .isNotNull()
            .hasShapeBounds(comp.getCanvasBounds());
        History.assertNumEditsIs(1);

        History.undo("Invert Selection");
        assertThat(comp.getSelection())
            .isNotNull()
            .hasShapeBounds(origSelRect);

        History.redo("Invert Selection");
        assertThat(comp.getSelection())
            .isNotNull()
            .hasShapeBounds(comp.getCanvasBounds());
    }

    @Test
    void createSelectionFrom() {
        assertThat(comp).doesNotHaveSelection();

        Rectangle rect = new Rectangle(3, 3, 5, 5);
        comp.createSelectionFrom(rect);

        assertThat(comp)
            .hasSelection()
            .selectionShapeIs(rect)
            .invariantsAreOK();
    }

    @Test
    void changeStackIndex() {
        assertThat(comp).layerNamesAre("layer 1", "layer 2");

        Layer layer = comp.getLayer(0);
        comp.changeStackIndex(layer, 1);

        assertThat(comp)
            .layerNamesAre("layer 2", "layer 1")
            .invariantsAreOK();

        History.undo("Layer Reordering");
        assertThat(comp).layerNamesAre("layer 1", "layer 2");

        History.redo("Layer Reordering");
        assertThat(comp).layerNamesAre("layer 2", "layer 1");
    }

    @Test
    void translationWODuplicating() {
        testTranslation(false);
    }

    @Test
    void translationWithDuplicating() {
        testTranslation(true);
    }

    private void testTranslation(boolean makeDuplicateLayer) {
        // delete one layer in order to simplify
        comp.deleteLayer(comp.getActiveLayer(), true);

        assertThat(comp)
            .activeLayerTranslationIs(0, 0)
            .activeLayerAndMaskImageSizeIs(20, 10)
            .allLayerUIsAreOK();
        History.assertNumEditsIs(1);

        // 1. direction south-east
        TestHelper.move(comp, 2, 2, makeDuplicateLayer);

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
        TestHelper.move(comp, -2, -2, makeDuplicateLayer);

        assertThat(comp)
            // this time we have a non-zero translation
            .activeLayerTranslationIs(-2, -2)
            // no need to enlarge the image again
            .activeLayerAndMaskImageSizeIs(22, 12);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(3);
        }

        // 3. direction north-west again
        TestHelper.move(comp, -2, -2, makeDuplicateLayer);

        assertThat(comp)
            // the translation increases
            .activeLayerTranslationIs(-4, -4)
            // the image needs to be enlarged now
            .activeLayerAndMaskImageSizeIs(24, 14);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(4);
        }

        // 4. direction north-east
        TestHelper.move(comp, 2, -2, makeDuplicateLayer);
        assertThat(comp)
            // the translation increases
            .activeLayerTranslationIs(-2, -6)
            // the image needs to be enlarged vertically
            .activeLayerAndMaskImageSizeIs(24, 16);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(5);
        }

        // 5. opposite movement: direction south-west
        TestHelper.move(comp, -2, 2, makeDuplicateLayer);

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
            assertHistoryEditsAre("Delete layer 2",
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer");
        } else {
            numEdits = 6; // 1 initially + 5 movements
            assertHistoryEditsAre("Delete layer 2",
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
    void deselect() {
        assertThat(comp).doesNotHaveSelection();

        var selectionShape = new Rectangle(4, 4, 8, 4);
        TestHelper.setSelection(comp, selectionShape);

        assertThat(comp)
            .hasSelection()
            .selectionBoundsIs(selectionShape);

        comp.deselect(true);

        assertThat(comp).doesNotHaveSelection();
        History.assertNumEditsIs(1);

        History.undo("Deselect");
        assertThat(comp)
            .hasSelection()
            .selectionBoundsIs(selectionShape);

        History.redo("Deselect");
        assertThat(comp).doesNotHaveSelection();
    }

    @Test
    void intersectSelectionWith() {
        var selectionShape = new Rectangle(4, 4, 8, 4);
        TestHelper.setSelection(comp, selectionShape);
        assertThat(comp)
            .hasSelection()
            .selectionBoundsIs(selectionShape);

        var cropRect = new Rectangle(2, 2, 4, 4);
        comp.intersectSelectionWith(cropRect);

        assertThat(comp)
            .hasSelection()
            .selectionBoundsIs(new Rectangle(4, 4, 2, 2));

        var tx = Crop.createCropTransform(cropRect);
        comp.imCoordsChanged(tx, false, comp.getView());

        assertThat(comp)
            .hasSelection()
            .selectionBoundsIs(new Rectangle(2, 2, 2, 2));

        // There is no undo at this level
        History.assertNumEditsIs(0);
    }

    @Test
    void rename() {
        comp.setView(null); // avoid the creation of the main window
        assertThat(comp).hasName("CompositionTest").isNotOpen();

        comp.rename("CompositionTest", "CompositionTest New Name");
        assertThat(comp).hasName("CompositionTest New Name");

        History.undo("Rename Image");
        assertThat(comp).hasName("CompositionTest");

        History.redo("Rename Image");
        assertThat(comp).hasName("CompositionTest New Name");
    }
}