/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
import pixelitor.layers.ImageLayer;
import pixelitor.layers.ImageLayerTest;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.utils.Optional;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CompositionTest {
    public static final int sizeX = 20;
    public static final int sizeY = 10;
    private Composition comp;

    public static Composition createEmptyTestComposition() {
        MockImageDisplay mockImageDisplay = new MockImageDisplay();
        Canvas canvas = new Canvas(mockImageDisplay, sizeX, sizeY);
        Composition c = new Composition(mockImageDisplay, new File("unit_test.jpg"), "Composition", canvas);

        return c;
    }

    public static Composition create2LayerTestComposition() {
        Composition c = createEmptyTestComposition();

        ImageLayer layer1 = ImageLayerTest.createTestImageLayer("layer 1", c);
        ImageLayer layer2 = ImageLayerTest.createTestImageLayer("layer 2", c);

        c.addLayer(layer1, false, false, false);
        c.addLayer(layer2, false, false, false);
        c.setActiveLayer(layer1, false);

        assert layer1 == c.getActiveLayer();
        assert layer1 == c.getLayer(0);
        assert layer2 == c.getLayer(1);

        return c;
    }

    @Before
    public void setUp() {
        comp = create2LayerTestComposition();
    }

    @Test
    public void testAddNewEmptyLayer() {
        comp.addNewEmptyLayer("newLayer", true);
        comp.addNewEmptyLayer("newLayer", false);
        assertEquals(4, comp.getNrLayers());
        comp.checkConsistency();
    }

    @Test
    public void testSetActiveLayer() {
        Layer layer = comp.getLayer(0);
        comp.setActiveLayer(layer, true);
        assertSame(layer, comp.getActiveLayer());
        comp.setActiveLayer(layer, false);
        assertSame(layer, comp.getActiveLayer());
        comp.checkConsistency();
    }

    @Test
    public void testGetNrLayers() {
        int nrLayers = comp.getNrLayers();
        assertEquals(2, nrLayers);
        comp.checkConsistency();
    }

    @Test
    public void testAddLayerNoGUI() {
        ImageLayer newLayer = ImageLayerTest.createTestImageLayer("layer", comp);
        comp.addLayerNoGUI(newLayer);
        assertEquals(3, comp.getNrLayers());
        comp.checkConsistency();
    }

    @Test
    public void testAddLayer() {
        ImageLayer newLayer = ImageLayerTest.createTestImageLayer("layer", comp);
        comp.addLayer(newLayer, true, true, true);
        comp.addLayer(newLayer, true, true, false);
        comp.addLayer(newLayer, true, false, true);
        comp.addLayer(newLayer, true, false, false);
        comp.addLayer(newLayer, false, true, true);
        comp.addLayer(newLayer, false, true, false);
        comp.addLayer(newLayer, false, false, true);
        comp.addLayer(newLayer, false, false, false);

        comp.addLayer(newLayer, true, true, 0);
        comp.addLayer(newLayer, true, false, 0);
        comp.addLayer(newLayer, false, true, 0);
        comp.addLayer(newLayer, false, false, 0);

        comp.addLayer(newLayer, true, true, 1);
        comp.addLayer(newLayer, true, false, 1);
        comp.addLayer(newLayer, false, true, 1);
        comp.addLayer(newLayer, false, false, 1);

        assertEquals(18, comp.getNrLayers());
        comp.checkConsistency();
    }

    @Test
    public void testDuplicateLayer() {
        comp.duplicateLayer();
        assertEquals(3, comp.getNrLayers());
        comp.checkConsistency();
    }

    @Test
    public void testGetActiveLayer() {
        Layer activeLayer = comp.getActiveLayer();
        assertNotNull(activeLayer);
        comp.checkConsistency();
    }

    @Test
    public void testGetActiveLayerIndex() {
        int index = comp.getActiveLayerIndex();
        assertEquals(0, index);
        comp.checkConsistency();
    }

    @Test
    public void testFilterWithoutDialogFinished() {
        BufferedImage image = ImageLayerTest.createTestImage();
        comp.filterWithoutDialogFinished(image, ChangeReason.OP_WITHOUT_DIALOG, "opName");
        comp.checkConsistency();
    }

    @Test
    public void testGetActiveImageLayer() {
        ImageLayer imageLayer = comp.getActiveImageLayer();
        assertNotNull(imageLayer);
        comp.checkConsistency();
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
        comp.checkConsistency();
    }

    @Test
    public void testGetFilterSource() {
        BufferedImage image = comp.getFilterSource();
        assertNotNull(image);
        comp.checkConsistency();
    }

    @Test
    public void testGetCanvas() {
        Canvas canvas = comp.getCanvas();
        assertNotNull(canvas);
        comp.checkConsistency();
    }

    @Test
    public void testIsEmpty() {
        boolean empty = comp.isEmpty();
        assertFalse(empty);
        comp.checkConsistency();
    }

    @Test
    public void testGetName() {
        String name = comp.getName();
        assertNotNull(name);
        comp.checkConsistency();
    }

    @Test
    public void testStartTranslation() {
        comp.startTranslation(true);
        comp.startTranslation(false);
        comp.checkConsistency();
    }

    @Test
    public void testEndTranslation() {
        comp.endTranslation();
    }

    @Test
    public void testGetLayer() {
        Layer layer = comp.getLayer(0);
        assertNotNull(layer);
        comp.checkConsistency();
    }

    @Test
    public void testFlattenImage() {
        comp.flattenImage(false);
        comp.checkConsistency();
    }

    @Test
    public void testMergeDown() {
        comp.mergeDown();
        comp.checkConsistency();
    }

    @Test
    public void testMoveActiveLayer() {
        comp.moveActiveLayer(true);
        comp.checkConsistency();
        comp.moveActiveLayer(false);
        comp.checkConsistency();
    }

    @Test
    public void testMoveActiveLayerToTop() {
        comp.moveActiveLayerToTop();
        comp.checkConsistency();
    }

    @Test
    public void testMoveActiveLayerToBottom() {
        comp.moveActiveLayerToBottom();
        comp.checkConsistency();
    }

    @Test
    public void testSwapLayers() {
        comp.swapLayers(0, 1, false);
        comp.checkConsistency();
        comp.swapLayers(0, 1, true);
        comp.checkConsistency();
    }

    @Test
    public void testMoveLayerSelectionUp() {
        comp.moveLayerSelectionUp();
        comp.checkConsistency();
    }

    @Test
    public void testMoveLayerSelectionDown() {
        comp.moveLayerSelectionDown();
        comp.checkConsistency();
    }

    @Test
    public void testSetCanvas() {
        comp.setCanvas(new Canvas(null, sizeX, sizeY));
        comp.checkConsistency();
    }

    @Test
    public void testGenerateNewLayerName() {
        String newLayerName = comp.generateNewLayerName();
        assertEquals("layer 1", newLayerName);
        comp.checkConsistency();
    }

    @Test
    public void testIsDirty() {
        boolean dirty = comp.isDirty();
        assertFalse(dirty);
        comp.checkConsistency();
    }

    @Test
    public void testUpdateRegion() {
        comp.updateRegion(4, 4, 8, 8, 2);
        comp.checkConsistency();
    }

    @Test
    public void testSetImageComponent() {
        comp.setImageComponent(null);
        comp.checkConsistency();
    }

    @Test
    public void testGetCanvasBounds() {
        Rectangle bounds = comp.getCanvasBounds();
        assertNotNull(bounds);
        comp.checkConsistency();
    }

    @Test
    public void testGetFile() {
        File file = comp.getFile();
        assertNotNull(file);
        comp.checkConsistency();
    }

    @Test
    public void testSetFile() {
        comp.setFile(new File("unit_test.jpg"));
        comp.checkConsistency();
    }

    @Test
    public void testRemoveActiveLayer() {
        comp.removeActiveLayer();
        assertEquals(1, comp.getNrLayers());
        comp.checkConsistency();
    }

    @Test
    public void testRemoveLayer() {
        Layer layer2 = comp.getLayer(0);
        comp.removeLayer(layer2, true);
        assertEquals(1, comp.getNrLayers());
        comp.checkConsistency();
    }

    @Test
    public void testDispose() {
        comp.dispose();
        comp.checkConsistency();
    }

    @Test
    public void testAddNewLayerFromComposite() {
        comp.addNewLayerFromComposite("composite layer");
        assertEquals(3, comp.getNrLayers());
        comp.checkConsistency();
    }

    @Test
    public void testPaintSelection() {
        Graphics2D g2 = ImageLayerTest.createTestImage().createGraphics();
        comp.paintSelection(g2);
        comp.checkConsistency();
    }

    @Test
    public void testDeselect() {
        comp.deselect(false);
        comp.checkConsistency();
        comp.deselect(true);
        comp.checkConsistency();
    }

    @Test
    public void testGetSelection() {
        Optional<Selection> selection = comp.getSelection();
        assertFalse(selection.isPresent());

        comp.startSelection(SelectionType.ELLIPSE, SelectionInteraction.ADD);

        selection = comp.getSelection();
        assertTrue(selection.isPresent());
        comp.checkConsistency();
    }

    @Test
    public void testHasSelection() {
        assertFalse(comp.hasSelection());

        comp.startSelection(SelectionType.RECTANGLE, SelectionInteraction.ADD);
        assertTrue(comp.hasSelection());
        comp.checkConsistency();
    }

    @Test
    public void testGetCompositeImage() {
        BufferedImage image = comp.getCompositeImage();
        assertNotNull(image);
        comp.checkConsistency();
    }

    @Test
    public void testImageChanged() {
        comp.imageChanged(true, true);
        comp.checkConsistency();
        comp.imageChanged(true, false);
        comp.checkConsistency();
        comp.imageChanged(false, true);
        comp.checkConsistency();
        comp.imageChanged(false, false);
        comp.checkConsistency();
    }

    @Test
    public void testSetDirty() {
        comp.setDirty(true);
        assertTrue(comp.isDirty());
        comp.checkConsistency();
    }

    @Test
    public void testMoveActiveContentRelative() {
        comp.moveActiveContentRelative(2, 2, false);
        comp.checkConsistency();
        comp.moveActiveContentRelative(2, 2, true);
        comp.checkConsistency();
    }

    @Test
    public void testIsActiveLayer() {
        Layer layer = comp.getLayer(0);
        boolean b = comp.isActiveLayer(layer);
        assertTrue(b);
        comp.checkConsistency();
    }

    @Test
    public void testSetSelectionClipping() {
        Graphics2D g2 = ImageLayerTest.createTestImage().createGraphics();
        comp.setSelectionClipping(g2, AffineTransform.getTranslateInstance(1, 1));
        comp.checkConsistency();
    }

    @Test
    public void testInvertSelection() {
        comp.invertSelection();
        comp.checkConsistency();

        comp.startSelection(SelectionType.RECTANGLE, SelectionInteraction.ADD);
        comp.getSelection().get().setShape(new Rectangle(3, 3, 4, 4));

        comp.invertSelection();
        comp.checkConsistency();
    }

    @Test
    public void testStartSelection() {
        SelectionType[] selectionTypes = SelectionType.values();
        SelectionInteraction[] selectionInteractions = SelectionInteraction.values();

        for (SelectionType selectionType : selectionTypes) {
            for (SelectionInteraction interaction : selectionInteractions) {
                comp.startSelection(selectionType, interaction);
                comp.checkConsistency();
            }
        }
    }

    @Test
    public void testCreateSelectionFromShape() {
        comp.createSelectionFromShape(new Rectangle(3, 3, 5, 5));
        comp.checkConsistency();
    }

    @Test
    public void testLayerToCanvasSize() {
        comp.layerToCanvasSize();
        comp.checkConsistency();
    }

    @Test
    public void testEnlargeCanvas() {
        comp.enlargeCanvas(3, 4, 5, -2);
        comp.checkConsistency();
    }

    @Test
    public void testGetCanvasWidth() {
        int canvasWidth = comp.getCanvasWidth();
        assertEquals(sizeX, canvasWidth);
        comp.checkConsistency();
    }

    @Test
    public void testGetCanvasHeight() {
        int canvasHeight = comp.getCanvasHeight();
        assertEquals(sizeY, canvasHeight);
        comp.checkConsistency();
    }

    @Test
    public void testDragFinished() {
        Layer layer = comp.getLayer(0);
        comp.dragFinished(layer, 1);
        comp.checkConsistency();
    }
}