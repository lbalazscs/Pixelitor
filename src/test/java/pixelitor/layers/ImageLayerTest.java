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

package pixelitor.layers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.TestHelper;
import pixelitor.history.AddToHistory;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.selection.IgnoreSelection;
import pixelitor.selection.Selection;
import pixelitor.testutils.WithMask;
import pixelitor.testutils.WithTranslation;
import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pixelitor.ChangeReason.OP_PREVIEW;
import static pixelitor.ChangeReason.OP_WITHOUT_DIALOG;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;
import static pixelitor.layers.ImageLayer.State.NORMAL;
import static pixelitor.layers.ImageLayer.State.PREVIEW;

@RunWith(Parameterized.class)
public class ImageLayerTest {
    private ImageLayer layer;

    @Parameter
    public WithMask withMask;

    @Parameter(value = 1)
    public WithTranslation withTranslation;

    private Composition comp;

    private IconUpdateChecker iconUpdates;

    @Parameters(name = "{index}: mask = {0}, translation = {1}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {WithMask.NO, WithTranslation.NO},
                {WithMask.YES, WithTranslation.NO},
                {WithMask.NO, WithTranslation.YES},
                {WithMask.YES, WithTranslation.YES},
        });
    }

    @Before
    public void setUp() {
        comp = TestHelper.createMockComposition();

        layer = TestHelper.createImageLayer("layer 1", comp);

        LayerGUI ui = mock(LayerGUI.class);
        layer.setUI(ui);

        withMask.init(layer);
        LayerMask mask = null;
        if (withMask.isYes()) {
            mask = layer.getMask();
        }

        withTranslation.init(layer);

        int layerIconUpdatesAtStart = 0;
        if (withTranslation.isYes()) {
            layerIconUpdatesAtStart = 1;
        }

        iconUpdates = new IconUpdateChecker(ui, layer, mask, layerIconUpdatesAtStart, 1);
    }

    @Test
    public void test_getSetImage() {
        // setImage is called already in the ImageLayer constructor
        int expectedImageChangedCalls = 1;
        if (withMask.isYes()) {
            // plus the mask constructor
            expectedImageChangedCalls++;
        }
        if (withTranslation.isYes()) {
            expectedImageChangedCalls++;
        }
        verify(comp, times(expectedImageChangedCalls)).imageChanged(INVALIDATE_CACHE);

        BufferedImage image = layer.getImage();
        assertThat(image).isNotNull();

        BufferedImage testImage = TestHelper.createImage();
        layer.setImage(testImage);

        // called one more time
        verify(comp, times(expectedImageChangedCalls + 1)).imageChanged(INVALIDATE_CACHE);

        // actually setImage should not update the layer image
        iconUpdates.check(0, 0);

        assertThat(layer.getImage()).isSameAs(testImage);
    }

    @Test
    public void test_startPreviewing_WOSelection() {
        BufferedImage image = layer.getImage();
        layer.startPreviewing();
        assertThat(layer.getState()).isEqualTo(PREVIEW);
        assertThat(layer.getPreviewImage()).isSameAs(image);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_startPreviewing_WithSelection() {
        BufferedImage image = layer.getImage();
        TestHelper.addSelectionRectTo(comp, 2, 2, 2, 2);
        layer.startPreviewing();
        assertThat(layer.getState()).isEqualTo(PREVIEW);
        assertThat(layer.getPreviewImage()).isNotSameAs(image);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_okPressedInDialog() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode
        layer.okPressedInDialog("filterName");
        assertThat(layer.getState()).isEqualTo(NORMAL);
        assertThat(layer.getPreviewImage()).isNull();
        iconUpdates.check(0, 0);
    }

    @Test(expected = AssertionError.class)
    public void test_cancelPressedInDialog_Fail() {
        layer.cancelPressedInDialog();
    }

    @Test
    public void test_cancelPressedInDialog_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.cancelPressedInDialog();
        assertThat(layer.getState()).isEqualTo(NORMAL);
        assertThat(layer.getPreviewImage()).isNull();
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_tweenCalculatingStarted() {
        assertThat(layer.getPreviewImage()).isNull();
        layer.tweenCalculatingStarted();
        assertThat(layer.getState()).isEqualTo(PREVIEW);
        assertThat(layer.getPreviewImage()).isNotNull();
        iconUpdates.check(0, 0);
    }

    @Test(expected = AssertionError.class)
    public void test_tweenCalculatingEnded_Fail() {
        // fails because the the tween calculation was not started
        layer.tweenCalculatingEnded();
    }

    @Test
    public void test_tweenCalculatingEnded_OK() {
        layer.tweenCalculatingStarted(); // make sure that the layer is in PREVIEW mode

        layer.tweenCalculatingEnded();
        assertThat(layer.getState()).isEqualTo(NORMAL);
        assertThat(layer.getPreviewImage()).isNull();
        iconUpdates.check(0, 0);
    }

    @Test(expected = IllegalStateException.class)
    public void test_changePreviewImage_Fail() {
        layer.changePreviewImage(TestHelper.createImage(), "filterName", OP_PREVIEW);
    }

    @Test
    public void test_changePreviewImage_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.changePreviewImage(TestHelper.createImage(), "filterName", OP_PREVIEW);
        assertThat(layer.getState()).isEqualTo(PREVIEW);
        assertThat(layer.getPreviewImage()).isNotNull();
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_filterWithoutDialogFinished() {
        assert ConsistencyChecks.imageCoversCanvasCheck(layer);

        BufferedImage dest = ImageUtils.copyImage(layer.getImage());
        layer.filterWithoutDialogFinished(dest,
                OP_WITHOUT_DIALOG, "opName");
        assertThat(layer.getState()).isEqualTo(NORMAL);

        iconUpdates.check(1, 0);
    }

    @Test
    public void test_changeImageUndoRedo() {
        TestHelper.addSelectionRectTo(comp, 2, 2, 2, 2);
        layer.changeImageUndoRedo(TestHelper.createImage(),
                IgnoreSelection.NO);
        layer.changeImageUndoRedo(TestHelper.createImage(),
                IgnoreSelection.YES);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_getImageBounds() {
        Rectangle bounds = layer.getImageBounds();
        assertThat(bounds).isNotNull();

        if (withTranslation == WithTranslation.NO) {
            assertThat(bounds).isEqualTo(layer.canvas.getBounds());
        } else {
            assertThat(bounds).isNotEqualTo(layer.canvas.getBounds());
        }
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_getImageForFilterDialogs_WOSelection() {
        BufferedImage image = layer.getImageForFilterDialogs();

        assertThat(image).isNotNull();
        // no selection, we expect it to return the image
        assertThat(image).isSameAs(layer.getImage());
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_getImageForFilterDialogs_WithSelection() {
        TestHelper.addSelectionRectTo(comp, 2, 2, 2, 2);

        BufferedImage image = layer.getImageForFilterDialogs();

        assertThat(image).isNotNull();
        assertThat(image).isNotSameAs(layer.getImage());
        assertThat(image.getWidth()).isEqualTo(2);
        assertThat(image.getHeight()).isEqualTo(2);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_TmpDrawingLayer() {
        TmpDrawingLayer tmpDrawingLayer
                = layer.createTmpDrawingLayer(AlphaComposite.SrcOver);
        assertThat(tmpDrawingLayer).isNotNull();
        assertThat(tmpDrawingLayer.getWidth()).isEqualTo(layer.canvas.getWidth());
        assertThat(tmpDrawingLayer.getHeight()).isEqualTo(layer.canvas.getHeight());

        layer.mergeTmpDrawingLayerDown();
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_createCompositionSizedTmpImage() {
        BufferedImage image = layer.createCompositionSizedTmpImage();
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(layer.canvas.getWidth());
        assertThat(image.getHeight()).isEqualTo(layer.canvas.getHeight());
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_getCanvasSizedSubImage() {
        BufferedImage image = layer.getCanvasSizedSubImage();
        assertThat(image).isNotNull();
        Canvas canvas = layer.getComp().getCanvas();
        assert image.getWidth() == canvas.getWidth();
        assert image.getHeight() == canvas.getHeight();
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_getFilterSourceImage() {
        BufferedImage image = layer.getFilterSourceImage();
        assertThat(image).isNotNull();
        iconUpdates.check(0, 0);
        // TODO
    }

    @Test
    public void test_getImageOrSubImageIfSelected() {
        BufferedImage imageTT = layer.getImageOrSubImageIfSelected(true, true);
        assertThat(imageTT).isNotNull();

        BufferedImage imageTF = layer.getImageOrSubImageIfSelected(true, false);
        assertThat(imageTF).isNotNull();

        BufferedImage imageFT = layer.getImageOrSubImageIfSelected(false, true);
        assertThat(imageFT).isNotNull();

        BufferedImage imageFF = layer.getImageOrSubImageIfSelected(false, false);
        assertThat(imageFF).isNotNull();

        iconUpdates.check(0, 0);
        // TODO
    }

    @Test
    public void test_getSelectionSizedPartFrom() {
        Selection selection = mock(Selection.class);
        when(selection.getShapeBounds()).thenReturn(new Rectangle(2, 2, 10, 10));

        BufferedImage imageT = layer.getSelectionSizedPartFrom(TestHelper.createImage(), selection, true);
        assertThat(imageT).isNotNull();

        BufferedImage imageF = layer.getSelectionSizedPartFrom(TestHelper.createImage(), selection, false);
        assertThat(imageF).isNotNull();

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_cropToCanvasSize() {
        layer.cropToCanvasSize();

        Canvas canvas = layer.getComp().getCanvas();
        BufferedImage image = layer.getImage();
        assert image.getWidth() == canvas.getWidth();
        assert image.getHeight() == canvas.getHeight();
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_enlargeCanvas() {
        layer.enlargeCanvas(5, 5, 5, 10);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_createMovementEdit() {
        ContentLayerMoveEdit edit = layer.createMovementEdit(5, 5);
        assertThat(edit).isNotNull();
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_duplicate() {
        ImageLayer duplicate = layer.duplicate(false);
        assertThat(duplicate).isNotNull();

        BufferedImage image = layer.getImage();
        BufferedImage duplicateImage = duplicate.getImage();

        assertNotSame(duplicateImage, image);
        assertThat(image.getWidth()).isEqualTo(duplicateImage.getWidth());
        assertThat(image.getHeight()).isEqualTo(duplicateImage.getHeight());

        assertThat(duplicate.getImageBounds()).isEqualTo(layer.getImageBounds());
        assertSame(layer.getBlendingMode(), duplicate.getBlendingMode());
        assertThat(duplicate.getOpacity()).isEqualTo(layer.getOpacity());

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_applyLayerMask() {
        if (withMask.isYes()) {
            History.clear();

            assertThat(layer.hasMask()).isTrue();

            layer.applyLayerMask(AddToHistory.YES);
            assertThat(layer.hasMask()).isFalse();
            iconUpdates.check(1, 0);

            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Apply Layer Mask");

            History.undo();
            assertThat(layer.hasMask()).isTrue();
            iconUpdates.check(2, 1);

            History.redo();
            assertThat(layer.hasMask()).isFalse();
            iconUpdates.check(3, 1);
        }
    }
}