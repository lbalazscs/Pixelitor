/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.utils.UpdateGUI;

import java.awt.Rectangle;
import java.awt.Shape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CompositionTest {
    private Composition comp;
    private CompTester tester;

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerComposition(true);
        assertEquals("Composition{name='Test', activeLayer=layer 2, layerList=[" +
                "ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 1', visible=true, " +
                "mask=LayerMask{state=NORMAL, super={tx=0, ty=0, super={name='layer 1 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, " +
                "ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 2', visible=true, " +
                "mask=LayerMask{state=NORMAL, super={tx=0, ty=0, super={name='layer 2 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}], " +
                "canvas=Canvas{width=20, height=10}, selection=null, dirty=false}", comp.toString());
        tester = new CompTester(comp);
        tester.checkDirty(false);

        History.setUndoLevels(10);
        History.clear();
    }

    @Test
    public void testAddNewEmptyLayer() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        comp.addNewEmptyLayer("newLayer 1", true);
        tester.checkLayers("[layer 1, ACTIVE newLayer 1, layer 2]");
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("New Empty Layer");

        comp.addNewEmptyLayer("newLayer 2", false);

        tester.checkLayers("[layer 1, newLayer 1, ACTIVE newLayer 2, layer 2]");
        tester.checkDirty(true);
        History.assertNumEditsIs(2);
        History.assertLastEditNameIs("New Empty Layer");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE newLayer 1, layer 2]");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[layer 1, ACTIVE newLayer 1, layer 2]");

        History.redo();
        tester.checkLayers("[layer 1, newLayer 1, ACTIVE newLayer 2, layer 2]");
    }

    @Test
    public void testSetActiveLayer() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        Layer layer = comp.getLayer(0);
        comp.setActiveLayer(layer, AddToHistory.YES);
        assertSame(layer, comp.getActiveLayer());
        tester.checkLayers("[ACTIVE layer 1, layer 2]");
        tester.checkDirty(true);
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Layer Selection Change");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[ACTIVE layer 1, layer 2]");
    }

    @Test
    public void testAddLayerNoGUI() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        ImageLayer newLayer = TestHelper.createImageLayer("layer 3", comp);
        comp.addLayerNoGUI(newLayer);
        tester.checkLayers("[layer 1, layer 2, ACTIVE layer 3]");
        tester.checkDirty(false);
    }

    @Test
    public void testAddLayer() {
        // add bellow active
        comp.addLayer(TestHelper.createImageLayer("layer A", comp),
                AddToHistory.YES, "New Test Layer", true, true);
        tester.checkLayers("[layer 1, ACTIVE layer A, layer 2]");
        tester.checkDirty(true);

        // add above active
        comp.addLayer(TestHelper.createImageLayer("layer B", comp),
                AddToHistory.YES, "New Test Layer", true, false);
        tester.checkLayers("[layer 1, layer A, ACTIVE layer B, layer 2]");

        // add to position 0
        comp.addLayer(TestHelper.createImageLayer("layer C", comp),
                AddToHistory.YES, "New Test Layer", true, 0);
        tester.checkLayers("[ACTIVE layer C, layer 1, layer A, layer B, layer 2]");

        // add to position 2
        comp.addLayer(TestHelper.createImageLayer("layer D", comp),
                AddToHistory.YES, "New Test Layer", true, 2);
        tester.checkLayers("[layer C, layer 1, ACTIVE layer D, layer A, layer B, layer 2]");

        History.assertNumEditsIs(4);
        History.assertLastEditNameIs("New Test Layer");

        History.undo();
        tester.checkLayers("[ACTIVE layer C, layer 1, layer A, layer B, layer 2]");

        History.undo();
        tester.checkLayers("[layer 1, layer A, ACTIVE layer B, layer 2]");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer A, layer 2]");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[layer 1, ACTIVE layer A, layer 2]");

        History.redo();
        tester.checkLayers("[layer 1, layer A, ACTIVE layer B, layer 2]");

        History.redo();
        tester.checkLayers("[ACTIVE layer C, layer 1, layer A, layer B, layer 2]");

        History.redo();
        tester.checkLayers("[layer C, layer 1, ACTIVE layer D, layer A, layer B, layer 2]");
    }

    @Test
    public void testDuplicateLayer() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        comp.duplicateActiveLayer();

        tester.checkLayers("[layer 1, layer 2, ACTIVE layer 2 copy]");
        tester.checkDirty(true);
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Duplicate Layer");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[layer 1, layer 2, ACTIVE layer 2 copy]");
    }

    @Test
    public void testGetCanvas() {
        Canvas canvas = comp.getCanvas();
        assertThat(canvas).isNotNull();

        assertThat(canvas.toString()).isEqualTo("Canvas{width=20, height=10}");

        comp.checkInvariant();
    }

    @Test
    public void testIsEmpty() {
        boolean empty = comp.isEmpty();
        assertThat(empty).isFalse();
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

    @Test
    public void testFlattenImage() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        comp.flattenImage(UpdateGUI.NO);
        tester.checkLayers("[ACTIVE flattened]");
        tester.checkDirty(true);

        // there is no undo for flatten image
    }

    @Test
    public void testMergeDown() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        comp.setActiveLayer(comp.getLayer(1), AddToHistory.YES);

        comp.mergeDown(UpdateGUI.NO);

        tester.checkLayers("[ACTIVE layer 1]");
        tester.checkDirty(true);
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Merge Down");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[ACTIVE layer 1]");
    }

    @Test
    public void testMoveActiveLayer() {
        tester.checkDirty(false);
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        comp.moveActiveLayerUp();
        tester.checkDirty(false);
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        comp.moveActiveLayerDown();
        tester.checkDirty(true);
        tester.checkLayers("[ACTIVE layer 2, layer 1]");
        comp.moveActiveLayerUp();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        comp.moveActiveLayerToBottom();
        tester.checkLayers("[ACTIVE layer 2, layer 1]");

        comp.moveActiveLayerToTop();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        comp.swapLayers(0, 1, AddToHistory.YES);
        tester.checkLayers("[ACTIVE layer 2, layer 1]");
    }

    @Test
    public void testMoveLayerSelection() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        comp.moveLayerSelectionDown();
        tester.checkLayers("[ACTIVE layer 1, layer 2]");
        comp.moveLayerSelectionUp();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        tester.checkDirty(true);
    }

    @Test
    public void testGenerateNewLayerName() {
        String newLayerName = comp.generateNewLayerName();
        assertThat(newLayerName).isEqualTo("layer 1");
        comp.checkInvariant();
    }

    @Test
    public void testGetCanvasBounds() {
        Rectangle bounds = comp.getCanvasBounds();
        assertThat(bounds).isNotNull();
        assertThat(bounds.toString()).isEqualTo("java.awt.Rectangle[x=0,y=0,width=20,height=10]");
        comp.checkInvariant();
    }

    @Test
    public void testDeleteActiveLayer() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        comp.deleteActiveLayer(UpdateGUI.NO, AddToHistory.YES);
        tester.checkLayers("[ACTIVE layer 1]");
        tester.checkDirty(true);
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Delete Layer");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[ACTIVE layer 1]");
    }

    @Test
    public void testDeleteLayer() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        Layer layer2 = comp.getLayer(1);

        comp.deleteLayer(layer2, AddToHistory.YES, UpdateGUI.NO);
        tester.checkLayers("[ACTIVE layer 1]");
        tester.checkDirty(true);
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Delete Layer");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[ACTIVE layer 1]");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        // now delete layer 1
        Layer layer1 = comp.getLayer(0);
        comp.setActiveLayer(layer1, AddToHistory.YES);
        tester.checkLayers("[ACTIVE layer 1, layer 2]");

        comp.deleteLayer(layer1, AddToHistory.YES, UpdateGUI.NO);
        tester.checkLayers("[ACTIVE layer 2]");

        History.undo();
        tester.checkLayers("[ACTIVE layer 1, layer 2]");

        History.redo();
        tester.checkLayers("[ACTIVE layer 2]");
    }

    @Test
    public void testAddNewLayerFromComposite() {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        comp.addNewLayerFromComposite("composite layer");

        tester.checkLayers("[layer 1, layer 2, ACTIVE composite layer]");
        tester.checkDirty(true);
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("New Layer from Composite");

        History.undo();
        tester.checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        tester.checkLayers("[layer 1, layer 2, ACTIVE composite layer]");
    }

    @Test
    public void testIsActiveLayer() {
        Layer layer1 = comp.getLayer(0);
        Layer layer2 = comp.getLayer(1);

        assertThat(comp.isActiveLayer(layer1)).isFalse();
        assertThat(comp.isActiveLayer(layer2)).isTrue();

        comp.checkInvariant();

        comp.setActiveLayer(layer1, AddToHistory.YES);
        assertThat(comp.isActiveLayer(layer1)).isTrue();
        assertThat(comp.isActiveLayer(layer2)).isFalse();

        comp.checkInvariant();
    }

    @Test
    public void testInvertSelection() {
        comp.invertSelection();
        comp.checkInvariant();
        tester.addRectangleSelection(3, 3, 4, 4);
        tester.checkSelectionBounds(new Rectangle(3, 3, 4, 4));

        comp.invertSelection();

        tester.checkSelectionBounds(comp.getCanvasBounds());
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Invert Selection");

        History.undo();
        tester.checkSelectionBounds(new Rectangle(3, 3, 4, 4));

        History.redo();
        tester.checkSelectionBounds(comp.getCanvasBounds());
    }

    @Test
    public void testCreateSelectionFromShape() {
        comp.createSelectionFromShape(new Rectangle(3, 3, 5, 5));
        comp.checkInvariant();

        Shape shape = comp.getSelection().getShape();
        assertThat(shape).isEqualTo(new Rectangle(3, 3, 5, 5));
    }

    @Test
    public void testDragFinished() {
        Layer layer = comp.getLayer(0);
        comp.dragFinished(layer, 1);
        comp.checkInvariant();
    }

    private void testTranslation(boolean makeDuplicateLayer) {
        tester.checkLayers("[layer 1, ACTIVE layer 2]");
        // delete one layer so that we have undo
        comp.deleteLayer(comp.getActiveLayer(), AddToHistory.YES, UpdateGUI.NO);
        tester.checkLayers("[ACTIVE layer 1]");
        History.assertNumEditsIs(1);

        tester.checkActiveLayerTranslation(0, 0);
        tester.checkActiveLayerAndMaskImageSize(20, 10);

        // 1. direction south-east
        tester.moveLayer(makeDuplicateLayer, 2, 2);

        tester.checkDirty(true);
        // no translation because the image was moved south-east, and enlarged
        tester.checkActiveLayerTranslation(0, 0);
        tester.checkActiveLayerAndMaskImageSize(22, 12);
        History.assertNumEditsIs(makeDuplicateLayer ? 3 : 2);
        History.assertLastEditNameIs("Move Layer");

        if (makeDuplicateLayer) {
            tester.checkLayers("[layer 1, ACTIVE layer 1 copy]");
        } else {
            // no change in the number of layers
            tester.checkLayers("[ACTIVE layer 1]");
            History.assertNumEditsIs(2);
        }

        // 2. direction north-west
        tester.moveLayer(makeDuplicateLayer, -2, -2);
        // this time we have a non-zero translation
        tester.checkActiveLayerTranslation(-2, -2);
        // no need to enlarge the image again
        tester.checkActiveLayerAndMaskImageSize(22, 12);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(3);
        }

        // 3. direction north-west again
        tester.moveLayer(makeDuplicateLayer, -2, -2);
        // the translation increases
        tester.checkActiveLayerTranslation(-4, -4);
        // the image needs to be enlarged now
        tester.checkActiveLayerAndMaskImageSize(24, 14);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(4);
        }

        // 4. direction north-east
        tester.moveLayer(makeDuplicateLayer, 2, -2);
        // the translation increases
        tester.checkActiveLayerTranslation(-2, -6);
        // the image needs to be enlarged vertically
        tester.checkActiveLayerAndMaskImageSize(24, 16);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(5);
        }

        // 5. opposite movement: direction south-west
        tester.moveLayer(makeDuplicateLayer, -2, 2);
        // translation back to -4, -4
        tester.checkActiveLayerTranslation(-4, -4);
        // no need to enlarge the image
        tester.checkActiveLayerAndMaskImageSize(24, 16);
        if (!makeDuplicateLayer) {
            History.assertNumEditsIs(6);
        }

        if (makeDuplicateLayer) {
            tester.checkLayers("[layer 1, layer 1 copy, layer 1 copy 2, layer 1 copy 3, layer 1 copy 4, ACTIVE layer 1 copy 5]");
        } else {
            // no change in the number of layers
            tester.checkLayers("[ACTIVE layer 1]");
        }

        if (!makeDuplicateLayer) { // we should have undo in this case
            for (int i = 0; i < 6; i++) {
                History.undo();
            }
            tester.checkActiveLayerTranslation(0, 0);
            tester.checkActiveLayerAndMaskImageSize(20, 10);

            for (int i = 0; i < 6; i++) {
                History.redo();
            }
            tester.checkActiveLayerTranslation(-4, -4);
            tester.checkActiveLayerAndMaskImageSize(24, 16);
        }

        // now test "Layer to Canvas Size"
        comp.activeLayerToCanvasSize();
        tester.checkActiveLayerTranslation(0, 0);
        tester.checkActiveLayerAndMaskImageSize(20, 10);

        History.undo();
        tester.checkActiveLayerTranslation(-4, -4);
        tester.checkActiveLayerAndMaskImageSize(24, 16);

        History.redo();
        tester.checkActiveLayerTranslation(0, 0);
        tester.checkActiveLayerAndMaskImageSize(20, 10);
    }

    @Test
    public void testDeselect() {
        assertThat(comp.hasSelection()).isFalse();
        tester.setStandardTestSelection();
        assertThat(comp.hasSelection()).isTrue();
        tester.checkSelectionBounds(tester.getStandardTestSelectionShape());

        comp.deselect(AddToHistory.YES);

        assertThat(comp.hasSelection()).isFalse();
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Deselect");

        History.undo();
        assertThat(comp.hasSelection()).isTrue();
        tester.checkSelectionBounds(tester.getStandardTestSelectionShape());

        History.redo();
        assertThat(comp.hasSelection()).isFalse();
    }

    @Test
    public void testCropSelection() {
        tester.setStandardTestSelection();
        tester.checkSelectionBounds(tester.getStandardTestSelectionShape());
        comp.cropSelection(new Rectangle(2, 2, 4, 4));
        tester.checkSelectionBounds(new Rectangle(2, 2, 2, 2));
    }

    @Test
    public void testSimpleMethods() {
        assertThat(comp.getActiveLayerIndex()).isEqualTo(1);
        assertThat(comp.activeIsImageLayerOrMask()).isTrue();

        assertThat(comp.getName()).isEqualTo("Test");
        comp.setName("New Name");
        assertThat(comp.getName()).isEqualTo("New Name");
    }
}