/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Build;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.TestHelper;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;
import pixelitor.testutils.WithSelection;
import pixelitor.testutils.WithTranslation;
import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.ChangeReason.FILTER_WITHOUT_DIALOG;
import static pixelitor.ChangeReason.PREVIEWING;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.layers.ImageLayer.State.NORMAL;
import static pixelitor.layers.ImageLayer.State.PREVIEW;

@RunWith(Parameterized.class)
public class ImageLayerTest {
    private ImageLayer layer;

    @Parameter
    public WithMask withMask;

    @Parameter(value = 1)
    public WithTranslation withTranslation;

    @Parameter(value = 2)
    public WithSelection withSelection;

    private Composition comp;

    private IconUpdateChecker iconUpdates;

    @Parameters(name = "{index}: mask = {0}, transl = {1}, sel = {2}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {WithMask.NO, WithTranslation.NO, WithSelection.NO},
                {WithMask.YES, WithTranslation.NO, WithSelection.NO},
                {WithMask.NO, WithTranslation.YES, WithSelection.NO},
                {WithMask.YES, WithTranslation.YES, WithSelection.NO},
                {WithMask.NO, WithTranslation.NO, WithSelection.YES},
        });
    }

    @BeforeClass
    public static void beforeAllTests() {
        Build.setUnitTestingMode();
    }

    @Before
    public void beforeEachTest() {
        comp = TestHelper.createMockComp();

        layer = ImageLayer.createEmpty(comp, "layer 1");

        withMask.setupFor(layer);
        LayerMask mask = null;
        if (withMask.isTrue()) {
            mask = layer.getMask();
        }

        withTranslation.setupFor(layer);
        withSelection.setupFor(comp);

        int layerIconUpdatesAtStart = 0;
        if (withTranslation.isTrue()) {
            layerIconUpdatesAtStart = 1;
        }

        iconUpdates = new IconUpdateChecker(layer, mask, layerIconUpdatesAtStart, 1);
    }

    @Test
    public void getSetImage() {
        // setImage is called already in the ImageLayer constructor
        int expectedImageChangedCalls = 1;
        if (withMask.isTrue()) {
            // plus the mask constructor
            expectedImageChangedCalls++;
        }
        if (withTranslation.isTrue()) {
            expectedImageChangedCalls++;
        }
        verify(comp, times(expectedImageChangedCalls)).imageChanged(INVALIDATE_CACHE);

        BufferedImage image = layer.getImage();
        assertThat(image).isNotNull();

        BufferedImage testImage = TestHelper.createImage();
        layer.setImage(testImage);

        // called one more time
        verify(comp, times(expectedImageChangedCalls + 1)).imageChanged(INVALIDATE_CACHE);

        // actually setImage should not update the icon image
        iconUpdates.check(0, 0);

        assertThat(layer).imageIs(testImage);
    }

    @Test
    public void startPreviewing() {
        BufferedImage imageBefore = layer.getImage();

        layer.startPreviewing();

        assertThat(layer).stateIs(PREVIEW);

        if (withSelection.isTrue()) {
            assertThat(layer).previewImageIsNot(imageBefore);
        } else {
            assertThat(layer).previewImageIs(imageBefore);
        }

        iconUpdates.check(0, 0);
    }

    @Test
    public void onDialogAccepted() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.onFilterDialogAccepted("filterName");

        assertThat(layer)
                .stateIs(NORMAL)
                .previewImageIs(null);
        iconUpdates.check(0, 0);
    }

    @Test
    public void onDialogCanceled_Fail() {
        assertThrows(AssertionError.class, () ->
                layer.onFilterDialogCanceled());
    }

    @Test
    public void onDialogCanceled_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.onFilterDialogCanceled();

        assertThat(layer)
                .stateIs(NORMAL)
                .previewImageIs(null);
        iconUpdates.check(0, 0);
    }

    @Test
    public void tweenCalculatingStarted() {
        assertThat(layer)
                .stateIs(NORMAL)
                .previewImageIs(null);

        layer.tweenCalculatingStarted();

        assertThat(layer)
                .stateIs(PREVIEW)
                .previewImageIsNot(null);
        iconUpdates.check(0, 0);
    }

    @Test
    public void tweenCalculatingEnded_Fail() {
        // fails because the the tween calculation was not started
        assertThrows(AssertionError.class, () ->
                layer.tweenCalculatingEnded());
    }

    @Test
    public void tweenCalculatingEnded_OK() {
        layer.tweenCalculatingStarted(); // make sure that the layer is in PREVIEW mode

        layer.tweenCalculatingEnded();

        assertThat(layer)
                .stateIs(NORMAL)
                .previewImageIs(null);
        iconUpdates.check(0, 0);
    }

    @Test
    public void changePreviewImage_Fail() {
        assertThrows(IllegalStateException.class, () ->
                layer.changePreviewImage(TestHelper.createImage(), "filterName", PREVIEWING));
    }

    @Test
    public void changePreviewImage_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.changePreviewImage(TestHelper.createImage(), "filterName", PREVIEWING);

        assertThat(layer)
                .stateIs(PREVIEW)
                .previewImageIsNot(null);
        iconUpdates.check(0, 0);
    }

    @Test
    public void filterWithoutDialogFinished() {
        assert ConsistencyChecks.imageCoversCanvas(layer);
        BufferedImage dest = ImageUtils.copyImage(layer.getImage());

        layer.filterWithoutDialogFinished(dest,
                FILTER_WITHOUT_DIALOG, "opName");

        assertThat(layer).stateIs(NORMAL);
        iconUpdates.check(1, 0);
    }

    @Test
    public void changeImageForUndoRedo() {
        TestHelper.setSelection(comp, new Rectangle(2, 2, 2, 2));

        layer.changeImageForUndoRedo(TestHelper.createImage(),
                false);
        layer.changeImageForUndoRedo(TestHelper.createImage(),
                true);

        iconUpdates.check(0, 0);
    }

    @Test
    public void getImageBounds() {
        Rectangle bounds = layer.getImageBounds();

        assertThat(bounds).isNotNull();
        Canvas canvas = layer.getComp().getCanvas();
        if (withTranslation == WithTranslation.NO) {
            assertThat(bounds).isEqualTo(canvas.getImBounds());
        } else {
            assertThat(bounds).isNotEqualTo(canvas.getImBounds());
        }
        iconUpdates.check(0, 0);
    }

    @Test
    public void getImageForFilterDialogs() {
        BufferedImage image = layer.getImageForFilterDialogs();

        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(image)
                    .isNotNull()
                    .isNotSameAs(layer.getImage())
                    .widthIs(selShape.width)
                    .heightIs(selShape.height);
        } else {
            // no selection, we expect it to return the image
            assertThat(image)
                    .isNotNull()
                    .isSameAs(layer.getImage());
        }

        iconUpdates.check(0, 0);
    }

    @Test
    public void tmpDrawingLayer() {
        TmpDrawingLayer tmpDrawingLayer
                = layer.createTmpDrawingLayer(AlphaComposite.SrcOver, false);
        assertThat(tmpDrawingLayer).isNotNull();
        Canvas canvas = layer.getComp().getCanvas();
        assertThat(tmpDrawingLayer.getWidth()).isEqualTo(canvas.getImWidth());
        assertThat(tmpDrawingLayer.getHeight()).isEqualTo(canvas.getImHeight());

        layer.mergeTmpDrawingLayerDown();
        iconUpdates.check(0, 0);
    }

    @Test
    public void createCanvasSizedTmpImage() {
        Canvas canvas = layer.getComp().getCanvas();
        BufferedImage image = canvas.createTmpImage();

        assertThat(image)
                .isNotNull()
                .widthIs(canvas.getImWidth())
                .heightIs(canvas.getImHeight());
        iconUpdates.check(0, 0);
    }

    @Test
    public void getCanvasSizedSubImage() {
        BufferedImage image = layer.getCanvasSizedSubImage();

        Canvas canvas = layer.getComp().getCanvas();
        assertThat(image)
                .isNotNull()
                .widthIs(canvas.getImWidth())
                .heightIs(canvas.getImHeight());
        iconUpdates.check(0, 0);
    }

    @Test
    public void getFilterSourceImage() {
        var filterSourceImage = layer.getFilterSourceImage();

        assertThat(filterSourceImage).isNotNull();
        iconUpdates.check(0, 0);
        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(filterSourceImage)
                    .isNotSameAs(layer.getImage())
                    .widthIs(selShape.width)
                    .heightIs(selShape.height);
        } else {
            assertThat(filterSourceImage).isSameAs(layer.getImage());
        }
    }

    @Test
    public void getSelectedSubImage() {
        BufferedImage imageT = layer.getSelectedSubImage(true);
        assertThat(imageT).isNotNull();
        BufferedImage layerImage = layer.getImage();
        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(imageT)
                    .isNotSameAs(layerImage)
                    .widthIs(selShape.width)
                    .heightIs(selShape.height);
        } else {
            assertThat(imageT)
                    .isNotSameAs(layerImage) // copy even if there is no selection
                    .widthIs(layerImage.getWidth())
                    .heightIs(layerImage.getHeight());
        }

        BufferedImage imageF = layer.getSelectedSubImage(false);
        assertThat(imageF).isNotNull();

        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(imageF)
                    .isNotSameAs(layerImage)
                    .widthIs(selShape.width)
                    .heightIs(selShape.height);
        } else {
            assertThat(imageF)
                    .isSameAs(layerImage) // don't copy if there is no selection
                    .widthIs(layerImage.getWidth())
                    .heightIs(layerImage.getHeight());
        }

        iconUpdates.check(0, 0);
    }

    @Test
    public void crop_deletePixels_noGrowing() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).imageBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(3, 3, 5, 5);

        // when
        layer.crop(cropRect, true, false);

        // then
        // intersect because growing is disabled
        Rectangle expectedNewCanvas = cropRect.intersection(origBounds);
        checkLayerAfterCrop(0, 0,
                5, 5, expectedNewCanvas);
    }

    @Test
    public void crop_deletePixels_withGrowing() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).imageBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(3, 3, 50, 50);

        // when
        layer.crop(cropRect, true, true);

        // then
        checkLayerAfterCrop(0, 0,
                50, 50, cropRect);
    }

    @Test
    public void crop_keepPixels_noGrowing() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origImBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).imageBoundsIsEqualTo(origImBounds);
        Rectangle cropRect = new Rectangle(3, 4, 50, 50);

        // when
        layer.crop(cropRect, false, false);

        // then
        // intersect because growing is disabled
        Rectangle expectedNewCanvas = cropRect.intersection(origImBounds);
        checkLayerAfterCrop(tx - 3, ty - 4,
                20 - tx, 10 - ty, expectedNewCanvas);
    }

    @Test
    public void crop_keepPixels_withGrowingRightDown() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).imageBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(3, 4, 50, 50);

        // when
        layer.crop(cropRect, false, true);

        // then
        checkLayerAfterCrop(tx - 3, ty - 4,
                53 - tx, 54 - ty, cropRect);
    }

    @Test
    public void crop_keepPixels_withGrowingLeft() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).imageBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(-10, 3, 20, 3);

        // when
        layer.crop(cropRect, false, true);

        // then
        checkLayerAfterCrop(0, -3 + ty,
                30, 10 - ty, cropRect);
    }

    @Test
    public void crop_keepPixels_withGrowingDown() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - ty);
        assertThat(layer).imageBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(8, 5, 4, 10);

        // when
        layer.crop(cropRect, false, true);

        // then
        checkLayerAfterCrop(-8 + tx, -5 + ty,
                20 - tx, 15 - ty, cropRect);
    }

    private void checkLayerAfterCrop(int expectedTx, int expectedTy,
                                     int expectedImWidth, int expectedImHeight,
                                     Rectangle expectedNewCanvas) {
        assertThat(layer).imageBoundsIsEqualTo(
                new Rectangle(expectedTx, expectedTy,
                        expectedImWidth, expectedImHeight));
        iconUpdates.check(0, 0);

        comp.getCanvas().changeImSize(
                (int) expectedNewCanvas.getWidth(),
                (int) expectedNewCanvas.getHeight(), comp.getView());
        ConsistencyChecks.imageCoversCanvas(layer);
    }

    @Test
    public void cropToCanvasSize() {
        layer.toCanvasSize();

        Canvas canvas = layer.getComp().getCanvas();
        BufferedImage image = layer.getImage();
        assertThat(image)
                .widthIs(canvas.getImWidth())
                .heightIs(canvas.getImHeight());
        iconUpdates.check(0, 0);
    }

    @Test
    public void enlargeCanvas() {
        layer.enlargeCanvas(5, 5, 5, 10);

        iconUpdates.check(0, 0);
    }

    @Test
    public void createMovementEdit() {
        ContentLayerMoveEdit edit = layer.createMovementEdit(5, 5);

        assertThat(edit).isNotNull();
        iconUpdates.check(0, 0);
    }

    @Test
    public void duplicate() {
        ImageLayer duplicate = layer.duplicate(false);

        assertThat(duplicate)
                .blendingModeIs(layer.getBlendingMode())
                .opacityIs(layer.getOpacity())
                .imageBoundsIsEqualTo(layer.getImageBounds());

        BufferedImage image = layer.getImage();
        BufferedImage duplicateImage = duplicate.getImage();
        assertNotSame(duplicateImage, image);
        assertThat(image).widthIs(duplicateImage.getWidth());
        assertThat(image).heightIs(duplicateImage.getHeight());

        iconUpdates.check(0, 0);
    }

    @Test
    public void applyLayerMask() {
        if (withMask.isTrue()) {
            History.clear();
            assertThat(layer).hasMask();

            layer.applyLayerMask(true);

            assertThat(layer).hasNoMask();
            iconUpdates.check(1, 0);
            History.assertNumEditsIs(1);

            History.undo("Apply Layer Mask");
            assertThat(layer).hasMask();
            iconUpdates.check(2, 1);

            History.redo("Apply Layer Mask");
            assertThat(layer).hasNoMask();
            iconUpdates.check(3, 1);
        }
    }
}