/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.compactions;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.testutils.NumLayers;
import pixelitor.testutils.WithMask;
import pixelitor.testutils.WithSelection;
import pixelitor.testutils.WithTranslation;
import pixelitor.tools.Tools;
import pixelitor.utils.QuadrantAngle;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.compactions.Flip.Direction.HORIZONTAL;
import static pixelitor.compactions.Flip.Direction.VERTICAL;
import static pixelitor.utils.QuadrantAngle.*;

@RunWith(Parameterized.class)
public class CompActionTest {
    private static final int ORIG_CANVAS_WIDTH = 20;
    private static final int ORIG_CANVAS_HEIGHT = 10;

    private Composition origComp;
    private View view;

    private Rectangle origSelection;
    private final int origTX;
    private final int origTY;
    private final int origImageWidth;
    private final int origImageHeight;

    // the parameters
    private final WithTranslation withTranslation;
    private final WithSelection withSelection;
    private final WithMask withMask;
    private final NumLayers numLayers;

    public CompActionTest(WithTranslation withTranslation,
                          WithSelection withSelection,
                          WithMask withMask,
                          NumLayers numLayers) {
        this.withSelection = withSelection;
        this.withTranslation = withTranslation;
        this.withMask = withMask;
        this.numLayers = numLayers;

        origTX = withTranslation.getExpectedTX();
        origTY = withTranslation.getExpectedTY();

        origImageWidth = ORIG_CANVAS_WIDTH - origTX;
        origImageHeight = ORIG_CANVAS_HEIGHT - origTY;
    }

    @Parameters(name = "{index}: translation = {0}, selection = {1}, mask = {2}, layers = {3}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
            {WithTranslation.NO, WithSelection.NO, WithMask.NO, NumLayers.ONE},
            {WithTranslation.YES, WithSelection.NO, WithMask.NO, NumLayers.ONE},
            {WithTranslation.YES, WithSelection.YES, WithMask.NO, NumLayers.ONE},
            {WithTranslation.YES, WithSelection.YES, WithMask.YES, NumLayers.ONE},
            {WithTranslation.YES, WithSelection.YES, WithMask.YES, NumLayers.TWO},
        });
    }

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();

        Tools.setCurrentTool(Tools.CROP);
    }

    @Before
    public void beforeEachTest() {
        origComp = TestHelper.createComp(2, true);
        assertThat(origComp)
            .isNotEmpty()
            .hasName("Test")
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .activeLayerNameIs("layer 2")
            .doesNotHaveSelection()
            .firstLayerHasMask()
            .secondLayerHasMask();

        withTranslation.setupFor(origComp);
        withSelection.setupFor(origComp);
        withMask.setupFor(origComp);
        numLayers.setupFor(origComp);

        if (withSelection.isTrue()) {
            origSelection = WithSelection.SELECTION_SHAPE;
        }
        History.clear();
        view = origComp.getView();
    }

    private void checkOriginalState() {
        assert view.getComp() == origComp;

        assertThat(origComp)
            .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT)
            .activeLayerAndMaskImageSizeIs(origImageWidth, origImageHeight)
            .activeLayerTranslationIs(origTX, origTY)
            .allLayerUIsAreOK()
            .invariantIsOK();

        if (withSelection.isTrue()) {
            assertThat(origComp).selectionBoundsIs(origSelection);
        }

        checkTranslationOfNonActiveLayer();
    }

    @Test
    public void enlargeCanvas() {
        checkOriginalState();

        int north = 3;
        int east = 4;
        int south = 5;
        int west = 2;
        EnlargeCanvas action = new EnlargeCanvas(north, east, south, west);
        Composition enlarged = action.process(view.getComp()).join();
        assert enlarged != origComp;
        assert view.getComp() == enlarged;

        checkStateAfterEnlargeCanvas(north, east, south, west);

        History.assertNumEditsIs(1);

        History.undo("Enlarge Canvas");
        checkOriginalState();

        History.redo("Enlarge Canvas");
        checkStateAfterEnlargeCanvas(north, east, south, west);
    }

    private void checkStateAfterEnlargeCanvas(int north, int east, int south, int west) {
        int newCanvasWidth = ORIG_CANVAS_WIDTH + west + east;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT + north + south;

        var newComp = view.getComp();

        assertThat(newComp)
            .invariantIsOK()
            .canvasSizeIs(newCanvasWidth, newCanvasHeight)
            .activeLayerTranslationIs(
                Math.min(0, origTX + west),
                Math.min(0, origTY + north))
            .activeLayerAndMaskImageSizeIs(
                origImageWidth + east + Math.max(0, origTX + west),
                origImageHeight + south + Math.max(0, origTY + north));

        if (withSelection.isTrue()) {
            assertThat(newComp).selectionBoundsIs(new Rectangle(
                origSelection.x + west,
                origSelection.y + north,
                origSelection.width,
                origSelection.height));
        }
    }

    @Test
    public void resize() {
        checkOriginalState();

        int targetWidth = ORIG_CANVAS_WIDTH / 2;
        int targetHeight = ORIG_CANVAS_HEIGHT / 2;
        Composition resized = new Resize(targetWidth, targetHeight, false).process(view.getComp()).join();
        assert resized != origComp;
        assert view.getComp() == resized;

        checkStateAfterResize();

        History.assertNumEditsIs(1);

        History.undo("Resize");
        checkOriginalState();

        History.redo("Resize");
        checkStateAfterResize();
    }

    private void checkStateAfterResize() {
        var newComp = view.getComp();

        if (withSelection.isTrue()) {
            var halfOfOrigSelection = new Rectangle(
                origSelection.x / 2,
                origSelection.y / 2,
                origSelection.width / 2,
                origSelection.height / 2);
            assertThat(newComp).selectionBoundsIs(halfOfOrigSelection);
        }

        int newCanvasWidth = ORIG_CANVAS_WIDTH / 2;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT / 2;

        assertThat(newComp)
            .invariantIsOK()
            .canvasSizeIs(newCanvasWidth, newCanvasHeight)
            .activeLayerAndMaskImageSizeIs(
                newCanvasWidth - origTX / 2,
                newCanvasHeight - origTY / 2)
            .activeLayerTranslationIs(
                Math.min(0, origTX / 2),
                Math.min(0, origTY / 2));
    }

    @Test
    public void rotate90() {
        testRotate(ANGLE_90, "Rotate 90° CW");
    }

    @Test
    public void rotate180() {
        testRotate(ANGLE_180, "Rotate 180°");
    }

    @Test
    public void rotate270() {
        testRotate(ANGLE_270, "Rotate 90° CCW");
    }

    private void testRotate(QuadrantAngle angle, String editName) {
        checkOriginalState();
        Composition rotated = new Rotate(angle).process(view.getComp()).join();
        assert rotated != origComp;
        assert view.getComp() == rotated;

        checkStateAfterRotate(angle);

        History.assertNumEditsIs(1);

        History.undo(editName);
        checkOriginalState();

        History.redo(editName);
        checkStateAfterRotate(angle);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void checkStateAfterRotate(QuadrantAngle angle) {
        var newComp = view.getComp();

        if (angle == ANGLE_180) {
            assertThat(newComp)
                .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT)
                .activeLayerAndMaskImageSizeIs(
                    origImageWidth,
                    origImageHeight);
        } else {
            assertThat(newComp)
                .canvasSizeIs(ORIG_CANVAS_HEIGHT, ORIG_CANVAS_WIDTH)
                .activeLayerAndMaskImageSizeIs(
                    origImageHeight,
                    origImageWidth);
        }

        int canvasDistFromImgBottom = origImageHeight - ORIG_CANVAS_HEIGHT
            + withTranslation.getExpectedTY();
        int canvasDistFromImgRight = origImageWidth - ORIG_CANVAS_WIDTH
            + withTranslation.getExpectedTX();

        switch (angle) {
            case ANGLE_90 -> assertThat(newComp).activeLayerTranslationIs(
                canvasDistFromImgBottom, withTranslation.getExpectedTX());
            case ANGLE_180 -> assertThat(newComp).activeLayerTranslationIs(
                canvasDistFromImgRight, canvasDistFromImgBottom);
            case ANGLE_270 -> assertThat(newComp).activeLayerTranslationIs(
                withTranslation.getExpectedTY(), canvasDistFromImgRight);
        }

        if (withSelection.isTrue()) {
            int distFromBottom = ORIG_CANVAS_HEIGHT - origSelection.height - origSelection.y;
            int distFromRight = ORIG_CANVAS_WIDTH - origSelection.width - origSelection.x;
            int distFromLeft = origSelection.x;
            int distFromTop = origSelection.y;

            Rectangle rotatedSelectionBounds = switch (angle) {
                case ANGLE_90 -> new Rectangle(
                    distFromBottom, distFromLeft,
                    origSelection.height, origSelection.width);
                case ANGLE_180 -> new Rectangle(
                    distFromRight, distFromBottom,
                    origSelection.width, origSelection.height);
                case ANGLE_270 -> new Rectangle(
                    distFromTop, distFromRight,
                    origSelection.height, origSelection.width);
            };

            assertThat(newComp).selectionBoundsIs(rotatedSelectionBounds);
        }

        assertThat(newComp).invariantIsOK();
    }

    @Test
    public void flipHorizontal() {
        testFlip(HORIZONTAL, "Flip Horizontal");
    }

    @Test
    public void flipVertical() {
        testFlip(VERTICAL, "Flip Vertical");
    }

    private void testFlip(Flip.Direction direction, String editName) {
        checkOriginalState();

        Composition flipped = new Flip(direction).process(view.getComp()).join();
        assert flipped != origComp;
        assert view.getComp() == flipped;

        checkStateAfterFlip(direction);

        History.assertNumEditsIs(1);

        History.undo(editName);
        checkOriginalState();

        History.redo(editName);
        checkStateAfterFlip(direction);
    }

    private void checkStateAfterFlip(Flip.Direction direction) {
        var newComp = view.getComp();

        assertThat(newComp)
            .invariantIsOK()
            .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT)
            .activeLayerAndMaskImageSizeIs(origImageWidth, origImageHeight);

        if (direction == HORIZONTAL) {
            assertThat(newComp).activeLayerTranslationIs(
                -(origImageWidth - ORIG_CANVAS_WIDTH + origTX),
                origTY);
        } else if (direction == VERTICAL) {
            assertThat(newComp).activeLayerTranslationIs(
                origTX,
                -(origImageHeight - ORIG_CANVAS_HEIGHT + origTY));
        } else {
            throw new IllegalStateException();
        }

        if (withSelection.isTrue()) {
            int flippedX, flippedY;
            if (direction == HORIZONTAL) {
                flippedX = ORIG_CANVAS_WIDTH - origSelection.x - origSelection.width;
                flippedY = origSelection.y;
            } else if (direction == VERTICAL) {
                flippedX = origSelection.x;
                flippedY = ORIG_CANVAS_HEIGHT - origSelection.y - origSelection.height;
            } else {
                throw new IllegalStateException();
            }
            assertThat(newComp).selectionBoundsIs(new Rectangle(
                flippedX,
                flippedY,
                origSelection.width,
                origSelection.height
            ));
        }
    }

    @Test
    public void crop() {
        boolean[] options = {false, true};

        // could be simple after this file is transitioned to JUnit 5
        for (boolean selectionCrop : options) {
            for (boolean allowGrowing : options) {
                for (boolean deleteCroppedPixels : options) {
                    for (boolean addHidingMask : options) {
                        if (addHidingMask && !selectionCrop) {
                            continue; // this combination doesn't make sense
                        }
                        if (addHidingMask && !withSelection.isTrue()) {
                            continue;
                        }

                        testCrop(selectionCrop, allowGrowing,
                            deleteCroppedPixels, addHidingMask);
                    }
                }
            }
        }
    }

    private void testCrop(boolean selectionCrop, boolean allowGrowing,
                          boolean deleteCroppedPixels, boolean addHidingMask) {
        var imCropRect = new Rectangle(10, 3, 15, 3);
        Crop crop = new Crop(imCropRect, selectionCrop, allowGrowing, deleteCroppedPixels, addHidingMask);
        Composition cropped = crop.process(origComp).join();
        assert cropped != origComp;
        assert view.getComp() == cropped;
        assertThat(origComp)
            .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT);

        checkStateAfterCrop(selectionCrop, allowGrowing, deleteCroppedPixels, addHidingMask);

        // test undo with one layer
        History.assertNumEditsIs(1);

        String expectedEditName = "Crop";
        if (addHidingMask) {
            expectedEditName = "Crop and Hide";
        }

        History.undo(expectedEditName);
        checkOriginalState();

        History.redo(expectedEditName);
        checkStateAfterCrop(selectionCrop, allowGrowing, deleteCroppedPixels, addHidingMask);

        // prepare next round of testing
        History.undo(expectedEditName);
        checkOriginalState();
    }

    private void checkStateAfterCrop(boolean selectionCrop, boolean allowGrowing,
                                     boolean deleteCroppedPixels, boolean addHidingMask) {
        Composition croppedComp = view.getComp();
        assert croppedComp != origComp;

        int expectedCanvasWidth = 10;
        if (allowGrowing) {
            expectedCanvasWidth = 15;
        }
        assertThat(croppedComp).canvasSizeIs(expectedCanvasWidth, 3);
        ImageLayer croppedLayer = (ImageLayer) croppedComp.getLayer(0);

        if (deleteCroppedPixels) {
            if (allowGrowing) {
                assertThat(croppedLayer).imageSizeIs(15, 3);
            } else {
                assertThat(croppedLayer).imageSizeIs(10, 3);
            }
        } else {
            if (allowGrowing) {
                assertThat(croppedLayer).imageSizeIs(origImageWidth + 5, origImageHeight);
            } else {
                assertThat(croppedLayer).imageSizeIs(origImageWidth, origImageHeight);
            }
        }

        if (selectionCrop) {
            assertThat(croppedComp).doesNotHaveSelection();
        }

        if (addHidingMask) {
            assertThat(croppedLayer).hasMask();
        } else {
            if (withMask.isTrue()) {
                assertThat(croppedLayer).hasMask();
            } else {
                assertThat(croppedLayer).hasNoMask();
            }
        }
    }

    private void checkTranslationOfNonActiveLayer() {
        if (numLayers == NumLayers.TWO) {
            var activeComp = view.getComp();

            var layer2 = (ImageLayer) activeComp.getLayer(1);
            if (withTranslation.isTrue()) {
                assertThat(layer2).translationIs(-4, -4);
            } else {
                assertThat(layer2).translationIs(0, 0);
            }
        }
    }
}
