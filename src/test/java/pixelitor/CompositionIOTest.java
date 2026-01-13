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

package pixelitor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import pixelitor.io.FileIO;
import pixelitor.io.OpenRaster;
import pixelitor.io.PXCFormat;
import pixelitor.layers.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Composition I/O tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CompositionIOTest {
    // all test files have a 10x10 canvas
    private static final int EXPECTED_CANVAS_WIDTH = 10;
    private static final int EXPECTED_CANVAS_HEIGHT = 10;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @ParameterizedTest(name = "read {0}")
    @ValueSource(strings = {
        "jpeg_test_input.jpg", "png_test_input.png", "bmp_test_input.bmp",
        "gif_test_input.gif", "tiff_test_input.tiff", "tga_test_input.tga",
        "pam_test_input.pam", "pbm_test_input.pbm", "pgm_test_input.pgm",
        "ppm_test_input.ppm", "pfm_test_input.pfm", "webp_test_input.webp"
    })
    void shouldReadSingleLayerFormats(String fileName) {
        checkSingleLayerImageRead(fileName);
    }

    static Stream<Arguments> pxcTestProvider() {
        // pair files with file-specific extra checks on the second layer
        return Stream.of(
            Arguments.of("pxc_test_input.pxc", (Consumer<Layer>) secondLayer ->
                assertThat(secondLayer)
                    .isInstanceOf(ImageLayer.class)
                    .blendingModeIs(BlendingMode.MULTIPLY)
                    .opacityIs(0.75f)),
            Arguments.of("pxc_w_layer_mask.pxc", (Consumer<Layer>) secondLayer ->
                assertThat(secondLayer)
                    .isInstanceOf(ImageLayer.class)
                    .hasMask()
                    .maskIsLinked()
                    .maskIsEnabled()),
            Arguments.of("pxc_w_text_layer.pxc", (Consumer<Layer>) secondLayer -> {
                assert secondLayer instanceof TextLayer;
                assertThat((TextLayer) secondLayer)
                    .textIs("T")
                    .hasNumEffects(4)
                    .hasNoMask();
            }),
            Arguments.of("pxc_w_adj_layer.pxc", (Consumer<Layer>) secondLayer ->
                assertThat(secondLayer)
                    .isInstanceOf(AdjustmentLayer.class)
                    .hasNoMask())
        );
    }

    @ParameterizedTest(name = "read/write PXC file {0}")
    @MethodSource("pxcTestProvider")
    void pxcRoundTrip(String fileName, Consumer<Layer> secondLayerValidator, @TempDir File tempDir) {
        File inputFile = getTestResourceFile(fileName);

        // test reading
        var comp = checkMultiLayerRead(inputFile, secondLayerValidator);

        // test round-trip by writing to temporary file and reading back
        File tmpFile = new File(tempDir, fileName);
        PXCFormat.write(comp, tmpFile);
        checkMultiLayerRead(tmpFile, secondLayerValidator);
    }

    @Test
    @DisplayName("read/write complete PXC")
    void readPXCWithAllFeatures() {
        File inputFile = getTestResourceFile("pxc_all_features.pxc");

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

        checkFutureCompletedOK(loadFuture);
    }

    @Test
    @DisplayName("read/write ORA")
    void oraRoundTrip(@TempDir File tempDir) throws IOException {
        Consumer<Layer> secondLayerValidator = secondLayer ->
            assertThat(secondLayer)
                .isInstanceOf(ImageLayer.class)
                .blendingModeIs(BlendingMode.MULTIPLY)
                .opacityIs(0.75f);

        // test reading
        File inputFile = getTestResourceFile("gimp_ora_test_input.ora");
        var comp = checkMultiLayerRead(inputFile, secondLayerValidator);

        // test round-trip
        File tmpFile = new File(tempDir, "pix_tmp.ora");
        OpenRaster.write(comp, tmpFile);
        checkMultiLayerRead(tmpFile, secondLayerValidator);
    }

    private static void checkSingleLayerImageRead(String fileName) {
        File inputFile = getTestResourceFile(fileName);
        var loadFuture = FileIO.loadCompAsync(inputFile);
        var comp = loadFuture.join();

        assertThat(comp)
            .numLayersIs(1)
            .canvasSizeIs(EXPECTED_CANVAS_WIDTH, EXPECTED_CANVAS_HEIGHT)
            .invariantsAreOK();

        assertThat(comp.getLayer(0))
            .isInstanceOf(ImageLayer.class)
            .blendingModeIs(BlendingMode.NORMAL)
            .isOpaque()
            .hasNoMask();

        checkFutureCompletedOK(loadFuture);
    }

    private static Composition checkMultiLayerRead(File f, Consumer<Layer> secondLayerValidator) {
        var loadFuture = FileIO.loadCompAsync(f);

        var comp = loadFuture.join();
        assertThat(comp)
            .numLayersIs(2)
            .canvasSizeIs(EXPECTED_CANVAS_WIDTH, EXPECTED_CANVAS_HEIGHT)
            .invariantsAreOK();
        checkFutureCompletedOK(loadFuture);

        Layer bottomLayer = comp.getLayer(0);
        assertThat(bottomLayer)
            .isInstanceOf(ImageLayer.class)
            .blendingModeIs(BlendingMode.NORMAL)
            .isOpaque();

        Layer secondLayer = comp.getLayer(1);
        secondLayerValidator.accept(secondLayer);

        return comp;
    }

    private static void checkFutureCompletedOK(CompletableFuture<Composition> cf) {
        assertThat(cf)
            .isDone()
            .isNotCancelled()
            .isNotCompletedExceptionally();
    }

    private static File getTestResourceFile(String fileName) {
        URL resource = CompositionIOTest.class.getClassLoader().getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Test resource not found: " + fileName);
        }
        return new File(resource.getFile());
    }
}
