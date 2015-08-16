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
import pixelitor.Canvas;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.selection.IgnoreSelection;
import pixelitor.selection.Selection;

import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static pixelitor.ChangeReason.OP_PREVIEW;

public class ImageLayerTest {
    private Composition comp;
    private ImageLayer layer;

    @Before
    public void setUp() {
        comp = TestHelper.createEmptyTestComposition();
        layer = TestHelper.createTestImageLayer("layer 1", comp);
        comp.addLayerNoGUI(layer);

        assert layer.getComp().checkInvariant();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetImage() {
        BufferedImage image = layer.getImage();
        assertThat(image).isNotNull();
    }

    @Test
    public void testSetImage() {
        layer.setImage(TestHelper.createTestImage());
    }

    @Test
    public void testStartPreviewing() {
        layer.startPreviewing();
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.PREVIEW);
    }

    @Test
    public void testOkPressedInDialog() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode
        layer.okPressedInDialog("filterName");
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.NORMAL);
    }

    @Test(expected = AssertionError.class)
    public void testCancelPressedInDialog_Fail() {
        layer.cancelPressedInDialog();
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.NORMAL);
    }

    @Test
    public void testCancelPressedInDialog_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.cancelPressedInDialog();
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.NORMAL);
    }

    @Test
    public void testTweenCalculatingStarted() {
        layer.tweenCalculatingStarted();
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.PREVIEW);
    }

    @Test(expected = AssertionError.class)
    public void testTweenCalculatingEnded_Fail() {
        layer.tweenCalculatingEnded();
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.NORMAL);
    }

    @Test
    public void testTweenCalculatingEnded_OK() {
        layer.tweenCalculatingStarted(); // make sure that the layer is in PREVIEW mode

        layer.tweenCalculatingEnded();
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.NORMAL);
    }

    @Test(expected = IllegalStateException.class)
    public void testChangePreviewImage_Fail() {
        layer.changePreviewImage(TestHelper.createTestImage(), "filterName", OP_PREVIEW);
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.PREVIEW);
    }

    @Test
    public void testChangePreviewImage_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.changePreviewImage(TestHelper.createTestImage(), "filterName", OP_PREVIEW);
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.PREVIEW);
    }

    @Test
    public void testFilterWithoutDialogFinished() {
        ChangeReason[] values = ChangeReason.values();
        for (ChangeReason changeReason : values) {
            layer.filterWithoutDialogFinished(TestHelper.createTestImage(),
                    changeReason, "opName");
            assertThat(layer.getState()).isEqualTo(ImageLayer.State.NORMAL);
        }
    }

    @Test
    public void testChangeImageUndoRedo() {
        layer.changeImageUndoRedo(TestHelper.createTestImage(),
                IgnoreSelection.NO);
        layer.changeImageUndoRedo(TestHelper.createTestImage(),
                IgnoreSelection.YES);
    }

    @Test
    public void testGetImageBounds() {
        Rectangle bounds = layer.getImageBounds();
        assertThat(bounds).isNotNull();
    }

    @Test
    public void testCheckImageDoesNotCoverCanvas() {
        boolean b = layer.checkImageDoesNotCoverCanvas();
    }

    @Test
    public void testEnlargeLayer() {
        layer.enlargeLayer();
    }

    @Test
    public void testGetImageForFilterDialogs() {
        BufferedImage image = layer.getImageForFilterDialogs();
        assertThat(image).isNotNull();
    }

    @Test
    public void testCreateTmpDrawingLayer() {
        TmpDrawingLayer tmpDrawingLayer1 = layer.createTmpDrawingLayer(AlphaComposite.SrcOver, true);
        assertThat(tmpDrawingLayer1).isNotNull();
        TmpDrawingLayer tmpDrawingLayer2 = layer.createTmpDrawingLayer(AlphaComposite.SrcOver, false);
        assertThat(tmpDrawingLayer2).isNotNull();
    }

    @Test
    public void testMergeTmpDrawingImageDown() {
        layer.mergeTmpDrawingLayerDown();
    }

    @Test
    public void testCreateCompositionSizedTmpImage() {
        BufferedImage image = layer.createCompositionSizedTmpImage();
        assertThat(image).isNotNull();
    }

    @Test
    public void testGetCanvasSizedSubImage() {
        // TODO would be better with translation
        BufferedImage image = layer.getCanvasSizedSubImage();
        assertThat(image).isNotNull();
        Canvas canvas = layer.getComp().getCanvas();
        assert image.getWidth() == canvas.getWidth();
        assert image.getHeight() == canvas.getHeight();
    }

    @Test
    public void testGetFilterSourceImage() {
        BufferedImage image = layer.getFilterSourceImage();
        assertThat(image).isNotNull();
    }

    @Test
    public void testGetImageOrSubImageIfSelected() {
        BufferedImage imageTT = layer.getImageOrSubImageIfSelected(true, true);
        assertThat(imageTT).isNotNull();

        BufferedImage imageTF = layer.getImageOrSubImageIfSelected(true, false);
        assertThat(imageTF).isNotNull();

        BufferedImage imageFT = layer.getImageOrSubImageIfSelected(false, true);
        assertThat(imageFT).isNotNull();

        BufferedImage imageFF = layer.getImageOrSubImageIfSelected(false, false);
        assertThat(imageFF).isNotNull();
    }

    @Test
    public void testGetSelectionSizedPartFrom() {
        layer.getComp().createSelectionFromShape(new Rectangle(2, 2, 10, 10));
        Selection selection = layer.getComp().getSelection().get();

        BufferedImage imageT = layer.getSelectionSizedPartFrom(TestHelper.createTestImage(), selection, true);
        assertThat(imageT).isNotNull();

        BufferedImage imageF = layer.getSelectionSizedPartFrom(TestHelper.createTestImage(), selection, false);
        assertThat(imageF).isNotNull();
    }

    @Test
    public void testCropToCanvasSize() {
        layer.cropToCanvasSize();
    }

    @Test
    public void testDuplicate() {
        ImageLayer duplicate = layer.duplicate();
        assertThat(duplicate).isNotNull();

        BufferedImage image = layer.getImage();
        BufferedImage duplicateImage = duplicate.getImage();

        assertNotSame(duplicateImage, image);
        assertThat(image.getWidth()).isEqualTo(duplicateImage.getWidth());
        assertThat(image.getHeight()).isEqualTo(duplicateImage.getHeight());

        assertThat(duplicate.getImageBounds()).isEqualTo(layer.getImageBounds());
        assertSame(layer.getBlendingMode(), duplicate.getBlendingMode());
        assertThat(duplicate.getOpacity()).isEqualTo(layer.getOpacity());
    }

}