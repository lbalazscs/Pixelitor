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

package pixelitor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import pixelitor.compactions.Crop;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMoveDirection;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import static pixelitor.TestHelper.assertHistoryEditsAre;
import static pixelitor.TestHelper.createEmptyImageLayer;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.layers.LayerAdder.Position;

@DisplayName("Composition tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CompositionTest {
    private Composition comp;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode(true);
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

    @AfterEach
    void afterEachTest() {
        TestHelper.verifyAndClearHistory();
    }

    @Test
    void addNewEmptyImageLayer() {
        assertThat(comp)
            .isNotDirty()
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();
        TestHelper.setMaxUntestedEdits(2);

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

    @ParameterizedTest
    @EnumSource(Position.class)
    void addLayerRelative(Position position) {
        String historyName = "add relative " + position;

        comp.adder()
            .withHistory(historyName)
            .atPosition(position)
            .add(createEmptyImageLayer(comp, "new layer"));

        // initial state: ["layer 1", "layer 2"], active is "layer 2"
        String[] expectedLayerNames = switch (position) {
            case BELOW_ACTIVE -> new String[]{"layer 1", "new layer", "layer 2"};
            case ABOVE_ACTIVE -> new String[]{"layer 1", "layer 2", "new layer"};
        };

        assertThat(comp)
            .isDirty()
            .numLayersIs(3)
            .layerNamesAre(expectedLayerNames)
            .activeLayerNameIs("new layer");

        History.undo(historyName);
        assertThat(comp)
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive()
            .activeLayerNameIs("layer 2");

        History.redo(historyName);
        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre(expectedLayerNames)
            .activeLayerNameIs("new layer");
    }

    @ParameterizedTest
    @CsvSource({
        "bottom, 0",
        "middle, 1",
        "top, 2"
    })
    void addLayerAtIndex(String positionName, int index) {
        String historyName = "add at index " + positionName;

        comp.adder()
            .withHistory(historyName)
            .atIndex(index)
            .add(createEmptyImageLayer(comp, "new layer"));

        // initial state: ["layer 1", "layer 2"]
        String[] expectedLayerNames = switch (positionName) {
            case "bottom" -> new String[]{"new layer", "layer 1", "layer 2"};
            case "middle" -> new String[]{"layer 1", "new layer", "layer 2"};
            case "top" -> new String[]{"layer 1", "layer 2", "new layer"};
            default -> throw new IllegalArgumentException("Unexpected position: " + positionName);
        };

        assertThat(comp)
            .isDirty()
            .numLayersIs(3)
            .layerNamesAre(expectedLayerNames)
            .activeLayerIsAtIndex(index)
            .activeLayerNameIs("new layer");

        History.undo(historyName);
        assertThat(comp)
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive()
            .activeLayerNameIs("layer 2");

        History.redo(historyName);
        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre(expectedLayerNames)
            .activeLayerIsAtIndex(index)
            .activeLayerNameIs("new layer");
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
        comp.flattenImage();

        assertThat(comp)
            .isDirty()
            .numLayersIs(1)
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

    @ParameterizedTest
    @EnumSource(LayerMoveDirection.class)
    @DisplayName("move active layer relative")
    void moveActiveLayerRelative(LayerMoveDirection direction) {
        // add a 3rd layer to allow movement in both directions from the middle
        addThirdLayer();
        // state: layer 1, layer 2, layer 3. Active: layer 3.

        // activate the middle layer
        comp.setActiveLayer(comp.getLayer(1));

        String historyName = direction.getName();
        comp.getActiveHolder().reorderActiveLayer(direction);

        String[] expectedLayerNames = switch (direction) {
            case UP -> new String[]{"layer 1", "layer 3", "layer 2"};
            case DOWN -> new String[]{"layer 2", "layer 1", "layer 3"};
        };

        assertThat(comp)
            .isDirty()
            .layerNamesAre(expectedLayerNames)
            .activeLayerNameIs("layer 2");

        History.undo(historyName);
        assertThat(comp).layerNamesAre("layer 1", "layer 2", "layer 3");

        History.redo(historyName);
        assertThat(comp).layerNamesAre(expectedLayerNames);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOP", "BOTTOM"})
    @DisplayName("move active layer to extreme")
    void moveActiveLayerToExtreme(String dest) {
        addThirdLayer();
        // state: layer 1, layer 2, layer 3. Active: layer 3.
        comp.setActiveLayer(comp.getLayer(1)); // activate middle

        String historyName;
        String[] expectedLayers;

        if ("TOP".equals(dest)) {
            comp.getActiveHolder().moveActiveLayerToTop();
            historyName = "Layer to Top";
            expectedLayers = new String[]{"layer 1", "layer 3", "layer 2"};
        } else {
            comp.getActiveHolder().moveActiveLayerToBottom();
            historyName = "Layer to Bottom";
            expectedLayers = new String[]{"layer 2", "layer 1", "layer 3"};
        }

        assertThat(comp)
            .isDirty()
            .layerNamesAre(expectedLayers)
            .activeLayerNameIs("layer 2");

        History.undo(historyName);
        assertThat(comp).layerNamesAre("layer 1", "layer 2", "layer 3");

        History.redo(historyName);
        assertThat(comp).layerNamesAre(expectedLayers);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 2, 'layer 2, layer 3, layer 1'", // move bottom (L1) to top
        "2, 0, 'layer 3, layer 1, layer 2'", // move top (L3) to bottom
        "1, 0, 'layer 2, layer 1, layer 3'", // move middle (L2) to bottom
        "1, 2, 'layer 1, layer 3, layer 2'"  // move middle (L2) to top
    })
    @DisplayName("reorder layer by index")
    void reorderLayerByIndex(int fromIndex, int toIndex, String expectedNamesStr) {
        addThirdLayer();
        // state: layer 1, layer 2, layer 3

        comp.reorderLayer(fromIndex, toIndex, true, null);

        String[] expectedNames = expectedNamesStr.split(", ");
        assertThat(comp)
            .isDirty()
            .layerNamesAre(expectedNames);

        History.undo("Layer Order Change");
        assertThat(comp).layerNamesAre("layer 1", "layer 2", "layer 3");

        History.redo("Layer Order Change");
        assertThat(comp).layerNamesAre(expectedNames);
    }

    @Test
    @DisplayName("lower/raise layer selection")
    void moveLayerSelection() {
        // initial state
        assertThat(comp)
            .isNotDirty()
            .layerNamesAre("layer 1", "layer 2")
            .secondLayerIsActive();

        comp.getActiveHolder().lowerLayerSelection(); // make the first layer active
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

        comp.getActiveHolder().raiseLayerSelection(); // make the second layer active
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
        comp.setActiveLayer(layer1, false, null);
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
    void isActiveLayer() {
        Layer layer1 = comp.getLayer(0);
        Layer layer2 = comp.getLayer(1);

        assertThat(comp).activeLayerIs(layer2);
        assertThat(comp.isActiveLayer(layer1)).isFalse();
        assertThat(comp.isActiveLayer(layer2)).isTrue();

        comp.setActiveLayer(layer1, true, null);

        assertThat(comp).activeLayerIs(layer1).invariantsAreOK();
        assertThat(comp.isActiveLayer(layer1)).isTrue();
        assertThat(comp.isActiveLayer(layer2)).isFalse();

        History.undo("Layer Selection Change");

        assertThat(comp).activeLayerIs(layer2);
        assertThat(comp.isActiveLayer(layer1)).isFalse();
        assertThat(comp.isActiveLayer(layer2)).isTrue();

        History.redo("Layer Selection Change");

        assertThat(comp).activeLayerIs(layer1);
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testTranslation(boolean duplicateLayer) {
        // delete one layer in order to simplify
        comp.deleteLayer(comp.getActiveLayer(), false);

        TestHelper.setMaxUntestedEdits(duplicateLayer ? 10 : 5);

        assertThat(comp)
            .activeLayerTranslationIs(0, 0)
            .activeLayerAndMaskImageSizeIs(20, 10)
            .allLayerUIsAreOK();
        History.assertNumEditsIs(0);

        // 1. direction south-east
        TestHelper.move(comp, 2, 2, duplicateLayer);

        String[] expectedLayers = {"layer 1"};
        if (duplicateLayer) {
            expectedLayers = new String[]{"layer 1", "layer 1 copy"};
        }

        assertThat(comp)
            .layerNamesAre(expectedLayers)
            // no translation because the image was
            // moved south-east, and enlarged
            .activeLayerTranslationIs(0, 0)
            .activeLayerAndMaskImageSizeIs(22, 12);
        History.assertNumEditsIs(duplicateLayer ? 2 : 1);
        History.assertLastEditNameIs("Move Layer");

        // 2. direction north-west
        TestHelper.move(comp, -2, -2, duplicateLayer);

        assertThat(comp)
            // this time we have a non-zero translation
            .activeLayerTranslationIs(-2, -2)
            // no need to enlarge the image again
            .activeLayerAndMaskImageSizeIs(22, 12);
        History.assertNumEditsIs(duplicateLayer ? 4 : 2);

        // 3. direction north-west again
        TestHelper.move(comp, -2, -2, duplicateLayer);

        assertThat(comp)
            // the translation increases
            .activeLayerTranslationIs(-4, -4)
            // the image needs to be enlarged now
            .activeLayerAndMaskImageSizeIs(24, 14);
        History.assertNumEditsIs(duplicateLayer ? 6 : 3);

        // 4. direction north-east
        TestHelper.move(comp, 2, -2, duplicateLayer);
        assertThat(comp)
            // the translation increases
            .activeLayerTranslationIs(-2, -6)
            // the image needs to be enlarged vertically
            .activeLayerAndMaskImageSizeIs(24, 16);
        History.assertNumEditsIs(duplicateLayer ? 8 : 4);

        // 5. opposite movement: direction south-west
        TestHelper.move(comp, -2, 2, duplicateLayer);

        if (duplicateLayer) {
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
        if (duplicateLayer) {
            numEdits = 10; // 2 * 5
            assertHistoryEditsAre(
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer",
                "Duplicate Layer", "Move Layer");
        } else {
            numEdits = 5; // 5 movements
            assertHistoryEditsAre(
                "Move Layer", "Move Layer", "Move Layer", "Move Layer", "Move Layer");
        }
        History.assertNumEditsIs(numEdits);

        // undo everything
        for (int i = 0; i < numEdits; i++) {
            boolean duplicating = duplicateLayer && i % 2 == 1;
            History.undo(duplicating ? "Duplicate Layer" : "Move Layer");
        }
        assertThat(comp)
            .activeLayerTranslationIs(0, 0)
            .activeLayerAndMaskImageSizeIs(20, 10);

        // redo everything
        for (int i = 0; i < numEdits; i++) {
            boolean duplicating = duplicateLayer && i % 2 == 0;
            History.redo(duplicating ? "Duplicate Layer" : "Move Layer");
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

        // the history isn't managed in this method
        History.assertNumEditsIs(0);
    }

    @Test
    void rename() {
        comp.rename("CompositionTest", "CompositionTest New Name");
        assertThat(comp).hasName("CompositionTest New Name");

        History.undo("Rename Image");
        assertThat(comp).hasName("CompositionTest");

        History.redo("Rename Image");
        assertThat(comp).hasName("CompositionTest New Name");
    }

    @Test
    void copyComposition() {
        Composition copy = comp.copy(CopyType.DUPLICATE_COMP, true);

        assertThat(copy)
            .isNotSameAs(comp)
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .activeLayerNameIs("layer 2");

        // verify deep copy of layers
        assertThat(copy.getLayer(0)).isNotSameAs(comp.getLayer(0));
    }

    @Test
    void dispose() {
        assertThat(comp).isOpen();

        comp.dispose();

        assertThat(comp)
            .isNotOpen()
            .doesNotHaveSelection();
    }

    @Test
    void mergeDown() {
        Layer bottomLayer = comp.getLayer(0);
        Layer topLayer = comp.getLayer(1);

        // in the standard setup, only the top layer can be merged down
        assertThat(comp.canMergeDown(topLayer)).isTrue();
        assertThat(comp.canMergeDown(bottomLayer)).isFalse();

        // a hidden layer can't be merged down
        topLayer.setVisible(false);
        assertThat(comp.canMergeDown(topLayer)).isFalse();
        topLayer.setVisible(true);

        // the bottom layer must also be visible
        bottomLayer.setVisible(false);
        assertThat(comp.canMergeDown(topLayer)).isFalse();
        bottomLayer.setVisible(true);

        // insert a non-image layer between the two image layers
        Layer textLayer = TestHelper.createTextLayer(comp, "Text Layer");
        comp.adder()
            .atIndex(1)
            .add(textLayer);
        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre("layer 1", "Text Layer", "layer 2");

        // can't merge down onto the text layer
        assertThat(comp.canMergeDown(topLayer)).isFalse();
        // the text layer can be merged down onto the bottom image layer
        assertThat(comp.canMergeDown(textLayer)).isTrue();

        // actually merge down
        comp.mergeDown(textLayer);
        assertThat(comp)
            .isDirty()
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2");
        History.assertNumEditsIs(1);

        History.undo("Merge Down");
        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre("layer 1", "Text Layer", "layer 2");

        History.redo("Merge Down");
        assertThat(comp)
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2");
    }

    private void addThirdLayer() {
        var layer3 = ImageLayer.createEmpty(comp, "layer 3");
        comp.addLayerWithoutUI(layer3); // adds it without history
    }
}
