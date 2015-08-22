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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Canvas;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.FgBgColors;
import pixelitor.TestHelper;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.selection.IgnoreSelection;
import pixelitor.selection.Selection;
import pixelitor.testutils.WithMask;
import pixelitor.tools.FgBgColorSelector;

import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static pixelitor.ChangeReason.OP_PREVIEW;

@RunWith(Parameterized.class)
public class ImageLayerTest {
    private ImageLayer layer;

    @Parameter
    public WithMask withMask;

    @Parameters(name = "{index}: mask = {0}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {WithMask.NO},
                {WithMask.YES},
        });
    }

    @Before
    public void setUp() {
        Composition comp = TestHelper.createEmptyComposition();
        layer = TestHelper.createImageLayer("layer 1", comp);
        comp.addLayerNoGUI(layer);

        withMask.init(layer);

        assert layer.getComp().checkInvariant();
    }

    @Test
    public void testGetImage() {
        BufferedImage image = layer.getImage();
        assertThat(image).isNotNull();
    }

    @Test
    public void testSetImage() {
        layer.setImage(TestHelper.createImage());
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
        layer.changePreviewImage(TestHelper.createImage(), "filterName", OP_PREVIEW);
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.PREVIEW);
    }

    @Test
    public void testChangePreviewImage_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.changePreviewImage(TestHelper.createImage(), "filterName", OP_PREVIEW);
        assertThat(layer.getState()).isEqualTo(ImageLayer.State.PREVIEW);
    }

    @Test
    public void testFilterWithoutDialogFinished() {
        ChangeReason[] values = ChangeReason.values();
        for (ChangeReason changeReason : values) {
            layer.filterWithoutDialogFinished(TestHelper.createImage(),
                    changeReason, "opName");
            assertThat(layer.getState()).isEqualTo(ImageLayer.State.NORMAL);
        }
    }

    @Test
    public void testChangeImageUndoRedo() {
        // TODO add selection
        layer.changeImageUndoRedo(TestHelper.createImage(),
                IgnoreSelection.NO);
        layer.changeImageUndoRedo(TestHelper.createImage(),
                IgnoreSelection.YES);
    }

    @Test
    public void testGetImageBounds() {
        Rectangle bounds = layer.getImageBounds();
        assertThat(bounds).isNotNull();
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

        BufferedImage imageT = layer.getSelectionSizedPartFrom(TestHelper.createImage(), selection, true);
        assertThat(imageT).isNotNull();

        BufferedImage imageF = layer.getSelectionSizedPartFrom(TestHelper.createImage(), selection, false);
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

    @Test
    public void testApplyLayerMask() {
        if (withMask == WithMask.YES) {
            FgBgColors.setGUI(new FgBgColorSelector() {
                @Override
                protected void setupKeyboardShortcuts() {
                    // do nothing - prevent initializing whe whole GUI
                }
            });

            assertThat(layer.hasMask()).isTrue();

            layer.applyLayerMask(AddToHistory.YES);
            assertThat(layer.hasMask()).isFalse();

            History.undo();
            assertThat(layer.hasMask()).isTrue();

            History.redo();
            assertThat(layer.hasMask()).isFalse();
        }
    }
}