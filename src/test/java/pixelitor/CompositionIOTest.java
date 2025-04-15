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

package pixelitor;

import org.junit.jupiter.api.*;
import pixelitor.io.FileIO;
import pixelitor.io.OpenRaster;
import pixelitor.io.PXCFormat;
import pixelitor.layers.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Composition I/O tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CompositionIOTest {
    private static final String TEST_RESOURCES_PATH = "src/test/resources/";

    // all test files have a 10x10 canvas
    private static final int EXPECTED_CANVAS_WIDTH = 10;
    private static final int EXPECTED_CANVAS_HEIGHT = 10;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("read JPEG")
    void shouldReadJPEGFormat() {
        checkSingleLayerImageRead("jpeg_test_input.jpg");
    }

    @Test
    @DisplayName("read PNG")
    void shouldReadPNGFormat() {
        checkSingleLayerImageRead("png_test_input.png");
    }

    @Test
    @DisplayName("read BMP")
    void shouldReadBMPFormat() {
        checkSingleLayerImageRead("bmp_test_input.bmp");
    }

    @Test
    @DisplayName("read GIF")
    void shouldReadGIFFormat() {
        checkSingleLayerImageRead("gif_test_input.gif");
    }

    @Test
    @DisplayName("read TIFF")
    void shouldReadTIFFFormat() {
        checkSingleLayerImageRead("tiff_test_input.tiff");
    }

    @Test
    @DisplayName("read TGA")
    void shouldReadTGAFormat() {
        checkSingleLayerImageRead("tga_test_input.tga");
    }

    @Test
    @DisplayName("read PAM")
    void shouldReadPAMFormat() {
        checkSingleLayerImageRead("pam_test_input.pam");
    }

    @Test
    @DisplayName("read PBM")
    void shouldReadPBMFormat() {
        checkSingleLayerImageRead("pbm_test_input.pbm");
    }

    @Test
    @DisplayName("read PGM")
    void shouldReadPGMFormat() {
        checkSingleLayerImageRead("pgm_test_input.pgm");
    }

    @Test
    @DisplayName("read PPM")
    void shouldReadPPMFormat() {
        checkSingleLayerImageRead("ppm_test_input.ppm");
    }

    @Test
    @DisplayName("read PFM")
    void shouldReadPFMFormat() {
        checkSingleLayerImageRead("pfm_test_input.pfm");
    }

    @Test
    @DisplayName("read/write PXC")
    void shouldReadAndWritePXCFormat() {
        Map<String, Consumer<Layer>> tests = initSimplePXCTests();

        for (var testCase : tests.entrySet()) {
            String fileName = testCase.getKey();
            Consumer<Layer> secondLayerValidator = testCase.getValue();
            try {
                File inputFile = getTestResourceFile(fileName);

                // test reading
                var comp = checkMultiLayerRead(inputFile, secondLayerValidator);

                // Test round-trip by writing to temporary file and reading back
                File tmpFile = File.createTempFile("pix_tmp", ".pxc");
                PXCFormat.write(comp, tmpFile);
                checkMultiLayerRead(tmpFile, secondLayerValidator);

                cleanupTempFile(tmpFile);
            } catch (Exception e) {
                throw new IllegalStateException("Error while testing " + fileName, e);
            }
        }
    }

    private static Map<String, Consumer<Layer>> initSimplePXCTests() {
        // map files to file-specific extra checks for the second layer
        Map<String, Consumer<Layer>> tests = new LinkedHashMap<>();

        tests.put("pxc_test_input.pxc", secondLayer ->
            assertThat(secondLayer)
                .isInstanceOf(ImageLayer.class)
                .blendingModeIs(BlendingMode.MULTIPLY)
                .opacityIs(0.75f));

        tests.put("pxc_file_w_layer_mask.pxc", secondLayer ->
            assertThat(secondLayer)
                .isInstanceOf(ImageLayer.class)
                .hasMask()
                .maskIsLinked()
                .maskIsEnabled());

        tests.put("pxc_file_w_text_layer.pxc", secondLayer -> {
            assert secondLayer instanceof TextLayer;
            assertThat((TextLayer) secondLayer)
                .textIs("T")
                .hasNumEffects(4)
                .hasNoMask();
        });

        tests.put("pxc_file_w_adj_layer.pxc", secondLayer ->
            assertThat(secondLayer)
                .isInstanceOf(AdjustmentLayer.class)
                .hasNoMask());

        return tests;
    }

    @Test
    @DisplayName("read/write complete PXC")
    void readPXCWithAllFeatures() {
        String fileName = "pxc_all_features.pxc";
        File inputFile = getTestResourceFile(fileName);

        var loadFuture = FileIO.loadCompAsync(inputFile);
        var comp = loadFuture.join();
        assertThat(comp)
            .numLayersIs(6)
            .typeOfLayerNIs(0, ImageLayer.class)
            .typeOfLayerNIs(1, TextLayer.class)
            .typeOfLayerNIs(2, GradientFillLayer.class)
            .typeOfLayerNIs(3, ShapesLayer.class)
            .typeOfLayerNIs(4, ColorFillLayer.class)
            .typeOfLayerNIs(5, SmartObject.class)
            .canvasSizeIs(EXPECTED_CANVAS_WIDTH, EXPECTED_CANVAS_HEIGHT)
            .hasGuides()
            .hasPath()
            .invariantsAreOK();

        checkAsyncReadResult(loadFuture);
    }

    @Test
    @DisplayName("read/write ORA")
    void readWriteORA() throws IOException {
        Consumer<Layer> secondLayerValidator = secondLayer ->
            assertThat(secondLayer)
                .isInstanceOf(ImageLayer.class)
                .blendingModeIs(BlendingMode.MULTIPLY)
                .opacityIs(0.75f);

        // Test reading
        File inputFile = getTestResourceFile("gimp_ora_test_input.ora");
        var comp = checkMultiLayerRead(inputFile, secondLayerValidator);

        // Test round-trip
        File tmpFile = File.createTempFile("pix_tmp", ".ora");
        OpenRaster.write(comp, tmpFile);
        checkMultiLayerRead(tmpFile, secondLayerValidator);

        cleanupTempFile(tmpFile);
    }

    private static void checkSingleLayerImageRead(String fileName) {
        File inputFile = getTestResourceFile(fileName);
        var loadFuture = FileIO.loadCompAsync(inputFile);

        var comp = loadFuture.join();
        assertThat(comp)
            .numLayersIs(1)
            .canvasSizeIs(EXPECTED_CANVAS_WIDTH, EXPECTED_CANVAS_HEIGHT)
            .invariantsAreOK();

        checkAsyncReadResult(loadFuture);
    }

    private static Composition checkMultiLayerRead(File f, Consumer<Layer> secondLayerValidator) {
        var loadFuture = FileIO.loadCompAsync(f);

        var comp = loadFuture.join();
        assertThat(comp)
            .numLayersIs(2)
            .canvasSizeIs(EXPECTED_CANVAS_WIDTH, EXPECTED_CANVAS_HEIGHT)
            .invariantsAreOK();
        checkAsyncReadResult(loadFuture);

        Layer secondLayer = comp.getLayer(1);
        secondLayerValidator.accept(secondLayer);

        return comp;
    }

    private static void checkAsyncReadResult(CompletableFuture<Composition> cf) {
        assertThat(cf)
            .isDone()
            .isNotCancelled()
            .isNotCompletedExceptionally();
    }

    private static File getTestResourceFile(String fileName) {
        return new File(TEST_RESOURCES_PATH, fileName);
    }

    private static void cleanupTempFile(File tmpFile) {
        boolean deleted = tmpFile.delete();
        if (!deleted) {
            throw new IllegalStateException("could not delete " + tmpFile.getAbsolutePath());
        }
    }
}