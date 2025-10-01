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

package pixelitor.compactions;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.testutils.LayerCount;
import pixelitor.testutils.WithMask;
import pixelitor.testutils.WithSelection;
import pixelitor.testutils.WithTranslation;
import pixelitor.tools.Tools;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.compactions.QuadrantAngle.ANGLE_180;

@ParameterizedClass(name = "translation = {0}, sel = {1}, mask = {2}, #layers = {3}")
@MethodSource("instancesToTest")
@DisplayName("comp actions tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CompActionTest {
    private static final int ORIG_CANVAS_WIDTH = 20;
    private static final int ORIG_CANVAS_HEIGHT = 10;

    private Composition origComp;
    private View view;

    private Rectangle origSelection;
    private int origTX;
    private int origTY;
    private int origImageWidth;
    private int origImageHeight;

    // the parameters
    @Parameter(0)
    private WithTranslation withTranslation;

    @Parameter(1)
    private WithSelection withSelection;

    @Parameter(2)
    private WithMask withMask;

    @Parameter(3)
    private LayerCount layerCount;

    static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
            {WithTranslation.NO, WithSelection.NO, WithMask.NO, LayerCount.ONE},
            {WithTranslation.YES, WithSelection.NO, WithMask.NO, LayerCount.ONE},
            {WithTranslation.YES, WithSelection.YES, WithMask.NO, LayerCount.ONE},
            {WithTranslation.YES, WithSelection.YES, WithMask.YES, LayerCount.ONE},
            {WithTranslation.YES, WithSelection.YES, WithMask.YES, LayerCount.TWO},
        });
    }

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode(true);

        Tools.setActiveTool(Tools.CROP);
    }

    @BeforeEach
    void beforeEachTest() {
        origTX = withTranslation.getExpectedTX();
        origTY = withTranslation.getExpectedTY();
        origImageWidth = ORIG_CANVAS_WIDTH - origTX;
        origImageHeight = ORIG_CANVAS_HEIGHT - origTY;

        origComp = TestHelper.createComp("CompActionTest", 2, true);
        assertThat(origComp)
            .hasName("CompActionTest")
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .activeLayerNameIs("layer 2")
            .doesNotHaveSelection()
            .firstLayerHasMask()
            .secondLayerHasMask();

        withTranslation.configure(origComp);
        withSelection.configure(origComp);
        withMask.configure(origComp);
        layerCount.configure(origComp);

        if (withSelection.isTrue()) {
            origSelection = WithSelection.SELECTION_SHAPE;
        }
        History.clear();
        view = origComp.getView();
    }

    @AfterEach
    void afterEachTest() {
        TestHelper.verifyAndClearHistory();
    }

    private void checkOriginalState() {
        assertThat(origComp)
            .isSameAs(view.getComp())
            .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT)
            .activeLayerAndMaskImageSizeIs(origImageWidth, origImageHeight)
            .activeLayerTranslationIs(origTX, origTY)
            .allLayerUIsAreOK()
            .invariantsAreOK();

        if (withSelection.isTrue()) {
            assertThat(origComp).selectionBoundsIs(origSelection);
        }

        checkTranslationOfNonActiveLayer();
    }

    @Test
    void enlargeCanvas() {
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

        var enlargedComp = view.getComp();

        assertThat(enlargedComp)
            .invariantsAreOK()
            .canvasSizeIs(newCanvasWidth, newCanvasHeight)
            .activeLayerTranslationIs(
                Math.min(0, origTX + west),
                Math.min(0, origTY + north))
            .activeLayerAndMaskImageSizeIs(
                origImageWidth + east + Math.max(0, origTX + west),
                origImageHeight + south + Math.max(0, origTY + north));

        if (withSelection.isTrue()) {
            assertThat(enlargedComp).selectionBoundsIs(new Rectangle(
                origSelection.x + west,
                origSelection.y + north,
                origSelection.width,
                origSelection.height));
        }
    }

    @Test
    void resize() {
        checkOriginalState();

        int targetWidth = ORIG_CANVAS_WIDTH / 2;
        int targetHeight = ORIG_CANVAS_HEIGHT / 2;
        Composition resized = new Resize(targetWidth, targetHeight).process(view.getComp()).join();
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
        var resizedComp = view.getComp();

        if (withSelection.isTrue()) {
            var halfOfOrigSelection = new Rectangle(
                origSelection.x / 2,
                origSelection.y / 2,
                origSelection.width / 2,
                origSelection.height / 2);
            assertThat(resizedComp).selectionBoundsIs(halfOfOrigSelection);
        }

        int newCanvasWidth = ORIG_CANVAS_WIDTH / 2;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT / 2;

        assertThat(resizedComp)
            .invariantsAreOK()
            .canvasSizeIs(newCanvasWidth, newCanvasHeight)
            .activeLayerAndMaskImageSizeIs(
                newCanvasWidth - origTX / 2,
                newCanvasHeight - origTY / 2)
            .activeLayerTranslationIs(
                Math.min(0, origTX / 2),
                Math.min(0, origTY / 2));
    }

    @ParameterizedTest
    @EnumSource
    void rotate(QuadrantAngle angle) {
        checkOriginalState();
        Composition rotated = new Rotate(angle).process(view.getComp()).join();
        assert rotated != origComp;
        assert view.getComp() == rotated;

        checkStateAfterRotate(angle);

        History.assertNumEditsIs(1);
        String editName = angle.getDisplayName();

        History.undo(editName);
        checkOriginalState();

        History.redo(editName);
        checkStateAfterRotate(angle);
    }

    private void checkStateAfterRotate(QuadrantAngle angle) {
        var rotatedComp = view.getComp();
        assertThat(rotatedComp).invariantsAreOK();

        checkCanvasAfterRotate(rotatedComp, angle);
        checkTranslationAfterRotate(rotatedComp, angle);
        if (withSelection.isTrue()) {
            checkSelectionAfterRotate(rotatedComp, angle);
        }
    }

    private void checkCanvasAfterRotate(Composition rotatedComp, QuadrantAngle angle) {
        if (angle == ANGLE_180) {
            assertThat(rotatedComp)
                .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT)
                .activeLayerAndMaskImageSizeIs(
                    origImageWidth,
                    origImageHeight);
        } else {
            assertThat(rotatedComp)
                .canvasSizeIs(ORIG_CANVAS_HEIGHT, ORIG_CANVAS_WIDTH)
                .activeLayerAndMaskImageSizeIs(
                    origImageHeight,
                    origImageWidth);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void checkTranslationAfterRotate(Composition rotatedComp, QuadrantAngle angle) {
        int canvasDistFromImgBottom = origImageHeight - ORIG_CANVAS_HEIGHT
            + withTranslation.getExpectedTY();
        int canvasDistFromImgRight = origImageWidth - ORIG_CANVAS_WIDTH
            + withTranslation.getExpectedTX();

        Point expectedTranslation = switch (angle) {
            case ANGLE_90 -> new Point(canvasDistFromImgBottom, withTranslation.getExpectedTX());
            case ANGLE_180 -> new Point(canvasDistFromImgRight, canvasDistFromImgBottom);
            case ANGLE_270 -> new Point(withTranslation.getExpectedTY(), canvasDistFromImgRight);
        };
        assertThat(rotatedComp).activeLayerTranslationIs(expectedTranslation);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void checkSelectionAfterRotate(Composition rotatedComp, QuadrantAngle angle) {
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

        assertThat(rotatedComp).selectionBoundsIs(rotatedSelectionBounds);
    }

    @ParameterizedTest
    @EnumSource
    void flip(FlipDirection direction) {
        checkOriginalState();

        Composition flipped = new Flip(direction).process(view.getComp()).join();
        assert flipped != origComp;
        assert view.getComp() == flipped;

        checkStateAfterFlip(direction);

        History.assertNumEditsIs(1);
        String editName = direction.getDisplayName();

        History.undo(editName);
        checkOriginalState();

        History.redo(editName);
        checkStateAfterFlip(direction);
    }

    private void checkStateAfterFlip(FlipDirection direction) {
        var flippedComp = view.getComp();
        assertThat(flippedComp)
            .invariantsAreOK()
            .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT)
            .activeLayerAndMaskImageSizeIs(origImageWidth, origImageHeight);

        checkTranslationAfterFlip(flippedComp, direction);
        if (withSelection.isTrue()) {
            checkSelectionAfterFlip(flippedComp, direction);
        }
    }

    private void checkTranslationAfterFlip(Composition flippedComp, FlipDirection direction) {
        Point expectedTranslation = switch (direction) {
            case HORIZONTAL -> new Point(-(origImageWidth - ORIG_CANVAS_WIDTH + origTX), origTY);
            case VERTICAL -> new Point(origTX, -(origImageHeight - ORIG_CANVAS_HEIGHT + origTY));
        };
        assertThat(flippedComp).activeLayerTranslationIs(expectedTranslation);
    }

    private void checkSelectionAfterFlip(Composition flippedComp, FlipDirection direction) {
        Point flippedSelOrigin = switch (direction) {
            case HORIZONTAL -> new Point(ORIG_CANVAS_WIDTH - origSelection.x - origSelection.width, origSelection.y);
            case VERTICAL -> new Point(origSelection.x, ORIG_CANVAS_HEIGHT - origSelection.y - origSelection.height);
        };

        assertThat(flippedComp).selectionBoundsIs(new Rectangle(
            flippedSelOrigin.x, flippedSelOrigin.y,
            origSelection.width, origSelection.height
        ));
    }

    @Test
    void crop() {
        boolean[] options = {false, true};

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
        Crop crop = new Crop(imCropRect, selectionCrop, allowGrowing, deleteCroppedPixels, addHidingMask, null);
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

        // undo again, to prepare the next round of testing
        History.undo(expectedEditName);
        checkOriginalState();
    }

    private void checkStateAfterCrop(boolean selectionCrop, boolean allowGrowing,
                                     boolean deleteCroppedPixels, boolean addHidingMask) {
        Composition croppedComp = view.getComp();
        assert croppedComp != origComp;

        int expectedCanvasWidth = allowGrowing ? 15 : 10;

        assertThat(croppedComp).canvasSizeIs(expectedCanvasWidth, 3);
        ImageLayer croppedLayer = (ImageLayer) croppedComp.getLayer(0);

        if (deleteCroppedPixels) {
            assertThat(croppedLayer).imageSizeIs(expectedCanvasWidth, 3);
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
            // a mask was added or changed
            assertThat(croppedLayer).hasMask();
        } else {
            // there is a mask only if we started with one
            assertThat(croppedLayer).hasMask(withMask.isTrue());
        }
    }

    private void checkTranslationOfNonActiveLayer() {
        if (layerCount == LayerCount.TWO) {
            var activeComp = view.getComp();
            var layer2 = (ImageLayer) activeComp.getLayer(1);

            // expect it to have the unchanged translation from the original setup
            assertThat(layer2).translationIs(withTranslation.getExpectedValue());
        }
    }
}
