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

package pixelitor.layers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.CompositionTest;
import pixelitor.filters.comp.Flip;
import pixelitor.history.TranslateEdit;
import pixelitor.selection.Selection;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ImageLayerTest {
    private Composition comp;
    private ImageLayer layer;

    public static BufferedImage createTestImage() {
        return new BufferedImage(CompositionTest.sizeX, CompositionTest.sizeY, BufferedImage.TYPE_INT_ARGB);
    }

    public static ImageLayer createTestImageLayer(String layerName, Composition comp) {
        BufferedImage image = createTestImage();
        return new ImageLayer(comp, image, layerName);
    }

    @Before
    public void setUp() {
        comp = CompositionTest.createEmptyTestComposition();
        layer = createTestImageLayer("layer 1", comp);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testDuplicate() {
        ImageLayer duplicate = layer.duplicate();
        assertNotNull(duplicate);
    }

    @Test
    public void testGetImage() {
        BufferedImage image = layer.getImage();
        assertNotNull(image);
    }

    @Test
    public void testSetImage() {
        layer.setImage(createTestImage());
    }

    @Test
    public void testStartPreviewing() {
        layer.startPreviewing();
        assertEquals(ImageLayer.State.PREVIEW, layer.getState());
    }

    @Test
    public void testStartNewPreviewFromDialog() {
        layer.startNewPreviewFromDialog();
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test
    public void testOkPressedInDialog() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode
        layer.okPressedInDialog("filterName");
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test
    public void testCancelPressedInDialog() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.cancelPressedInDialog();
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test
    public void testTweenCalculatingStarted() {
        layer.tweenCalculatingStarted();
        assertEquals(ImageLayer.State.PREVIEW, layer.getState());
    }

    @Test
    public void testTweenCalculatingEnded() {
        layer.tweenCalculatingStarted(); // make sure that the layer is in PREVIEW mode

        layer.tweenCalculatingEnded();
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test
    public void testChangePreviewImage() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.changePreviewImage(createTestImage(), "filterName");
        assertEquals(ImageLayer.State.PREVIEW, layer.getState());
    }

    @Test
    public void testFilterWithoutDialogFinished() {
        ChangeReason[] values = ChangeReason.values();
        for (ChangeReason changeReason : values) {
            layer.filterWithoutDialogFinished(createTestImage(),
                    changeReason, "opName");
            assertEquals(ImageLayer.State.NORMAL, layer.getState());
        }
    }

    @Test
    public void testChangeImageUndoRedo() {
        layer.changeImageUndoRedo(createTestImage());
    }

    @Test
    public void testGetBounds() {
        Rectangle bounds = layer.getBounds();
        assertNotNull(bounds);
    }

    @Test
    public void testCheckForLayerEnlargement() {
        boolean b = layer.checkForLayerEnlargement();
    }

    @Test
    public void testEnlargeLayer() {
        layer.enlargeLayer();
    }

    @Test
    public void testGetImageForFilterDialogs() {
        BufferedImage image = layer.getImageForFilterDialogs();
        assertNotNull(image);
    }

    @Test
    public void testFlip() {
        layer.flip(Flip.Direction.HORIZONTAL);
        layer.flip(Flip.Direction.VERTICAL);
    }

    @Test
    public void testRotate() {
        layer.rotate(90);
        layer.rotate(180);
        layer.rotate(270);
    }

    @Test
    public void testMergeDownOn() {
        layer.mergeDownOn(createTestImageLayer("layer 2", comp));
    }

    @Test
    public void testCreateTmpDrawingLayer() {
        TmpDrawingLayer drawingLayer1 = layer.createTmpDrawingLayer(AlphaComposite.SrcOver, true);
        assertNotNull(drawingLayer1);
        TmpDrawingLayer drawingLayer2 = layer.createTmpDrawingLayer(AlphaComposite.SrcOver, false);
        assertNotNull(drawingLayer2);
    }

    @Test
    public void testMergeTmpDrawingImageDown() {
        layer.mergeTmpDrawingImageDown();
    }

    @Test
    public void testCreateCompositionSizedTmpImage() {
        BufferedImage image = layer.createCompositionSizedTmpImage();
        assertNotNull(image);
    }

    @Test
    public void testCreateCompositionSizedSubImage() {
        BufferedImage image = layer.createCompositionSizedSubImage();
        assertNotNull(image);
    }

    @Test
    public void testGetFilterSourceImage() {
        BufferedImage image = layer.getFilterSourceImage();
        assertNotNull(image);
    }

    @Test
    public void testGetImageOrSubImageIfSelected() {
        BufferedImage imageTT = layer.getImageOrSubImageIfSelected(true, true);
        assertNotNull(imageTT);

        BufferedImage imageTF = layer.getImageOrSubImageIfSelected(true, false);
        assertNotNull(imageTF);

        BufferedImage imageFT = layer.getImageOrSubImageIfSelected(false, true);
        assertNotNull(imageFT);

        BufferedImage imageFF = layer.getImageOrSubImageIfSelected(false, false);
        assertNotNull(imageFF);
    }

    @Test
    public void testGetSelectionSizedPartFrom() {
        layer.getComposition().createSelectionFromShape(new Rectangle(2, 2, 10, 10));
        Selection selection = layer.getComposition().getSelection().get();

        BufferedImage imageT = layer.getSelectionSizedPartFrom(createTestImage(), selection, true);
        assertNotNull(imageT);

        BufferedImage imageF = layer.getSelectionSizedPartFrom(createTestImage(), selection, false);
        assertNotNull(imageF);
    }

    @Test
    public void testCropToCanvasSize() {
        layer.cropToCanvasSize();
    }

    @Test
    public void testEnlargeCanvas() {
        layer.enlargeCanvas(5, 5, 5, 10);
    }

    @Test
    public void testCreateTranslateEdit() {
        TranslateEdit edit = layer.createTranslateEdit(5, 5);
        assertNotNull(edit);
    }

    @Test
    public void testResize() {
        layer.resize(20, 20, true);
        layer.resize(20, 20, false);

        layer.resize(30, 25, true);
        layer.resize(25, 30, false);

        layer.resize(5, 5, true);
        layer.resize(20, 20, false);
    }

    @Test
    public void testCrop() {
        layer.crop(new Rectangle(3, 3, 5, 5));
    }

    @Test
    public void testPaintLayerOnGraphics() {
        Graphics2D g2 = createTestImage().createGraphics();
        layer.paintLayerOnGraphics(g2, true);
        layer.paintLayerOnGraphics(g2, false);
    }
}