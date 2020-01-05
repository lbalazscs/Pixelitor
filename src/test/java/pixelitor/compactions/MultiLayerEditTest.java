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

package pixelitor.compactions;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.testutils.NumLayers;
import pixelitor.testutils.WithMask;
import pixelitor.testutils.WithSelection;
import pixelitor.testutils.WithTranslation;
import pixelitor.tools.Tools;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.compactions.Flip.Direction.HORIZONTAL;
import static pixelitor.compactions.Flip.Direction.VERTICAL;
import static pixelitor.compactions.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.compactions.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.compactions.Rotate.SpecialAngle.ANGLE_90;

@RunWith(Parameterized.class)
public class MultiLayerEditTest {
    private static final int ORIG_CANVAS_WIDTH = 20;
    private static final int ORIG_CANVAS_HEIGHT = 10;

    private Composition comp;

    private ImageLayer layer2;

    private Rectangle origSelection;
    private final int origTX;
    private final int origTY;
    private final int origImageWidth;
    private final int origImageHeight;

    // the parameters
    private final NumLayers numLayers;
    private final WithTranslation withTranslation;
    private final WithSelection withSelection;
    private final WithMask withMask;

    public MultiLayerEditTest(NumLayers numLayers,
                              WithTranslation withTranslation,
                              WithSelection withSelection,
                              WithMask withMask) {
        this.numLayers = numLayers;
        this.withSelection = withSelection;
        this.withTranslation = withTranslation;
        this.withMask = withMask;

        origTX = withTranslation.getExpectedTX();
        origTY = withTranslation.getExpectedTY();

        origImageWidth = ORIG_CANVAS_WIDTH - origTX;
        origImageHeight = ORIG_CANVAS_HEIGHT - origTY;
    }

    @Parameters(name = "{index}: layers = {0}, translation = {1}, selection = {2}, mask = {3}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {NumLayers.MORE, WithTranslation.NO, WithSelection.NO, WithMask.NO},
                {NumLayers.MORE, WithTranslation.NO, WithSelection.NO, WithMask.YES},
                {NumLayers.ONE, WithTranslation.NO, WithSelection.NO, WithMask.YES},
                {NumLayers.ONE, WithTranslation.YES, WithSelection.NO, WithMask.YES},
                {NumLayers.ONE, WithTranslation.NO, WithSelection.YES, WithMask.YES},
        });
    }

    @BeforeClass
    public static void setupClass() {
        Build.setUnitTestingMode();

        Tools.setCurrentTool(Tools.CROP);
    }

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerComposition(true);
        assertThat(comp)
                .isNotEmpty()
                .hasName("Test")
                .numLayersIs(2)
                .layerNamesAre("layer 1", "layer 2")
                .activeLayerNameIs("layer 2")
                .doesNotHaveSelection()
                .firstLayerHasMask()
                .secondLayerHasMask();

//        ImageLayer layer1 = (ImageLayer) comp.getLayer(0);
        layer2 = (ImageLayer) comp.getLayer(1);

        numLayers.setupFor(comp);
        withTranslation.setupFor(comp);
        withSelection.setupFor(comp);
        withMask.setupFor(comp);

        if (withSelection.isYes()) {
            origSelection = WithSelection.SELECTION_SHAPE;
        }
        History.clear();
    }

    private void checkOriginalState() {
        assertThat(comp)
                .hasCanvasImWidth(ORIG_CANVAS_WIDTH)
                .hasCanvasImHeight(ORIG_CANVAS_HEIGHT)
                .activeLayerAndMaskImageSizeIs(origImageWidth, origImageHeight)
                .activeLayerTranslationIs(origTX, origTY)
                .invariantIsOK();

        if (withSelection.isYes()) {
            assertThat(comp).selectionBoundsIs(origSelection);
        }

        if (numLayers == NumLayers.MORE) {
            // check the translation of the non-active layer
            assertThat(layer2).translationIs(0, 0);
        }
    }

    @Test
    public void testEnlargeCanvas() {
        checkOriginalState();

        int north = 3;
        int east = 4;
        int south = 5;
        int west = 2;
        new EnlargeCanvas(north, east, south, west).process(comp).join();

        checkEnlargeCanvasAfterState(north, east, south, west);

        History.assertNumEditsIs(1);

        History.undo("Enlarge Canvas");
        checkOriginalState();

        History.redo("Enlarge Canvas");
        checkEnlargeCanvasAfterState(north, east, south, west);

    }

    private void checkEnlargeCanvasAfterState(int north, int east, int south, int west) {
        int newCanvasWidth = ORIG_CANVAS_WIDTH + west + east;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT + north + south;

        var newComp = OpenImages.getActiveComp();

        assertThat(newComp)
                .invariantIsOK()
                .canvasSizeIs(newCanvasWidth, newCanvasHeight)
                .activeLayerTranslationIs(
                        Math.min(0, origTX + west),
                        Math.min(0, origTY + north))
                .activeLayerAndMaskImageSizeIs(
                        origImageWidth + east + Math.max(0, origTX + west),
                        origImageHeight + south + Math.max(0, origTY + north));

        if (withSelection.isYes()) {
            assertThat(newComp).selectionBoundsIs(new Rectangle(
                    origSelection.x + west,
                    origSelection.y + north,
                    origSelection.width,
                    origSelection.height));
        }
    }

    @Test
    public void testResize() {
        checkOriginalState();

        int targetWidth = ORIG_CANVAS_WIDTH / 2;
        int targetHeight = ORIG_CANVAS_HEIGHT / 2;
        new Resize(targetWidth, targetHeight, false).process(comp).join();

        checkStateAfterResize();

        History.assertNumEditsIs(1);

        History.undo("Resize");
        checkOriginalState();

        History.redo("Resize");
        checkStateAfterResize();
    }

    private void checkStateAfterResize() {
        var newComp = OpenImages.getActiveComp();

        if (withSelection.isYes()) {
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
    public void testRotate90() {
        checkOriginalState();
        new Rotate(ANGLE_90).process(comp).join();
        checkStateAfterRotate(ANGLE_90);

        History.assertNumEditsIs(1);

        History.undo("Rotate 90° CW");
        checkOriginalState();

        History.redo("Rotate 90° CW");
        checkStateAfterRotate(ANGLE_90);
    }

    @Test
    public void testRotate180() {
        checkOriginalState();
        new Rotate(ANGLE_180).process(comp).join();
        checkStateAfterRotate(ANGLE_180);

        History.assertNumEditsIs(1);

        History.undo("Rotate 180°");
        checkOriginalState();

        History.redo("Rotate 180°");
        checkStateAfterRotate(ANGLE_180);
    }

    @Test
    public void testRotate270() {
        checkOriginalState();
        new Rotate(ANGLE_270).process(comp).join();
        checkStateAfterRotate(ANGLE_270);

        History.assertNumEditsIs(1);

        History.undo("Rotate 90° CCW");
        checkOriginalState();

        History.redo("Rotate 90° CCW");
        checkStateAfterRotate(ANGLE_270);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void checkStateAfterRotate(Rotate.SpecialAngle angle) {
        var newComp = OpenImages.getActiveComp();

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
        if (angle == ANGLE_90) {
            assertThat(newComp).activeLayerTranslationIs(
                    canvasDistFromImgBottom,
                    withTranslation.getExpectedTX());
        } else if (angle == ANGLE_180) {
            assertThat(newComp).activeLayerTranslationIs(
                    canvasDistFromImgRight,
                    canvasDistFromImgBottom);
        } else if (angle == ANGLE_270) {
            assertThat(newComp).activeLayerTranslationIs(
                    withTranslation.getExpectedTY(),
                    canvasDistFromImgRight);
        }

        if (withSelection.isYes()) {
            Rectangle rotatedSelectionBounds = null;

            int distFromBottom = ORIG_CANVAS_HEIGHT - origSelection.height - origSelection.y;
            int distFromRight = ORIG_CANVAS_WIDTH - origSelection.width - origSelection.x;
            int distFromLeft = origSelection.x;
            int distFromTop = origSelection.y;

            if (angle == ANGLE_90) {
                rotatedSelectionBounds = new Rectangle(
                        distFromBottom,
                        distFromLeft,
                        origSelection.height,
                        origSelection.width);
            } else if (angle == ANGLE_180) {
                rotatedSelectionBounds = new Rectangle(
                        distFromRight,
                        distFromBottom,
                        origSelection.width,
                        origSelection.height);
            } else if (angle == ANGLE_270) {
                rotatedSelectionBounds = new Rectangle(
                        distFromTop,
                        distFromRight,
                        origSelection.height,
                        origSelection.width);
            }

            assertThat(newComp).selectionBoundsIs(rotatedSelectionBounds);
        }

        if (numLayers == NumLayers.MORE) {
            // check the translation of the non-active layer
            assertThat(layer2).translationIs(0, 0);
        }

        assertThat(comp).invariantIsOK();
    }

    @Test
    public void testFlipHorizontal() {
        checkOriginalState();

        new Flip(HORIZONTAL).process(comp).join();
        checkStateAfterFlip(HORIZONTAL);

        History.assertNumEditsIs(1);

        History.undo("Flip Horizontal");
        checkOriginalState();

        History.redo("Flip Horizontal");
        checkStateAfterFlip(HORIZONTAL);
    }

    @Test
    public void testFlipVertical() {
        checkOriginalState();

        new Flip(VERTICAL).process(comp).join();
        checkStateAfterFlip(VERTICAL);

        History.assertNumEditsIs(1);

        History.undo("Flip Vertical");
        checkOriginalState();

        History.redo("Flip Vertical");
        checkStateAfterFlip(VERTICAL);
    }

    private void checkStateAfterFlip(Flip.Direction direction) {
        var newComp = OpenImages.getActiveComp();

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

        if (withSelection.isYes()) {
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

        if (numLayers == NumLayers.MORE) {
            // check the translation of the non-active layer
            assertThat(layer2).translationIs(0, 0);
        }
    }

    @Test
    public void testCrop() {
        checkOriginalState();

        var imCropRect = new Rectangle(3, 3, 6, 3);
        new Crop(imCropRect, false, false, true, false)
                .process(comp).join();
        checkAfterCropState();

        // test undo with one layer
        History.assertNumEditsIs(1);

        History.undo("Crop");
        checkOriginalState();

        History.redo("Crop");
        checkAfterCropState();

        // TODO
        // test selection crop with selection
        // test crop tool crop with selection
        // test with allow growing
    }

    private void checkAfterCropState() {
        String afterCropState = "{canvasWidth=6, canvasHeight=3, tx=0, ty=0, imgWidth=6, imgHeight=3}";

        // the layer references have changed after the crop
        var newComp = OpenImages.getActiveComp();
        var newLayer1 = (ImageLayer) newComp.getLayer(0);

        assertThat(newLayer1.toDebugCanvasString()).isEqualTo(afterCropState);
        if (numLayers == NumLayers.MORE) {
            var newLayer2 = (ImageLayer) newComp.getLayer(1);
            assertThat(newLayer2.toDebugCanvasString()).isEqualTo(afterCropState);
        }
        // TODO check translation, selection etc
    }
}
