package pixelitor.filters.comp;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pixelitor.CompTester;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.testutils.NumLayers;
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
public class MultiLayerEditTests {
    private Composition comp;
    private CompTester tester;

    private ImageLayer layer1;
    private ImageLayer layer2;

    private NumLayers numLayers;
    private WithTranslation withTranslation;
    private WithSelection withSelection;

    public MultiLayerEditTests(NumLayers numLayers, WithTranslation withTranslation, WithSelection withSelection) {
        this.numLayers = numLayers;
        this.withTranslation = withTranslation;
        this.withSelection = withSelection;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                // TODO add a WithMask parameter
                {NumLayers.MORE, WithTranslation.NO, WithSelection.NO},
                {NumLayers.ONE, WithTranslation.NO, WithSelection.NO},
//                {NumLayers.ONE, WithTranslation.YES, WithSelection.NO},
//                {NumLayers.ONE, WithTranslation.NO, WithSelection.YES},
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
                "mask=ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 1 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, " +
                "ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 2', visible=true, " +
                "mask=ImageLayer{state=NORMAL, super={tx=0, ty=0, super={name='layer 2 MASK', visible=true, mask=null, maskEditing=false, maskEnabled=true, isAdjustment=false}}}, maskEditing=false, maskEnabled=true, isAdjustment=false}}}], " +
                "canvas=Canvas{width=20, height=10}, selection=null, dirty=false}");
        tester = new CompTester(comp);
        tester.checkDirty(false);

        layer1 = (ImageLayer) comp.getLayer(0);
        layer2 = (ImageLayer) comp.getLayer(1);

        numLayers.init(tester);
        withTranslation.init(tester);
        withSelection.init(tester);
    }

    @Test
    public void testEnlargeCanvas() {
        // check original
        Rectangle origSelection = null;
        if (withSelection == WithSelection.YES) {
            origSelection = tester.getStandardTestSelectionShape();
        }

        int origCanvasWidth = 20;
        int origCanvasHeight = 10;
        tester.checkCanvasSize(origCanvasWidth, origCanvasHeight);

        if (withTranslation == WithTranslation.NO) {
            tester.checkActiveLayerAndMaskImageSize(origCanvasWidth, origCanvasHeight);
            tester.checkActiveLayerTranslation(0, 0);
        } else {
            tester.checkActiveLayerAndMaskImageSize(origCanvasWidth + 4, origCanvasHeight + 4);
            tester.checkActiveLayerTranslation(-4, -4);
        }

        int north = 3;
        int east = 4;
        int south = 5;
        int west = 2;
        new EnlargeCanvas(north, east, south, west).process(comp);

        int newCanvasWidth = origCanvasWidth + west + east;
        int newCanvasHeight = origCanvasHeight + north + south;
        tester.checkCanvasSize(newCanvasWidth, newCanvasHeight);

        if (withTranslation == WithTranslation.NO) {
            tester.checkActiveLayerAndMaskImageSize(newCanvasWidth, newCanvasHeight);
            tester.checkActiveLayerTranslation(0, 0);
        } else {
            tester.checkActiveLayerAndMaskImageSize(30, 22);
            tester.checkActiveLayerTranslation(-4, -4); // TODO BUG!!
        }

        Rectangle newSelection = null;
        if (withSelection == WithSelection.YES) {
            newSelection = new Rectangle(origSelection.x + west,
                    origSelection.y + north, origSelection.width, origSelection.height);
            tester.checkSelectionBounds(newSelection);
        }

        comp.checkInvariant();
        tester.checkDirty(true);

        if (numLayers.canUndo()) {
            History.undo();
            tester.checkCanvasSize(20, 10);
            tester.checkActiveLayerAndMaskImageSize(20, 10);
            tester.checkActiveLayerTranslation(0, 0);
            if (withSelection == WithSelection.YES) {
                tester.checkSelectionBounds(origSelection);
            }
            History.redo();
            tester.checkCanvasSize(26, 18);
            tester.checkActiveLayerAndMaskImageSize(26, 18);
            tester.checkActiveLayerTranslation(0, 0);
            if (withSelection == WithSelection.YES) {
                tester.checkSelectionBounds(newSelection);
            }
        }
    }

    @Test
    public void testResize() {
        // check original state
        String expectedState;
        if (withTranslation == WithTranslation.NO) {
            expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        } else {
            expectedState = "{canvasWidth=20, canvasHeight=10, tx=-4, ty=-4, imgWidth=24, imgHeight=14}";
        }
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);

        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(expectedState);
        }

        int targetWidth = 10;
        int targetHeight = 5;
        new Resize(targetWidth, targetHeight, false).process(comp);

        Rectangle orig2Selection = new Rectangle(2, 2, 4, 2); // also scaled down by 2
        if (withSelection == WithSelection.YES) {
            tester.checkSelectionBounds(orig2Selection);
        }

        expectedState = "{canvasWidth=10, canvasHeight=5, tx=0, ty=0, imgWidth=10, imgHeight=5}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(expectedState);
        }


        new Resize(5, 10, false).process(comp);

        expectedState = "{canvasWidth=5, canvasHeight=10, tx=0, ty=0, imgWidth=5, imgHeight=10}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);

        Rectangle newSelection = new Rectangle(1, 4, 2, 4); // x scaled down by 2, y scaled up by 2

        if (withSelection == WithSelection.YES) {
            tester.checkSelectionBounds(newSelection);
        }

        // test undo with one layer
        if (numLayers.canUndo()) {
            History.undo();
            expectedState = "{canvasWidth=10, canvasHeight=5, tx=0, ty=0, imgWidth=10, imgHeight=5}";
            assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);

            if (withSelection == WithSelection.YES) {
                tester.checkSelectionBounds(orig2Selection);
            }

            History.redo();
            expectedState = "{canvasWidth=5, canvasHeight=10, tx=0, ty=0, imgWidth=5, imgHeight=10}";
            assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);

            if (withSelection == WithSelection.YES) {
                tester.checkSelectionBounds(newSelection);
            }
        }
    }

    @Test
    public void testRotate() {
        String expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
        assertThat(layer2.toDebugCanvasString()).isEqualTo(expectedState);

        // test that both layers are rotated
        new Rotate(ANGLE_90).process(comp);
        expectedState = "{canvasWidth=10, canvasHeight=20, tx=0, ty=0, imgWidth=10, imgHeight=20}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(expectedState);
        }

        // test undo with one layer
        if (numLayers.canUndo()) {
            String origState = "{canvasWidth=10, canvasHeight=20, tx=0, ty=0, imgWidth=10, imgHeight=20}";
            assertThat(layer1.toDebugCanvasString()).isEqualTo(origState);

            Rotate.SpecialAngle[] rotations = {ANGLE_90, ANGLE_180, ANGLE_270};
            for (Rotate.SpecialAngle angle : rotations) {
                new Rotate(angle).process(comp);

                if (angle == ANGLE_90 || angle == ANGLE_270) {
                    expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
                } else {
                    expectedState = origState;
                }
                assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);

                History.undo();
                assertThat(layer1.toDebugCanvasString()).isEqualTo(origState);

                History.redo();
                assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);

                // undo again to get ready for the next angle
                History.undo();
                assertThat(layer1.toDebugCanvasString()).isEqualTo(origState);
            }
        }
    }

    @Test
    public void testFlip() {
        String expectedState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(expectedState);
        }

        // test horizontal flip
        new Flip(HORIZONTAL).process(comp);
        // expect no change
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(expectedState);
        }

        // test vertical flip
        new Flip(VERTICAL).process(comp);
        // expect no change
        assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(expectedState);
        }

        if (numLayers.canUndo()) {
            assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
            new Flip(HORIZONTAL).process(comp);
            new Flip(VERTICAL).process(comp);
            assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
            History.undo();
            History.undo();
            assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
            History.redo();
            History.redo();
            assertThat(layer1.toDebugCanvasString()).isEqualTo(expectedState);
        }
    }

    @Test
    public void testCrop() {
        // check initial state
        String initialState = "{canvasWidth=20, canvasHeight=10, tx=0, ty=0, imgWidth=20, imgHeight=10}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(initialState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(initialState);
        }

        // test crop
        new Crop(new Rectangle(3, 3, 6, 3), false, false).process(comp);
        String afterCropState = "{canvasWidth=6, canvasHeight=3, tx=0, ty=0, imgWidth=6, imgHeight=3}";
        assertThat(layer1.toDebugCanvasString()).isEqualTo(afterCropState);
        if (numLayers == NumLayers.MORE) {
            assertThat(layer2.toDebugCanvasString()).isEqualTo(afterCropState);
        }

        // test undo with one layer
        if (numLayers.canUndo()) {
            History.undo();
            assertThat(layer1.toDebugCanvasString()).isEqualTo(initialState);

            History.redo();
            assertThat(layer1.toDebugCanvasString()).isEqualTo(afterCropState);
        }

        // TODO
        // test selection crop with selection
        // test crop tool crop with selection
        // test with allow growing
    }
}
