/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import org.junit.jupiter.params.provider.Arguments;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.compactions.QuadrantAngle.ANGLE_180;

@ParameterizedClass(name = "translation = {0}, sel = {1}, mask = {2}, #layers = {3}")
@MethodSource("instancesToTest")
@DisplayName("comp actions tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CompActionTest {
    private static final int ORIG_CANVAS_WIDTH = 20;
    private static final int ORIG_CANVAS_HEIGHT = 10;

    private static final int CROP_X = 10;
    private static final int CROP_Y = 3;
    private static final int CROP_WIDTH = 15;
    private static final int CROP_HEIGHT = 3;

    private Composition origComp;
    private View view;

    private Rectangle origSelection;
    private int origTx;
    private int origTy;
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

    static Stream<Arguments> instancesToTest() {
        return TestHelper.combinations(
            List.of(WithTranslation.NO, WithTranslation.YES),
            List.of(WithSelection.NO, WithSelection.YES),
            List.of(WithMask.NO, WithMask.YES),
            List.of(LayerCount.ONE, LayerCount.TWO)
        );
    }

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode(true);

        Tools.setActiveTool(Tools.CROP);
    }

    @BeforeEach
    void beforeEachTest() {
        origTx = withTranslation.getExpectedTx();
        origTy = withTranslation.getExpectedTy();
        origImageWidth = ORIG_CANVAS_WIDTH - origTx;
        origImageHeight = ORIG_CANVAS_HEIGHT - origTy;

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

        checkOriginalState();
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
            .activeLayerTranslationIs(origTx, origTy)
            .allLayerUIsAreOK()
            .invariantsAreOK();

        if (withSelection.isTrue()) {
            assertThat(origComp).selectionBoundsIs(origSelection);
        }

        checkTranslationOfNonActiveLayer(withTranslation.getExpectedValue());
    }

    private void testActionWithUndoRedo(CompAction action, String editName, Consumer<Composition> postActionChecker) {
        Composition result = action.process(origComp).join();
        assertThat(result)
            .isNotSameAs(origComp)
            .isSameAs(view.getComp())
            .invariantsAreOK();

        postActionChecker.accept(result);

        History.assertNumEditsIs(1);

        History.undo(editName);
        checkOriginalState();

        History.redo(editName);
        Composition redoResult = view.getComp();
        assertThat(redoResult)
            .isSameAs(result)
            .invariantsAreOK();
        postActionChecker.accept(redoResult);
    }

    @Test
    void enlargeCanvas() {
        int north = 3;
        int east = 4;
        int south = 5;
        int west = 2;
        testActionWithUndoRedo(
            new EnlargeCanvas(north, east, south, west),
            "Enlarge Canvas",
            comp -> checkStateAfterEnlargeCanvas(north, east, south, west));
    }

    private void checkStateAfterEnlargeCanvas(int north, int east, int south, int west) {
        int newCanvasWidth = ORIG_CANVAS_WIDTH + west + east;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT + north + south;

        var enlargedComp = view.getComp();

        assertThat(enlargedComp)
            .canvasSizeIs(newCanvasWidth, newCanvasHeight)
            .activeLayerTranslationIs(
                Math.min(0, origTx + west),
                Math.min(0, origTy + north))
            .activeLayerAndMaskImageSizeIs(
                origImageWidth + east + Math.max(0, origTx + west),
                origImageHeight + south + Math.max(0, origTy + north));

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
        int targetWidth = ORIG_CANVAS_WIDTH / 2;
        int targetHeight = ORIG_CANVAS_HEIGHT / 2;

        testActionWithUndoRedo(
            new Resize(targetWidth, targetHeight),
            "Resize",
            comp -> checkStateAfterResize());
    }

    private void checkStateAfterResize() {
        var resizedComp = view.getComp();

        if (withSelection.isTrue()) {
            var resizedSelSize = new Rectangle(
                origSelection.x / 2,
                origSelection.y / 2,
                origSelection.width / 2,
                origSelection.height / 2);
            assertThat(resizedComp).selectionBoundsIs(resizedSelSize);
        }

        int newCanvasWidth = ORIG_CANVAS_WIDTH / 2;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT / 2;

        assertThat(resizedComp)
            .canvasSizeIs(newCanvasWidth, newCanvasHeight)
            .activeLayerAndMaskImageSizeIs(
                newCanvasWidth - origTx / 2,
                newCanvasHeight - origTy / 2)
            .activeLayerTranslationIs(
                Math.min(0, origTx / 2),
                Math.min(0, origTy / 2));
    }

    @ParameterizedTest
    @EnumSource
    void rotate(QuadrantAngle angle) {
        testActionWithUndoRedo(
            new Rotate(angle),
            angle.getDisplayName(),
            comp -> checkStateAfterRotate(angle));
    }

    private void checkStateAfterRotate(QuadrantAngle angle) {
        var rotatedComp = view.getComp();

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
            + withTranslation.getExpectedTy();
        int canvasDistFromImgRight = origImageWidth - ORIG_CANVAS_WIDTH
            + withTranslation.getExpectedTx();

        Point expectedTranslation = switch (angle) {
            case ANGLE_90 -> new Point(canvasDistFromImgBottom, withTranslation.getExpectedTx());
            case ANGLE_180 -> new Point(canvasDistFromImgRight, canvasDistFromImgBottom);
            case ANGLE_270 -> new Point(withTranslation.getExpectedTy(), canvasDistFromImgRight);
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
        testActionWithUndoRedo(
            new Flip(direction),
            direction.getDisplayName(),
            comp -> checkStateAfterFlip(direction));
    }

    private void checkStateAfterFlip(FlipDirection direction) {
        var flippedComp = view.getComp();
        assertThat(flippedComp)
            .canvasSizeIs(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT)
            .activeLayerAndMaskImageSizeIs(origImageWidth, origImageHeight);

        checkTranslationAfterFlip(flippedComp, direction);
        if (withSelection.isTrue()) {
            checkSelectionAfterFlip(flippedComp, direction);
        }
    }

    private void checkTranslationAfterFlip(Composition flippedComp, FlipDirection direction) {
        Point expectedTranslation = switch (direction) {
            case HORIZONTAL -> new Point(-(origImageWidth - ORIG_CANVAS_WIDTH + origTx), origTy);
            case VERTICAL -> new Point(origTx, -(origImageHeight - ORIG_CANVAS_HEIGHT + origTy));
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
        var cropRect = new Rectangle(CROP_X, CROP_Y, CROP_WIDTH, CROP_HEIGHT);
        String editName = addHidingMask ? "Crop and Hide" : "Crop";

        testActionWithUndoRedo(
            new Crop(cropRect, selectionCrop, allowGrowing, deleteCroppedPixels, addHidingMask, null),
            editName,
            comp -> checkStateAfterCrop(selectionCrop, allowGrowing, deleteCroppedPixels, addHidingMask));

        // undo again, to prepare the next round of testing
        History.undo(editName);
        checkOriginalState();
    }

    private void checkStateAfterCrop(boolean selectionCrop, boolean allowGrowing,
                                     boolean deleteCroppedPixels, boolean addHidingMask) {
        Composition croppedComp = view.getComp();

        int expectedCanvasWidth = allowGrowing ? CROP_WIDTH : ORIG_CANVAS_WIDTH - CROP_X;
        int expectedCanvasHeight = CROP_HEIGHT;

        assertThat(croppedComp)
            .canvasSizeIs(expectedCanvasWidth, expectedCanvasHeight)
            .hasSelection(withSelection.isTrue() && !selectionCrop); // selection crop should deselect

        int expectedImageWidth;
        int expectedImageHeight;

        if (deleteCroppedPixels) {
            expectedImageWidth = expectedCanvasWidth;
            expectedImageHeight = expectedCanvasHeight;
        } else {
            expectedImageHeight = origImageHeight;
            if (allowGrowing) {
                int growth = (CROP_X + CROP_WIDTH) - ORIG_CANVAS_WIDTH;
                expectedImageWidth = origImageWidth + growth;
            } else {
                expectedImageWidth = origImageWidth;
            }
        }

        ImageLayer croppedLayer = (ImageLayer) croppedComp.getActiveLayer();
        assertThat(croppedLayer).imageSizeIs(expectedImageWidth, expectedImageHeight);

        if (addHidingMask) {
            // a mask was added or changed
            assertThat(croppedLayer).hasMask();
        } else {
            // there is a mask only if we started with one
            assertThat(croppedLayer).hasMask(withMask.isTrue());
        }

        Point expectedTranslation = new Point(
            deleteCroppedPixels ? 0 : origTx - CROP_X,
            deleteCroppedPixels ? 0 : origTy - CROP_Y);
        assertThat(croppedComp).activeLayerTranslationIs(expectedTranslation);
        // verify that the non-active layer has the same translation
        checkTranslationOfNonActiveLayer(expectedTranslation);
    }

    private void checkTranslationOfNonActiveLayer(Point expectedValue) {
        if (layerCount == LayerCount.TWO) {
            var activeComp = view.getComp();
            var layer1 = (ImageLayer) activeComp.getLayer(0);

            assertThat(layer1)
                .isNotActive()
                .translationIs(expectedValue);
        }
    }
}
