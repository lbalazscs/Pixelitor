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

package pixelitor.filters.comp;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.CompTester;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.testutils.NumLayers;
import pixelitor.testutils.WithMask;
import pixelitor.testutils.WithSelection;
import pixelitor.testutils.WithTranslation;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_90;

@RunWith(Parameterized.class)
public class MultiLayerEditTest {
    private static final int ORIG_CANVAS_WIDTH = 20;
    private static final int ORIG_CANVAS_HEIGHT = 10;

    private Composition comp;
    private CompTester tester;

    private ImageLayer layer1;
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


    public MultiLayerEditTest(NumLayers numLayers, WithTranslation withTranslation, WithSelection withSelection, WithMask withMask) {
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
    public static void initTests() {
        History.setUndoLevels(10);
    }

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerComposition(true);
        assertThat(comp.toString()).isEqualTo("Composition{name='Test', activeLayer=layer 2, layerList=[" +
                "ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 1', visible=true, " +
                "mask=LayerMask{state=NORMAL, super={tx=0, ty=0, super={name='layer 1 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, " +
                "ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 2', visible=true, " +
                "mask=LayerMask{state=NORMAL, super={tx=0, ty=0, super={name='layer 2 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}], " +
                "canvas=Canvas{width=20, height=10}, selection=null, dirty=false}");
        tester = new CompTester(comp);
        tester.checkDirty(false);

        layer1 = (ImageLayer) comp.getLayer(0);
        layer2 = (ImageLayer) comp.getLayer(1);

        numLayers.init(tester);
        withTranslation.init(tester);
        withSelection.init(tester);
        withMask.init(comp);

        if (withSelection.isYes()) {
            origSelection = tester.getStandardTestSelectionShape();
        }
        History.clear();
    }

    private void checkOriginalState() {
        tester.checkCanvasSize(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT);
        tester.checkActiveLayerAndMaskImageSize(
                origImageWidth,
                origImageHeight);
        tester.checkActiveLayerTranslation(
                origTX,
                origTY);
        if (withSelection.isYes()) {
            tester.checkSelectionBounds(origSelection);
        }

        if (numLayers == NumLayers.MORE) {
            // check the translation of the non-active layer
            assertThat(layer2.getTX()).isEqualTo(0);
            assertThat(layer2.getTY()).isEqualTo(0);
        }

        comp.checkInvariant();
    }

    @Test
    public void testEnlargeCanvas() {
        checkOriginalState();

        int north = 3;
        int east = 4;
        int south = 5;
        int west = 2;
        new EnlargeCanvas(north, east, south, west).process(comp);

        checkEnlargeCanvasAfterState(north, east, south, west);

        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Enlarge Canvas");

            History.undo();
            checkOriginalState();

            History.redo();
            checkEnlargeCanvasAfterState(north, east, south, west);
        }
    }

    private void checkEnlargeCanvasAfterState(int north, int east, int south, int west) {
        int newCanvasWidth = ORIG_CANVAS_WIDTH + west + east;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT + north + south;
        tester.checkCanvasSize(newCanvasWidth, newCanvasHeight);

        tester.checkActiveLayerTranslation(
                Math.min(0, origTX + west),
                Math.min(0, origTY + north));

        tester.checkActiveLayerAndMaskImageSize(
                origImageWidth + east + Math.max(0, origTX + west),
                origImageHeight + south + Math.max(0, origTY + north)
        );

        if (withSelection.isYes()) {
            Rectangle newSelection = new Rectangle(origSelection.x + west,
                    origSelection.y + north, origSelection.width, origSelection.height);
            tester.checkSelectionBounds(newSelection);
        }

        comp.checkInvariant();
        tester.checkDirty(true);
    }

    @Test
    public void testResize() {
        checkOriginalState();

        int targetWidth = ORIG_CANVAS_WIDTH / 2;
        int targetHeight = ORIG_CANVAS_HEIGHT / 2;
        new Resize(targetWidth, targetHeight, false).process(comp);

        checkStateAfterResize();

        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Resize");

            History.undo();
            checkOriginalState();

            History.redo();
            checkStateAfterResize();
        }
    }

    private void checkStateAfterResize() {
        if (withSelection.isYes()) {
            Rectangle halfOfOrigSelection = new Rectangle(
                    origSelection.x / 2,
                    origSelection.y / 2,
                    origSelection.width / 2,
                    origSelection.height / 2);
            tester.checkSelectionBounds(halfOfOrigSelection);
        }

        int newCanvasWidth = ORIG_CANVAS_WIDTH / 2;
        int newCanvasHeight = ORIG_CANVAS_HEIGHT / 2;
        tester.checkCanvasSize(newCanvasWidth, newCanvasHeight);

        tester.checkActiveLayerAndMaskImageSize(
                newCanvasWidth - origTX / 2,
                newCanvasHeight - origTY / 2);

        tester.checkActiveLayerTranslation(
                Math.min(0, origTX / 2),
                Math.min(0, origTY / 2));

        comp.checkInvariant();
        tester.checkDirty(true);
    }

    @Test
    public void testRotate90() {
        checkOriginalState();
        new Rotate(ANGLE_90).process(comp);
        checkStateAfterRotate(ANGLE_90);

        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Rotate 90\u00B0 CW");

            History.undo();
            checkOriginalState();

            History.redo();
            checkStateAfterRotate(ANGLE_90);
        }
    }

    @Test
    public void testRotate180() {
        checkOriginalState();
        new Rotate(ANGLE_180).process(comp);
        checkStateAfterRotate(ANGLE_180);

        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Rotate 180\u00B0");

            History.undo();
            checkOriginalState();

            History.redo();
            checkStateAfterRotate(ANGLE_180);
        }
    }

    @Test
    public void testRotate270() {
        checkOriginalState();
        new Rotate(ANGLE_270).process(comp);
        checkStateAfterRotate(ANGLE_270);

        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Rotate 90\u00B0 CCW");

            History.undo();
            checkOriginalState();

            History.redo();
            checkStateAfterRotate(ANGLE_270);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void checkStateAfterRotate(Rotate.SpecialAngle angle) {
        if (angle == ANGLE_180) {
            tester.checkCanvasSize(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT);
            tester.checkActiveLayerAndMaskImageSize(
                    origImageWidth,
                    origImageHeight);
        } else {
            tester.checkCanvasSize(ORIG_CANVAS_HEIGHT, ORIG_CANVAS_WIDTH);
            tester.checkActiveLayerAndMaskImageSize(
                    origImageHeight,
                    origImageWidth);
        }

        int canvasDistFromImgBottom = origImageHeight - ORIG_CANVAS_HEIGHT + withTranslation.getExpectedTY();
        int canvasDistFromImgRight = origImageWidth - ORIG_CANVAS_WIDTH + withTranslation.getExpectedTX();
        if (angle == ANGLE_90) {
            tester.checkActiveLayerTranslation(
                    canvasDistFromImgBottom,
                    withTranslation.getExpectedTX());
        } else if (angle == ANGLE_180) {
            tester.checkActiveLayerTranslation(
                    canvasDistFromImgRight,
                    canvasDistFromImgBottom);
        } else if (angle == ANGLE_270) {
            tester.checkActiveLayerTranslation(
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

            tester.checkSelectionBounds(rotatedSelectionBounds);
        }

        if (numLayers == NumLayers.MORE) {
            // check the translation of the non-active layer
            assertThat(layer2.getTX()).isEqualTo(0);
            assertThat(layer2.getTY()).isEqualTo(0);
        }

        comp.checkInvariant();
        tester.checkDirty(true);
    }

    @Test
    public void testFlipHorizontal() {
        checkOriginalState();

        new Flip(HORIZONTAL).process(comp);
        checkStateAfterFlip(HORIZONTAL);

        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Flip Horizontal");

            History.undo();
            checkOriginalState();

            History.redo();
            checkStateAfterFlip(HORIZONTAL);
        }
    }

    @Test
    public void testFlipVertical() {
        checkOriginalState();

        new Flip(VERTICAL).process(comp);
        checkStateAfterFlip(VERTICAL);

        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Flip Vertical");

            History.undo();
            checkOriginalState();

            History.redo();
            checkStateAfterFlip(VERTICAL);
        }
    }

    private void checkStateAfterFlip(Flip.Direction direction) {
        tester.checkCanvasSize(ORIG_CANVAS_WIDTH, ORIG_CANVAS_HEIGHT);
        tester.checkActiveLayerAndMaskImageSize(
                origImageWidth,
                origImageHeight);

        if (direction == HORIZONTAL) {
            tester.checkActiveLayerTranslation(
                    -(origImageWidth - ORIG_CANVAS_WIDTH + origTX),
                    origTY);
        } else if (direction == VERTICAL) {
            tester.checkActiveLayerTranslation(
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
            Rectangle flippedSelection = new Rectangle(
                    flippedX, flippedY, origSelection.width, origSelection.height
            );
            tester.checkSelectionBounds(flippedSelection);
        }

        if (numLayers == NumLayers.MORE) {
            // check the translation of the non-active layer
            assertThat(layer2.getTX()).isEqualTo(0);
            assertThat(layer2.getTY()).isEqualTo(0);
        }

        comp.checkInvariant();
        tester.checkDirty(true);
    }

    @Test
    public void testCrop() {
        checkOriginalState();

        new Crop(new Rectangle(3, 3, 6, 3), false, false).process(comp);
        checkAfterCropState();

        // test undo with one layer
        if (numLayers.canUndo()) {
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Crop");

            History.undo();
            checkOriginalState();

            History.redo();
            checkAfterCropState();
        }

        // TODO
        // test selection crop with selection
        // test crop tool crop with selection
        // test with allow growing
    }

    private void checkAfterCropState() {
        String afterCropState = "{canvasWidth=6, canvasHeight=3, tx=0, ty=0, imgWidth=6, imgHeight=3}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(afterCropState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(afterCropState);
        }
        // TODO check translation, selection etc
    }
}
