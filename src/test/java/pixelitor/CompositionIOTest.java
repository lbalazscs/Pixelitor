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
public class CompositionIOTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    void readJPEG() {
        File f = new File("src/test/resources/jpeg_test_input.jpg");
        checkSingleLayerRead(f);
    }

    @Test
    void readPNG() {
        File f = new File("src/test/resources/png_test_input.png");
        checkSingleLayerRead(f);
    }

    @Test
    void readBMP() {
        File f = new File("src/test/resources/bmp_test_input.bmp");
        checkSingleLayerRead(f);
    }

    @Test
    void readGIF() {
        File f = new File("src/test/resources/gif_test_input.gif");
        checkSingleLayerRead(f);
    }

    @Test
    void readWritePXC() {
        // read and test
        String[] fileNames = {
            "src/test/resources/pxc_test_input.pxc",
            "src/test/resources/pxc_file_w_layer_mask.pxc",
            "src/test/resources/pxc_file_w_text_layer.pxc",
            "src/test/resources/pxc_file_w_adj_layer.pxc",
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
                .hasNoMask();
        });
        // extra check for pxc with adjustment layer
        extraChecks.add(secondLayer -> assertThat(secondLayer)
            .classIs(AdjustmentLayer.class)
            .hasNoMask());

        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            try {
                File f = new File(fileName);
                var comp = checkMultiLayerRead(f, extraChecks.get(i));

                // write to tmp file
                File tmp = File.createTempFile("pix_tmp", ".pxc");
                PXCFormat.write(comp, tmp);

                // read back and test
                checkMultiLayerRead(tmp, extraChecks.get(i));

                boolean deleted = tmp.delete();
                if (!deleted) {
                    throw new IllegalStateException("could not delete " + tmp.getAbsolutePath());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error while testing " + fileName, e);
            }
        }
    }

    @Test
    void readWriteORA() throws IOException {
        Consumer<Layer> extraCheck = secondLayer ->
            assertThat(secondLayer)
                .classIs(ImageLayer.class)
                .blendingModeIs(BlendingMode.MULTIPLY)
                .opacityIs(0.75f);

        // read and test
        File f = new File("src/test/resources/gimp_ora_test_input.ora");
        var comp = checkMultiLayerRead(f, extraCheck);

        File tmp = File.createTempFile("pix_tmp", ".ora");
        OpenRaster.write(comp, tmp, true);

        // read back and test
        checkMultiLayerRead(tmp, extraCheck);

        tmp.delete();
    }

    private static void checkSingleLayerRead(File f) {
        var future = IO.loadCompAsync(f);

        var comp = future.join();
        assertThat(comp)
            .numLayersIs(1)
            .canvasSizeIs(10, 10)
            .invariantIsOK();

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
            .invariantIsOK();
        checkAsyncReadResult(future);

        var secondLayer = comp.getLayer(1);
        secondLayerChecker.accept(secondLayer);

        return comp;
    }
}