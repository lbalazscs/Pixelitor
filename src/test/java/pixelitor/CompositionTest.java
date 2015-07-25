/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.filters.comp.Crop;
import pixelitor.filters.comp.EnlargeCanvas;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Resize;
import pixelitor.filters.comp.Rotate;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.test.Assertions;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.HISTOGRAM;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;
import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_90;
import static pixelitor.selection.SelectionInteraction.ADD;
import static pixelitor.selection.SelectionType.ELLIPSE;

public class CompositionTest {
    private Composition comp;

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerTestComposition(true);
        assertEquals("Composition{name='Test', activeLayer=layer 2, layerList=[" +
                "ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 1', visible=true, " +
                "mask=ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 1 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, " +
                "ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 2', visible=true, " +
                "mask=ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 2 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}], " +
                "canvas=Canvas{width=20, height=10}, selection=null, dirty=false}", comp.toString());
        checkDirty(false);
        History.setUndoLevels(10);
    }

    @Test
    public void testAddNewEmptyLayer() {
        checkLayers("[layer 1, ACTIVE layer 2]");

        comp.addNewEmptyLayer("newLayer 1", true);
        checkLayers("[layer 1, ACTIVE newLayer 1, layer 2]");

        comp.addNewEmptyLayer("newLayer 2", false);

        checkLayers("[layer 1, newLayer 1, ACTIVE newLayer 2, layer 2]");
        checkDirty(true);

        History.undo();
        checkLayers("[layer 1, ACTIVE newLayer 1, layer 2]");

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        checkLayers("[layer 1, ACTIVE newLayer 1, layer 2]");

        History.redo();
        checkLayers("[layer 1, newLayer 1, ACTIVE newLayer 2, layer 2]");
    }

    @Test
    public void testSetActiveLayer() {
        checkLayers("[layer 1, ACTIVE layer 2]");

        Layer layer = comp.getLayer(0);
        comp.setActiveLayer(layer, AddToHistory.YES);
        assertSame(layer, comp.getActiveLayer());
        checkLayers("[ACTIVE layer 1, layer 2]");
        checkDirty(false);

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        checkLayers("[ACTIVE layer 1, layer 2]");
    }

    @Test
    public void testAddLayerNoGUI() {
        checkLayers("[layer 1, ACTIVE layer 2]");

        ImageLayer newLayer = TestHelper.createTestImageLayer("layer 3", comp);
        comp.addLayerNoGUI(newLayer);
        checkLayers("[layer 1, layer 2, ACTIVE layer 3]");
        checkDirty(true);
    }

    @Test
    public void testAddLayer() {
        // add bellow active
        comp.addLayer(TestHelper.createTestImageLayer("layer A", comp),
                AddToHistory.YES, true, true);
        checkLayers("[layer 1, ACTIVE layer A, layer 2]");
        checkDirty(true);

        // add above active
        comp.addLayer(TestHelper.createTestImageLayer("layer B", comp),
                AddToHistory.YES, true, false);
        checkLayers("[layer 1, layer A, ACTIVE layer B, layer 2]");

        // add to position 0
        comp.addLayer(TestHelper.createTestImageLayer("layer C", comp),
                AddToHistory.YES, true, 0);
        checkLayers("[ACTIVE layer C, layer 1, layer A, layer B, layer 2]");

        // add to position 2
        comp.addLayer(TestHelper.createTestImageLayer("layer D", comp),
                AddToHistory.YES, true, 2);
        checkLayers("[layer C, layer 1, ACTIVE layer D, layer A, layer B, layer 2]");

        History.undo();
        checkLayers("[ACTIVE layer C, layer 1, layer A, layer B, layer 2]");

        History.undo();
        checkLayers("[layer 1, layer A, ACTIVE layer B, layer 2]");

        History.undo();
        checkLayers("[layer 1, ACTIVE layer A, layer 2]");

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        checkLayers("[layer 1, ACTIVE layer A, layer 2]");

        History.redo();
        checkLayers("[layer 1, layer A, ACTIVE layer B, layer 2]");

        History.redo();
        checkLayers("[ACTIVE layer C, layer 1, layer A, layer B, layer 2]");

        History.redo();
        checkLayers("[layer C, layer 1, ACTIVE layer D, layer A, layer B, layer 2]");
    }

    @Test
    public void testDuplicateLayer() {
        checkLayers("[layer 1, ACTIVE layer 2]");

        comp.duplicateLayer();

        checkLayers("[layer 1, layer 2, ACTIVE layer 2 copy]");
        checkDirty(true);

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");
        History.redo();
        checkLayers("[layer 1, layer 2, ACTIVE layer 2 copy]");
    }

    @Test
    public void testFilterWithoutDialogFinished() {
        BufferedImage image = TestHelper.createTestImage();
        comp.filterWithoutDialogFinished(image, ChangeReason.OP_WITHOUT_DIALOG, "opName");
        comp.checkInvariant();
    }

    @Test
    public void testGetActiveImageLayer() {
        assertTrue(comp.getActiveMaskOrImageLayerOpt().isPresent());
        comp.checkInvariant();
    }

    @Test
    public void testGetImageOrSubImageIfSelectedForActiveLayer() {
        BufferedImage imageTT = comp.getImageOrSubImageIfSelectedForActiveLayer(true, true);
        assertNotNull(imageTT);
        BufferedImage imageTF = comp.getImageOrSubImageIfSelectedForActiveLayer(true, false);
        assertNotNull(imageTF);
        BufferedImage imageFT = comp.getImageOrSubImageIfSelectedForActiveLayer(false, true);
        assertNotNull(imageFT);
        BufferedImage imageFF = comp.getImageOrSubImageIfSelectedForActiveLayer(false, false);
        assertNotNull(imageFF);
        comp.checkInvariant();
    }

    @Test
    public void testGetFilterSource() {
        BufferedImage image = comp.getFilterSource();
        assertNotNull(image);
        comp.checkInvariant();
    }

    @Test
    public void testGetCanvas() {
        Canvas canvas = comp.getCanvas();
        assertNotNull(canvas);

        assertEquals("Canvas{width=20, height=10}", canvas.toString());

        comp.checkInvariant();
    }

    @Test
    public void testIsEmpty() {
        boolean empty = comp.isEmpty();
        assertThat(empty, is(false));
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
        checkLayers("[layer 1, ACTIVE layer 2]");
        comp.flattenImage(UpdateGUI.NO);
        checkLayers("[ACTIVE flattened]");
        checkDirty(true);

        // there is no undo for flatten image
    }

    @Test
    public void testMergeDown() {
        checkLayers("[layer 1, ACTIVE layer 2]");
        comp.setActiveLayer(comp.getLayer(1), AddToHistory.YES);
        comp.mergeDown(UpdateGUI.NO);
        checkLayers("[ACTIVE layer 1]");
        checkDirty(true);

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");
        History.redo();
        checkLayers("[ACTIVE layer 1]");
    }

    @Test
    public void testMoveActiveLayer() {
        checkDirty(false);
        checkLayers("[layer 1, ACTIVE layer 2]");
        comp.moveActiveLayerUp();
        checkDirty(false);
        checkLayers("[layer 1, ACTIVE layer 2]");
        comp.moveActiveLayerDown();
        checkDirty(true);
        checkLayers("[ACTIVE layer 2, layer 1]");
        comp.moveActiveLayerUp();
        checkLayers("[layer 1, ACTIVE layer 2]");

        comp.moveActiveLayerToBottom();
        checkLayers("[ACTIVE layer 2, layer 1]");

        comp.moveActiveLayerToTop();
        checkLayers("[layer 1, ACTIVE layer 2]");

        comp.swapLayers(0, 1, AddToHistory.YES);
        checkLayers("[ACTIVE layer 2, layer 1]");
    }

    @Test
    public void testMoveLayerSelection() {
        checkLayers("[layer 1, ACTIVE layer 2]");
        comp.moveLayerSelectionDown();
        checkLayers("[ACTIVE layer 1, layer 2]");
        comp.moveLayerSelectionUp();
        checkLayers("[layer 1, ACTIVE layer 2]");

        checkDirty(false);
    }

    @Test
    public void testGenerateNewLayerName() {
        String newLayerName = comp.generateNewLayerName();
        assertEquals("layer 1", newLayerName);
        comp.checkInvariant();
    }

    @Test
    public void testGetCanvasBounds() {
        Rectangle bounds = comp.getCanvasBounds();
        assertNotNull(bounds);
        assertEquals("java.awt.Rectangle[x=0,y=0,width=20,height=10]", bounds.toString());
        comp.checkInvariant();
    }

    @Test
    public void testRemoveActiveLayer() {
        checkLayers("[layer 1, ACTIVE layer 2]");
        comp.removeActiveLayer(UpdateGUI.NO);
        checkLayers("[ACTIVE layer 1]");
        checkDirty(true);

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        checkLayers("[ACTIVE layer 1]");
    }

    @Test
    public void testRemoveLayer() {
        checkLayers("[layer 1, ACTIVE layer 2]");
        Layer layer2 = comp.getLayer(1);

        comp.removeLayer(layer2, AddToHistory.YES, UpdateGUI.NO);
        checkLayers("[ACTIVE layer 1]");
        checkDirty(true);

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");

        History.redo();
        checkLayers("[ACTIVE layer 1]");

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");

        // now remove layer 1
        Layer layer1 = comp.getLayer(0);
        comp.setActiveLayer(layer1, AddToHistory.YES);
        checkLayers("[ACTIVE layer 1, layer 2]");

        comp.removeLayer(layer1, AddToHistory.YES, UpdateGUI.NO);
        checkLayers("[ACTIVE layer 2]");

        History.undo();
        checkLayers("[ACTIVE layer 1, layer 2]");

        History.redo();
        checkLayers("[ACTIVE layer 2]");
    }

    @Test
    public void testAddNewLayerFromComposite() {
        checkLayers("[layer 1, ACTIVE layer 2]");
        comp.addNewLayerFromComposite("composite layer");
        checkLayers("[layer 1, layer 2, ACTIVE composite layer]");
        checkDirty(true);

        History.undo();
        checkLayers("[layer 1, ACTIVE layer 2]");
        History.redo();
        checkLayers("[layer 1, layer 2, ACTIVE composite layer]");
    }

    @Test
    public void testSelection() {
        assertThat(comp.hasSelection(), is(false));
        Optional<Selection> selection = comp.getSelection();
        assertThat(selection.isPresent(), is(false));

        comp.startSelection(ELLIPSE, ADD);

        selection = comp.getSelection();

        assertThat(selection.isPresent(), is(true));
        assertThat(comp.hasSelection(), is(true));
        comp.checkInvariant();
    }

    @Test
    public void testImageChanged() {
        comp.imageChanged(FULL);
        comp.checkInvariant();
        comp.imageChanged(REPAINT);
        comp.checkInvariant();
        comp.imageChanged(HISTOGRAM);
        comp.checkInvariant();
        comp.imageChanged(INVALIDATE_CACHE);
        comp.checkInvariant();
    }

    @Test
    public void testIsActiveLayer() {
        Layer layer1 = comp.getLayer(0);
        Layer layer2 = comp.getLayer(1);

        assertThat(comp.isActiveLayer(layer1), is(false));
        assertThat(comp.isActiveLayer(layer2), is(true));

        comp.checkInvariant();

        comp.setActiveLayer(layer1, AddToHistory.YES);
        assertThat(comp.isActiveLayer(layer1), is(true));
        assertThat(comp.isActiveLayer(layer2), is(false));

        comp.checkInvariant();
    }

    @Test
    public void testInvertSelection() {
        comp.invertSelection();
        comp.checkInvariant();

        addRectangleSelection(3, 3, 4, 4);
        checkSelectionBounds(new Rectangle(3, 3, 4, 4));

        comp.invertSelection();
        checkSelectionBounds(comp.getCanvasBounds());

        History.undo();
        checkSelectionBounds(new Rectangle(3, 3, 4, 4));

        History.redo();
        checkSelectionBounds(comp.getCanvasBounds());
    }

    private void addRectangleSelection(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        addRectangleSelection(rect);
    }

    private void addRectangleSelection(Rectangle rect) {
        comp.startSelection(SelectionType.RECTANGLE, SelectionInteraction.ADD);
        comp.getSelection().get().setShape(rect);
    }

    private void checkSelectionBounds(Rectangle expected) {
        Rectangle shapeBounds = comp.getSelectionOrNull().getShapeBounds();
        assertEquals(expected, shapeBounds);
    }

    @Test
    public void testStartSelection() {
        SelectionType[] selectionTypes = SelectionType.values();
        SelectionInteraction[] selectionInteractions = SelectionInteraction.values();

        for (SelectionType selectionType : selectionTypes) {
            for (SelectionInteraction interaction : selectionInteractions) {
                comp.startSelection(selectionType, interaction);
                comp.checkInvariant();
            }
        }
    }

    @Test
    public void testCreateSelectionFromShape() {
        comp.createSelectionFromShape(new Rectangle(3, 3, 5, 5));
        comp.checkInvariant();

        Shape shape = comp.getSelectionOrNull().getShape();
        assertEquals(new Rectangle(3, 3, 5, 5), shape);
    }

    @Test
    public void testEnlargeCanvas() {
        // remove one layer so that we have undo
        comp.removeLayer(comp.getActiveLayer(), AddToHistory.YES, UpdateGUI.NO);

        Rectangle origSelection = new Rectangle(2, 2, 3, 3);
        addRectangleSelection(origSelection);
        checkSelectionBounds(origSelection);

        checkCanvasSize(20, 10);
        checkActiveLayerAndMaskImageSize(20, 10);
        checkActiveLayerTranslation(0, 0);

        int north = 3;
        int east = 4;
        int south = 5;
        int west = 2;
        new EnlargeCanvas(north, east, south, west).process(comp);

        checkCanvasSize(26, 18);
        checkActiveLayerAndMaskImageSize(26, 18);
        checkActiveLayerTranslation(0, 0);
        Rectangle newSelection = new Rectangle(origSelection.x + west,
                origSelection.y + north, origSelection.width, origSelection.height);
        checkSelectionBounds(newSelection);

        comp.checkInvariant();
        checkDirty(true);

        History.undo();
        checkCanvasSize(20, 10);
        checkActiveLayerAndMaskImageSize(20, 10);
        checkActiveLayerTranslation(0, 0);
        checkSelectionBounds(origSelection);

        History.redo();
        checkCanvasSize(26, 18);
        checkActiveLayerAndMaskImageSize(26, 18);
        checkActiveLayerTranslation(0, 0);
        checkSelectionBounds(newSelection);

        // TODO test with translation
    }

    @Test
    public void testResize() {
        ImageLayer layer1 = (ImageLayer) comp.getLayer(0);
        ImageLayer layer2 = (ImageLayer) comp.getLayer(1);

        Rectangle origSelection = new Rectangle(4, 4, 8, 4);
        addRectangleSelection(origSelection);
        checkSelectionBounds(origSelection);

        String expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test that both layers are resized
        new Resize(10, 5, false).process(comp);

        Rectangle orig2Selection = new Rectangle(2, 2, 4, 2); // also scaled down by 2
        checkSelectionBounds(orig2Selection);

        expectedState = "{canvasWidth=10, canvasHeight=5, tx=0, ty=0, imgWidth=10, imgHeight=5}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test undo with one layer
        comp.removeLayer(layer2, AddToHistory.YES, UpdateGUI.NO);
        new Resize(5, 10, false).process(comp);
        expectedState = "{canvasWidth=5, canvasHeight=10, tx=0, ty=0, imgWidth=5, imgHeight=10}";
        assertEquals(expectedState, layer1.toDebugCanvasString());

        Rectangle newSelection = new Rectangle(1, 4, 2, 4); // x scaled down by 2, y scaled up by 2
        checkSelectionBounds(newSelection);

        History.undo();
        expectedState = "{canvasWidth=10, canvasHeight=5, tx=0, ty=0, imgWidth=10, imgHeight=5}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        checkSelectionBounds(orig2Selection);

        History.redo();
        expectedState = "{canvasWidth=5, canvasHeight=10, tx=0, ty=0, imgWidth=5, imgHeight=10}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        checkSelectionBounds(newSelection);

        // test with translation
        setStandardTestTranslation(layer1);

        // TODO clarify : target dimensions are canvas or image dimensions?
        new Resize(5, 10, false).process(comp);
        // TODO


    }

    @Test
    public void testRotate() {
        ImageLayer layer1 = (ImageLayer) comp.getLayer(0);
        ImageLayer layer2 = (ImageLayer) comp.getLayer(1);

        String expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test that both layers are rotated
        new Rotate(ANGLE_90).process(comp);
        expectedState = "{canvasWidth=10, canvasHeight=20, tx=0, ty=0, imgWidth=10, imgHeight=20}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test undo with one layer
        comp.removeLayer(layer2, AddToHistory.YES, UpdateGUI.NO);

        String origState = "{canvasWidth=10, canvasHeight=20, tx=0, ty=0, imgWidth=10, imgHeight=20}";
        assertEquals(origState, layer1.toDebugCanvasString());

        Rotate.SpecialAngle[] rotations = {ANGLE_90, ANGLE_180, ANGLE_270};
        for (Rotate.SpecialAngle angle : rotations) {
            new Rotate(angle).process(comp);

            String afterState;
            if (angle == ANGLE_90 || angle == ANGLE_270) {
                afterState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
            } else {
                afterState = origState;
            }
            assertEquals(afterState, layer1.toDebugCanvasString());

            History.undo();
            assertEquals(origState, layer1.toDebugCanvasString());

            History.redo();
            assertEquals(afterState, layer1.toDebugCanvasString());

            // undo again to get ready for the next angle
            History.undo();
            assertEquals(origState, layer1.toDebugCanvasString());
        }

        // test with translation
        setStandardTestTranslation(layer1);


        // TODO

        // test with selection

        // TODO
    }

    @Test
    public void testFlip() {
        ImageLayer layer1 = (ImageLayer) comp.getLayer(0);
        ImageLayer layer2 = (ImageLayer) comp.getLayer(1);

        String expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test that both layers are flipped
        new Flip(HORIZONTAL).process(comp);
        // no change
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());
        new Flip(VERTICAL).process(comp);
        // no change
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test undo with one layer
        comp.removeLayer(layer2, AddToHistory.YES, UpdateGUI.NO);
        assertEquals(expectedState, layer1.toDebugCanvasString());
        new Flip(HORIZONTAL).process(comp);
        new Flip(VERTICAL).process(comp);
        assertEquals(expectedState, layer1.toDebugCanvasString());
        History.undo();
        History.undo();
        assertEquals(expectedState, layer1.toDebugCanvasString());
        History.redo();
        History.redo();
        assertEquals(expectedState, layer1.toDebugCanvasString());

        // TODO
        // test with translation
        setStandardTestTranslation(layer1);

        // test with selection
    }

    @Test
    public void testCrop() {
        ImageLayer layer1 = (ImageLayer) comp.getLayer(0);
        ImageLayer layer2 = (ImageLayer) comp.getLayer(1);

        String expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test that both layers are cropped
        new Crop(new Rectangle(3, 3, 6, 3), false, false).process(comp);
        expectedState = "{canvasWidth=6, canvasHeight=3, tx=0, ty=0, imgWidth=6, imgHeight=3}";
        assertEquals(expectedState, layer1.toDebugCanvasString());
        assertEquals(expectedState, layer2.toDebugCanvasString());

        // test undo with one layer
        comp.removeLayer(layer2, AddToHistory.YES, UpdateGUI.NO);
        // TODO
        // test with translation
        setStandardTestTranslation(layer1);
        // TODO
        // test selection crop with selection
        // test crop tool crop with selection
        // test with allow growing
    }

    private void checkCanvasSize(int width, int height) {
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();
        assertEquals(width, canvasWidth);
        assertEquals(height, canvasHeight);
    }

    @Test
    public void testDragFinished() {
        Layer layer = comp.getLayer(0);
        comp.dragFinished(layer, 1);
        comp.checkInvariant();
    }

    private void testTranslation(boolean makeDuplicateLayer) {
        checkLayers("[layer 1, ACTIVE layer 2]");
        // remove one layer so that we have undo
        comp.removeLayer(comp.getActiveLayer(), AddToHistory.YES, UpdateGUI.NO);
        checkLayers("[ACTIVE layer 1]");

        checkActiveLayerTranslation(0, 0);
        checkActiveLayerAndMaskImageSize(20, 10);

        // 1. direction south-east
        moveLayer(makeDuplicateLayer, 2, 2);
        checkDirty(true);
        // no translation because the image was moved south-east, and enlarged
        checkActiveLayerTranslation(0, 0);
        checkActiveLayerAndMaskImageSize(22, 12);

        if (makeDuplicateLayer) {
            checkLayers("[layer 1, ACTIVE layer 1 copy]");
        } else {
            // no change in the number of layers
            checkLayers("[ACTIVE layer 1]");
        }

        // 2. direction north-west
        moveLayer(makeDuplicateLayer, -2, -2);
        // this time we have a non-zero translation
        checkActiveLayerTranslation(-2, -2);
        // no need to enlarge the image again
        checkActiveLayerAndMaskImageSize(22, 12);

        // 3. direction north-west again
        moveLayer(makeDuplicateLayer, -2, -2);
        // the translation increases
        checkActiveLayerTranslation(-4, -4);
        // the image needs to be enlarged now
        checkActiveLayerAndMaskImageSize(24, 14);

        // 4. direction north-east
        moveLayer(makeDuplicateLayer, 2, -2);
        // the translation increases
        checkActiveLayerTranslation(-2, -6);
        // the image needs to be enlarged vertically
        checkActiveLayerAndMaskImageSize(24, 16);

        // 5. opposite movement: direction south-west
        moveLayer(makeDuplicateLayer, -2, 2);
        // translation back to -4, -4
        checkActiveLayerTranslation(-4, -4);
        // no need to enlarge the image
        checkActiveLayerAndMaskImageSize(24, 16);

        if (makeDuplicateLayer) {
            checkLayers("[layer 1, layer 1 copy, layer 1 copy 2, layer 1 copy 3, layer 1 copy 4, ACTIVE layer 1 copy 5]");
        } else {
            // no change in the number of layers
            checkLayers("[ACTIVE layer 1]");
        }

        if (!makeDuplicateLayer) { // we should have undo in this case
            for (int i = 0; i < 5; i++) {
                History.undo();
            }
            checkActiveLayerTranslation(0, 0);
            checkActiveLayerAndMaskImageSize(20, 10);

            for (int i = 0; i < 5; i++) {
                History.redo();
            }
            checkActiveLayerTranslation(-4, -4);
            checkActiveLayerAndMaskImageSize(24, 16);
        }

        // now test "Layer to Canvas Size"
        comp.activeLayerToCanvasSize();
        checkActiveLayerTranslation(0, 0);
        checkActiveLayerAndMaskImageSize(20, 10);

        History.undo();
        checkActiveLayerTranslation(-4, -4);
        checkActiveLayerAndMaskImageSize(24, 16);

        History.redo();
        checkActiveLayerTranslation(0, 0);
        checkActiveLayerAndMaskImageSize(20, 10);
    }

    private void moveLayer(boolean makeDuplicateLayer, int relativeX, int relativeY) {
        comp.startMovement(makeDuplicateLayer);
        comp.moveActiveContentRelative(relativeX, relativeY);
        comp.endMovement();
    }

    private void checkActiveLayerTranslation(int tx, int ty) {
        ContentLayer layer = (ContentLayer) comp.getActiveLayer();
        assertEquals(tx, layer.getTranslationX());
        assertEquals(ty, layer.getTranslationY());
    }

    private void checkActiveLayerAndMaskImageSize(int w, int h) {
        ImageLayer layer = (ImageLayer) comp.getActiveLayer();
        BufferedImage image = layer.getImage();
        assertEquals(w, image.getWidth());
        assertEquals(h, image.getHeight());

        BufferedImage maskImage = layer.getMask().getImage();
        assertEquals(w, maskImage.getWidth());
        assertEquals(h, maskImage.getHeight());
    }

    private void checkLayers(String expected) {
        assertEquals(expected, comp.toLayerNamesString());
        comp.checkInvariant();
    }

    private void checkDirty(boolean expectedValue) {
        assertThat(comp.isDirty(), is(expectedValue));
    }

    private void setStandardTestTranslation(ImageLayer layer) {
        assert Assertions.translationIs(layer, 0, 0);
        assert comp.getActiveLayer() == layer;
        moveLayer(false, 2, 2);
        moveLayer(false, -4, -4);
        assert Assertions.translationIs(layer, -4, -4);
    }
}