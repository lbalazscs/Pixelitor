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

package pixelitor.layers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.selection.Selection;

import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static pixelitor.ChangeReason.OP_PREVIEW;

public class ImageLayerTest {
    private Composition comp;
    private ImageLayer layer;

    @Before
    public void setUp() {
        comp = TestHelper.createEmptyTestComposition();
        layer = TestHelper.createTestImageLayer("layer 1", comp);

        assert layer.getComposition().checkInvariant();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetImage() {
        BufferedImage image = layer.getImage();
        assertNotNull(image);
    }

    @Test
    public void testSetImage() {
        layer.setImage(TestHelper.createTestImage());
    }

    @Test
    public void testStartPreviewing() {
        layer.startPreviewing();
        assertEquals(ImageLayer.State.PREVIEW, layer.getState());
    }

    @Test
    public void testOkPressedInDialog() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode
        layer.okPressedInDialog("filterName");
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test(expected = AssertionError.class)
    public void testCancelPressedInDialog_Fail() {
        layer.cancelPressedInDialog();
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test
    public void testCancelPressedInDialog_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.cancelPressedInDialog();
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test
    public void testTweenCalculatingStarted() {
        layer.tweenCalculatingStarted();
        assertEquals(ImageLayer.State.PREVIEW, layer.getState());
    }

    @Test(expected = AssertionError.class)
    public void testTweenCalculatingEnded_Fail() {
        layer.tweenCalculatingEnded();
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test
    public void testTweenCalculatingEnded_OK() {
        layer.tweenCalculatingStarted(); // make sure that the layer is in PREVIEW mode

        layer.tweenCalculatingEnded();
        assertEquals(ImageLayer.State.NORMAL, layer.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void testChangePreviewImage_Fail() {
        layer.changePreviewImage(TestHelper.createTestImage(), "filterName", OP_PREVIEW);
        assertEquals(ImageLayer.State.PREVIEW, layer.getState());
    }

    @Test
    public void testChangePreviewImage_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.changePreviewImage(TestHelper.createTestImage(), "filterName", OP_PREVIEW);
        assertEquals(ImageLayer.State.PREVIEW, layer.getState());
    }

    @Test
    public void testFilterWithoutDialogFinished() {
        ChangeReason[] values = ChangeReason.values();
        for (ChangeReason changeReason : values) {
            layer.filterWithoutDialogFinished(TestHelper.createTestImage(),
                    changeReason, "opName");
            assertEquals(ImageLayer.State.NORMAL, layer.getState());
        }
    }

    @Test
    public void testChangeImageUndoRedo() {
        layer.changeImageUndoRedo(TestHelper.createTestImage());
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
        BufferedImage image = layer.getCompositionSizedSubImage();
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

        BufferedImage imageT = layer.getSelectionSizedPartFrom(TestHelper.createTestImage(), selection, true);
        assertNotNull(imageT);

        BufferedImage imageF = layer.getSelectionSizedPartFrom(TestHelper.createTestImage(), selection, false);
        assertNotNull(imageF);
    }

    @Test
    public void testCropToCanvasSize() {
        layer.cropToCanvasSize();
    }
}