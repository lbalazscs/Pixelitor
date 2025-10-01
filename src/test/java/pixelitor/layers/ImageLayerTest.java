/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.*;
import pixelitor.compactions.Outsets;
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.FilterContext.FILTER_WITHOUT_DIALOG;
import static pixelitor.FilterContext.PREVIEWING;
import static pixelitor.TestHelper.createEmptyImageLayer;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.assertions.PixelitorAssertions.assertThatThrownBy;
import static pixelitor.layers.ImageLayer.State.NORMAL;
import static pixelitor.layers.ImageLayer.State.PREVIEW;

@ParameterizedClass(name = "mask = {0}, translation = {1}, sel = {2}")
@MethodSource("instancesToTest")
@DisplayName("image layer tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ImageLayerTest {
    private ImageLayer layer;

    @Parameter(0)
    private WithMask withMask;

    @Parameter(1)
    private WithTranslation withTranslation;

    @Parameter(2)
    private WithSelection withSelection;

    private Composition comp;

    private IconUpdateChecker iconChecker;

    static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
            {WithMask.NO, WithTranslation.NO, WithSelection.NO},
            {WithMask.YES, WithTranslation.NO, WithSelection.NO},
            {WithMask.NO, WithTranslation.YES, WithSelection.NO},
            {WithMask.YES, WithTranslation.YES, WithSelection.NO},
            {WithMask.NO, WithTranslation.NO, WithSelection.YES},
        });
    }

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        comp = TestHelper.createMockComp("ImageLayerTest");

        layer = createEmptyImageLayer(comp, "layer 1");

        withMask.configure(layer);
        withTranslation.configure(layer);
        withSelection.configure(comp);

        iconChecker = new IconUpdateChecker(layer);
    }

    @Test
    void getSetImage() {
        // setImage is called already in the ImageLayer constructor
        int expectedCacheInvalidations = 1;
        if (withMask.isTrue()) {
            // plus the mask constructor
            expectedCacheInvalidations++;
        }
        if (withTranslation.isTrue()) {
            expectedCacheInvalidations++;
        }
        if (withMask.isTrue() && withTranslation.isTrue()) {
            expectedCacheInvalidations++;
        }
        verify(comp, times(expectedCacheInvalidations)).invalidateImageCache();

        BufferedImage image = layer.getImage();
        assertThat(image).isNotNull();

        BufferedImage testImage = TestHelper.createImage();
        layer.setImage(testImage);

        // called one more time
        verify(comp, times(expectedCacheInvalidations + 1)).invalidateImageCache();

        // actually setImage should not update the icon image
        iconChecker.verifyUpdateCounts(0, 0);

        assertThat(layer).imageIs(testImage);
    }

    @Test
    void startPreviewing() {
        BufferedImage imageBefore = layer.getImage();

        layer.startPreviewing();

        assertThat(layer).stateIs(PREVIEW);

        if (withSelection.isTrue()) {
            assertThat(layer).previewImageIsNot(imageBefore);
        } else {
            assertThat(layer).previewImageIs(imageBefore);
        }

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void onDialogAccepted() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.onFilterDialogAccepted("filterName");

        assertThat(layer)
            .stateIs(NORMAL)
            .previewImageIs(null);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void onDialogCanceled_Fail() {
        assertThatThrownBy(() -> layer.onFilterDialogCanceled())
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void onDialogCanceled_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.onFilterDialogCanceled();

        assertThat(layer)
            .stateIs(NORMAL)
            .previewImageIs(null);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void changePreviewImage_Fail() {
        assertThatThrownBy(() -> layer.changePreviewImage(TestHelper.createImage(), "filterName", PREVIEWING))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changePreviewImage_OK() {
        layer.startPreviewing(); // make sure that the layer is in PREVIEW mode

        layer.changePreviewImage(TestHelper.createImage(), "filterName", PREVIEWING);

        assertThat(layer)
            .stateIs(PREVIEW)
            .previewImageIsNot(null);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void filterWithoutDialogFinished() {
        assert ConsistencyChecks.imageCoversCanvas(layer);
        BufferedImage dest = ImageUtils.copyImage(layer.getImage());

        layer.filterWithoutDialogFinished(dest,
            FILTER_WITHOUT_DIALOG, "opName");

        assertThat(layer).stateIs(NORMAL);
        iconChecker.verifyUpdateCounts(1, 0);
    }

    @Test
    void changeImageForUndoRedo() {
        TestHelper.setSelection(comp, new Rectangle(2, 2, 2, 2));

        layer.changeImageForUndoRedo(TestHelper.createImage(),
            false);
        layer.changeImageForUndoRedo(TestHelper.createImage(),
            true);

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void getContentBounds() {
        Rectangle bounds = layer.getContentBounds();

        assertThat(bounds).isNotNull();
        Canvas canvas = layer.getComp().getCanvas();
        if (withTranslation == WithTranslation.NO) {
            assertThat(bounds).isEqualTo(canvas.getBounds());
        } else {
            assertThat(bounds).isNotEqualTo(canvas.getBounds());
        }
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void getImageForFilterDialogs() {
        BufferedImage image = layer.getImageForFilterDialogs();

        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(image)
                .isNotNull()
                .isNotSameAs(layer.getImage())
                .hasSameSizeAs(selShape);
        } else {
            // no selection, we expect it to return the image
            assertThat(image)
                .isNotNull()
                .isSameAs(layer.getImage());
        }

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void tmpDrawingLayer() {
        TmpLayer tmpLayer
            = layer.createTmpLayer(AlphaComposite.SrcOver, false);
        assertThat(tmpLayer).isNotNull();
        Canvas canvas = layer.getComp().getCanvas();
        assertThat(tmpLayer.getWidth()).isEqualTo(canvas.getWidth());
        assertThat(tmpLayer.getHeight()).isEqualTo(canvas.getHeight());

        layer.mergeTmpDrawingLayerDown();
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void createCanvasSizedTmpImage() {
        Canvas canvas = layer.getComp().getCanvas();
        BufferedImage image = canvas.createTmpImage();

        assertThat(image)
            .isNotNull()
            .hasSameSizeAs(canvas);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void getCanvasSizedSubImage() {
        BufferedImage image = layer.getCanvasSizedSubImage();

        Canvas canvas = layer.getComp().getCanvas();
        assertThat(image)
            .isNotNull()
            .hasSameSizeAs(canvas);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void getFilterSourceImage() {
        var filterSourceImage = layer.getFilterSourceImage();

        assertThat(filterSourceImage).isNotNull();
        iconChecker.verifyUpdateCounts(0, 0);
        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(filterSourceImage)
                .isNotSameAs(layer.getImage())
                .hasSameSizeAs(selShape);
        } else {
            assertThat(filterSourceImage).isSameAs(layer.getImage());
        }
    }

    @Test
    void getSelectedSubImage() {
        BufferedImage imageT = layer.getSelectedSubImage(true);
        assertThat(imageT).isNotNull();
        BufferedImage layerImage = layer.getImage();
        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(imageT)
                .isNotSameAs(layerImage)
                .hasSameSizeAs(selShape);
        } else {
            assertThat(imageT)
                .isNotSameAs(layerImage) // copy even if there is no selection
                .hasSameSizeAs(layerImage);
        }

        BufferedImage imageF = layer.getSelectedSubImage(false);
        assertThat(imageF).isNotNull();

        if (withSelection.isTrue()) {
            Rectangle selShape = WithSelection.SELECTION_SHAPE;
            assertThat(imageF)
                .isNotSameAs(layerImage)
                .hasSameSizeAs(selShape);
        } else {
            assertThat(imageF)
                .isSameAs(layerImage) // don't copy if there is no selection
                .hasSameSizeAs(layerImage);
        }

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void crop_deletePixels_noGrowing() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).contentBoundsIsEqualTo(origBounds);
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
    void crop_deletePixels_withGrowing() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).contentBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(3, 3, 50, 50);

        // when
        layer.crop(cropRect, true, true);

        // then
        checkLayerAfterCrop(0, 0,
            50, 50, cropRect);
    }

    @Test
    void crop_keepPixels_noGrowing() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origImBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).contentBoundsIsEqualTo(origImBounds);
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
    void crop_keepPixels_withGrowingRightDown() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).contentBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(3, 4, 50, 50);

        // when
        layer.crop(cropRect, false, true);

        // then
        checkLayerAfterCrop(tx - 3, ty - 4,
            53 - tx, 54 - ty, cropRect);
    }

    @Test
    void crop_keepPixels_withGrowingLeft() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - tx);
        assertThat(layer).contentBoundsIsEqualTo(origBounds);
        Rectangle cropRect = new Rectangle(-10, 3, 20, 3);

        // when
        layer.crop(cropRect, false, true);

        // then
        checkLayerAfterCrop(0, -3 + ty,
            30, 10 - ty, cropRect);
    }

    @Test
    void crop_keepPixels_withGrowingDown() {
        // given
        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle origBounds = new Rectangle(tx, ty, 20 - tx, 10 - ty);
        assertThat(layer).contentBoundsIsEqualTo(origBounds);
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
        assertThat(layer).contentBoundsIsEqualTo(new Rectangle(
            expectedTx, expectedTy, expectedImWidth, expectedImHeight));
        iconChecker.verifyUpdateCounts(0, 0);

        comp.getCanvas().resize(
            (int) expectedNewCanvas.getWidth(),
            (int) expectedNewCanvas.getHeight(), comp.getView(), true);
        ConsistencyChecks.imageCoversCanvas(layer);
    }

    @Test
    void cropToCanvasSize() {
        layer.toCanvasSize();

        Canvas canvas = layer.getComp().getCanvas();
        BufferedImage image = layer.getImage();
        assertThat(image)
            .hasSameSizeAs(canvas);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void enlargeCanvas() {
        layer.enlargeCanvas(new Outsets(5, 5, 5, 10));

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void createMovementEdit() {
        ContentLayerMoveEdit edit = layer.createMovementEdit(5, 5);

        assertThat(edit).isNotNull();
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void duplicate() {
        ImageLayer duplicate = (ImageLayer) layer.copy(CopyType.DUPLICATE_LAYER, true, comp);

        assertThat(duplicate)
            .contentBoundsIsEqualTo(layer.getContentBounds())
            .blendingModeIs(layer.getBlendingMode())
            .opacityIs(layer.getOpacity());

        BufferedImage image = layer.getImage();
        BufferedImage duplicateImage = duplicate.getImage();
        assertThat(duplicateImage).isNotSameAs(image);
        assertThat(image)
            .isNotSameAs(duplicateImage)
            .hasSameSizeAs(duplicateImage);

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void applyLayerMask() {
        if (withMask.isTrue()) {
            History.clear();
            assertThat(layer).hasMask();

            layer.applyLayerMask(true);

            assertThat(layer).hasNoMask();
            iconChecker.verifyUpdateCounts(1, 0);
            History.assertNumEditsIs(1);

            History.undo("Apply Layer Mask");
            assertThat(layer).hasMask();
            iconChecker.verifyUpdateCounts(2, 0);

            History.redo("Apply Layer Mask");
            assertThat(layer).hasNoMask();
            iconChecker.verifyUpdateCounts(3, 0);
        }
    }
}
