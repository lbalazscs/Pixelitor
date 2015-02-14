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
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.HISTOGRAM;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;
import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.selection.SelectionInteraction.ADD;
import static pixelitor.selection.SelectionType.ELLIPSE;
import static pixelitor.selection.SelectionType.RECTANGLE;

public class CompositionTest {
    private Composition comp;

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerTestComposition();
    }

    @Test
    public void testAddNewEmptyLayer() {
        comp.addNewEmptyLayer("newLayer", true);
        comp.addNewEmptyLayer("newLayer", false);
        assertEquals(4, comp.getNrLayers());
        comp.checkInvariant();
    }

    @Test
    public void testSetActiveLayer() {
        Layer layer = comp.getLayer(0);
        comp.setActiveLayer(layer, true);
        assertSame(layer, comp.getActiveLayer());
        comp.setActiveLayer(layer, false);
        assertSame(layer, comp.getActiveLayer());
        comp.checkInvariant();
    }

    @Test
    public void testGetNrLayers() {
        int nrLayers = comp.getNrLayers();
        assertEquals(2, nrLayers);
        comp.checkInvariant();
    }

    @Test
    public void testAddLayerNoGUI() {
        ImageLayer newLayer = TestHelper.createTestImageLayer("layer", comp);
        comp.addLayerNoGUI(newLayer);
        assertEquals(3, comp.getNrLayers());
        comp.checkInvariant();
    }

    @Test
    public void testAddLayer() {
        ImageLayer newLayer = TestHelper.createTestImageLayer("layer", comp);
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
        comp.checkInvariant();
    }

    @Test
    public void testDuplicateLayer() {
        comp.duplicateLayer();
        assertEquals(3, comp.getNrLayers());
        comp.checkInvariant();
    }

    @Test
    public void testGetActiveLayer() {
        Layer activeLayer = comp.getActiveLayer();
        assertNotNull(activeLayer);
        comp.checkInvariant();
    }

    @Test
    public void testGetActiveLayerIndex() {
        int index = comp.getActiveLayerIndex();
        assertThat(index, equalTo(0));
        comp.checkInvariant();
    }

    @Test
    public void testFilterWithoutDialogFinished() {
        BufferedImage image = TestHelper.createTestImage();
        comp.filterWithoutDialogFinished(image, ChangeReason.OP_WITHOUT_DIALOG, "opName");
        comp.checkInvariant();
    }

    @Test
    public void testGetActiveImageLayer() {
        ImageLayer imageLayer = comp.getActiveImageLayer();
        assertNotNull(imageLayer);
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
        comp.checkInvariant();
    }

    @Test
    public void testIsEmpty() {
        boolean empty = comp.isEmpty();
        assertThat(empty, is(false));
        comp.checkInvariant();
    }

    @Test
    public void testStartTranslation() {
        comp.startTranslation(true);
        comp.startTranslation(false);
        comp.checkInvariant();
    }

    @Test
    public void testEndTranslation() {
        comp.endTranslation();
    }

    @Test
    public void testGetLayer() {
        Layer layer = comp.getLayer(0);
        assertNotNull(layer);
        comp.checkInvariant();
    }

    @Test
    public void testFlattenImage() {
        comp.flattenImage(false);
        comp.checkInvariant();
    }

    @Test
    public void testMergeDown() {
        comp.mergeDown();
        comp.checkInvariant();
    }

    @Test
    public void testMoveActiveLayer() {
        comp.moveActiveLayer(true);
        comp.checkInvariant();
        comp.moveActiveLayer(false);
        comp.checkInvariant();
    }

    @Test
    public void testMoveActiveLayerToTop() {
        comp.moveActiveLayerToTop();
        comp.checkInvariant();
    }

    @Test
    public void testMoveActiveLayerToBottom() {
        comp.moveActiveLayerToBottom();
        comp.checkInvariant();
    }

    @Test
    public void testSwapLayers() {
        comp.swapLayers(0, 1, false);
        comp.checkInvariant();
        comp.swapLayers(0, 1, true);
        comp.checkInvariant();
    }

    @Test
    public void testMoveLayerSelectionUp() {
        comp.moveLayerSelectionUp();
        comp.checkInvariant();
    }

    @Test
    public void testMoveLayerSelectionDown() {
        comp.moveLayerSelectionDown();
        comp.checkInvariant();
    }

    @Test
    public void testSetCanvas() {
        comp.setCanvas(new Canvas(TestHelper.sizeX, TestHelper.sizeY));
        comp.checkInvariant();
    }

    @Test
    public void testGenerateNewLayerName() {
        String newLayerName = comp.generateNewLayerName();
        assertEquals("layer 1", newLayerName);
        comp.checkInvariant();
    }

    @Test
    public void testIsDirty() {
        boolean dirty = comp.isDirty();
        assertThat(dirty, is(false));
        comp.checkInvariant();
    }

    @Test
    public void testUpdateRegion() {
        comp.updateRegion(4, 4, 8, 8, 2);
        comp.checkInvariant();
    }

    @Test
    public void testSetImageComponent() {
        comp.setImageComponent(null);
        comp.checkInvariant();
    }

    @Test
    public void testGetCanvasBounds() {
        Rectangle bounds = comp.getCanvasBounds();
        assertNotNull(bounds);
        comp.checkInvariant();
    }

    @Test
    public void testRemoveActiveLayer() {
        comp.removeActiveLayer();
        assertEquals(1, comp.getNrLayers());
        comp.checkInvariant();
    }

    @Test
    public void testRemoveLayer() {
        Layer layer2 = comp.getLayer(0);
        comp.removeLayer(layer2, true);
        assertEquals(1, comp.getNrLayers());
        comp.checkInvariant();
    }

    @Test
    public void testDispose() {
        comp.dispose();
        comp.checkInvariant();
    }

    @Test
    public void testAddNewLayerFromComposite() {
        comp.addNewLayerFromComposite("composite layer");
        assertEquals(3, comp.getNrLayers());
        comp.checkInvariant();
    }

    @Test
    public void testPaintSelection() {
        Graphics2D g2 = TestHelper.createGraphics();
        comp.paintSelection(g2);
        comp.checkInvariant();
    }

    @Test
    public void testDeselect() {
        comp.deselect(false);
        comp.checkInvariant();
        comp.deselect(true);
        comp.checkInvariant();
    }

    @Test
    public void testGetSelection() {
        Optional<Selection> selection = comp.getSelection();
        assertThat(selection.isPresent(), is(false));

        comp.startSelection(ELLIPSE, ADD);

        selection = comp.getSelection();

        assertThat(selection.isPresent(), is(true));
        comp.checkInvariant();
    }

    @Test
    public void testHasSelection() {
        assertThat(comp.hasSelection(), is(false));

        comp.startSelection(RECTANGLE, ADD);
        assertThat(comp.hasSelection(), is(true));
        comp.checkInvariant();
    }

    @Test
    public void testGetCompositeImage() {
        BufferedImage image = comp.getCompositeImage();
        assertNotNull(image);
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
    public void testSetDirty() {
        comp.setDirty(true);
        assertThat(comp.isDirty(), is(true));
        comp.checkInvariant();
    }

    @Test
    public void testMoveActiveContentRelative() {
        comp.moveActiveContentRelative(2, 2);
        comp.checkInvariant();
    }

    @Test
    public void testIsActiveLayer() {
        Layer layer = comp.getLayer(0);
        boolean b = comp.isActiveLayer(layer);
        assertThat(b, is(true));
        comp.checkInvariant();
    }

    @Test
    public void testSetSelectionClipping() {
        Graphics2D g2 = TestHelper.createGraphics();
        comp.setSelectionClipping(g2, AffineTransform.getTranslateInstance(1, 1));
        comp.checkInvariant();
    }

    @Test
    public void testInvertSelection() {
        comp.invertSelection();
        comp.checkInvariant();

        comp.startSelection(SelectionType.RECTANGLE, SelectionInteraction.ADD);
        comp.getSelection().get().setShape(new Rectangle(3, 3, 4, 4));

        comp.invertSelection();
        comp.checkInvariant();
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
    }

    @Test
    public void testLayerToCanvasSize() {
        comp.layerToCanvasSize();
        comp.checkInvariant();
    }

    @Test
    public void testEnlargeCanvas() {
        comp.enlargeCanvas(3, 4, 5, -2);
        comp.checkInvariant();
    }

    @Test
    public void testGetCanvasWidth() {
        int canvasWidth = comp.getCanvasWidth();
        assertEquals(TestHelper.sizeX, canvasWidth);
        comp.checkInvariant();
    }

    @Test
    public void testGetCanvasHeight() {
        int canvasHeight = comp.getCanvasHeight();
        assertEquals(TestHelper.sizeY, canvasHeight);
        comp.checkInvariant();
    }

    @Test
    public void testDragFinished() {
        Layer layer = comp.getLayer(0);
        comp.dragFinished(layer, 1);
        comp.checkInvariant();
    }
}