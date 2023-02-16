/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.io.IO;
import pixelitor.io.OpenRaster;
import pixelitor.io.PXCFormat;
import pixelitor.layers.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Composition I/O tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CompositionIOTest {
    private static final String TEST_IMAGES_DIR = "src/test/resources/";

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    void readJPEG() {
        checkReadSingleLayerImage("jpeg_test_input.jpg");
    }

    @Test
    void readPNG() {
        checkReadSingleLayerImage("png_test_input.png");
    }

    @Test
    void readBMP() {
        checkReadSingleLayerImage("bmp_test_input.bmp");
    }

    @Test
    void readGIF() {
        checkReadSingleLayerImage("gif_test_input.gif");
    }

    @Test
    void readTiff() {
        checkReadSingleLayerImage("tiff_test_input.tiff");
    }

    @Test
    void readTGA() {
        checkReadSingleLayerImage("tga_test_input.tga");
    }

    @Test
    void readPAM() {
        checkReadSingleLayerImage("pam_test_input.pam");
    }

    @Test
    void readPBM() {
        checkReadSingleLayerImage("pbm_test_input.pbm");
    }

    @Test
    void readPGM() {
        checkReadSingleLayerImage("pgm_test_input.pgm");
    }

    @Test
    void readPPM() {
        checkReadSingleLayerImage("ppm_test_input.ppm");
    }

    @Test
    void readPFM() {
        checkReadSingleLayerImage("pfm_test_input.pfm");
    }

    @Test
    void readWritePXC() {
        // read and test
        String[] fileNames = {
            "pxc_test_input.pxc",
            "pxc_file_w_layer_mask.pxc",
            "pxc_file_w_text_layer.pxc",
            "pxc_file_w_adj_layer.pxc",
        };

        List<Consumer<Layer>> extraChecks = new ArrayList<>();
        // extra check for simple pxc file
        extraChecks.add(secondLayer -> assertThat(secondLayer)
            .classIs(ImageLayer.class)
            .blendingModeIs(BlendingMode.MULTIPLY)
            .opacityIs(0.75f));
        // extra check for pxc with layer mask
        extraChecks.add(secondLayer -> assertThat(secondLayer)
            .classIs(ImageLayer.class)
            .hasMask()
            .maskIsLinked()
            .maskIsEnabled());
        // extra check for pxc with text layer
        extraChecks.add(secondLayer -> {
            assert secondLayer instanceof TextLayer;
            assertThat((TextLayer) secondLayer)
                .textIs("T")
                .hasNumEffects(4)
                .hasNoMask();
        });
        // extra check for pxc with adjustment layer
        extraChecks.add(secondLayer -> assertThat(secondLayer)
            .classIs(AdjustmentLayer.class)
            .hasNoMask());

        for (int i = 0; i < fileNames.length; i++) {
            try {
                File inputFile = new File(TEST_IMAGES_DIR, fileNames[i]);
                var comp = checkMultiLayerRead(inputFile, extraChecks.get(i));

                // write to tmp file
                File tmpFile = File.createTempFile("pix_tmp", ".pxc");
                PXCFormat.write(comp, tmpFile);

                // read back and test
                checkMultiLayerRead(tmpFile, extraChecks.get(i));

                boolean deleted = tmpFile.delete();
                if (!deleted) {
                    throw new IllegalStateException("could not delete " + tmpFile.getAbsolutePath());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error while testing " + fileNames[i], e);
            }
        }
    }

    @Test
    void readPXCWithAllFeatures() {
        String fileName = "pxc_all_features.pxc";
        File inputFile = new File(TEST_IMAGES_DIR, fileName);

        var future = IO.loadCompAsync(inputFile);
        var comp = future.join();
        assertThat(comp)
            .numLayersIs(6)
            .typeOfLayerNIs(0, ImageLayer.class)
            .typeOfLayerNIs(1, TextLayer.class)
            .typeOfLayerNIs(2, GradientFillLayer.class)
            .typeOfLayerNIs(3, ShapesLayer.class)
            .typeOfLayerNIs(4, ColorFillLayer.class)
            .typeOfLayerNIs(5, SmartObject.class)
            .canvasSizeIs(10, 10)
            .hasGuides()
            .hasPath()
            .invariantsAreOK();
        checkAsyncReadResult(future);
    }

    @Test
    void readWriteORA() throws IOException {
        Consumer<Layer> extraCheck = secondLayer ->
            assertThat(secondLayer)
                .classIs(ImageLayer.class)
                .blendingModeIs(BlendingMode.MULTIPLY)
                .opacityIs(0.75f);

        // read and test
        File inputFile = new File(TEST_IMAGES_DIR, "gimp_ora_test_input.ora");
        var comp = checkMultiLayerRead(inputFile, extraCheck);

        File tmpFile = File.createTempFile("pix_tmp", ".ora");
        OpenRaster.write(comp, tmpFile);

        // read back and test
        checkMultiLayerRead(tmpFile, extraCheck);

        tmpFile.delete();
    }

    private static void checkReadSingleLayerImage(String fileName) {
        File inputFile = new File(TEST_IMAGES_DIR, fileName);
        var future = IO.loadCompAsync(inputFile);

        var comp = future.join();
        assertThat(comp)
            .numLayersIs(1)
            .canvasSizeIs(10, 10)
            .invariantsAreOK();

        checkAsyncReadResult(future);
    }

    private static void checkAsyncReadResult(CompletableFuture<Composition> cf) {
        assertThat(cf)
            .isDone()
            .isNotCancelled()
            .isNotCompletedExceptionally();
    }

    private static Composition checkMultiLayerRead(File f, Consumer<Layer> secondLayerChecker) {
        var future = IO.loadCompAsync(f);

        var comp = future.join();
        assertThat(comp)
            .numLayersIs(2)
            .canvasSizeIs(10, 10)
            .invariantsAreOK();
        checkAsyncReadResult(future);

        var secondLayer = comp.getLayer(1);
        secondLayerChecker.accept(secondLayer);

        return comp;
    }
}