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

package pixelitor;

import org.junit.Test;
import pixelitor.colors.FillType;
import pixelitor.io.OpenRaster;
import pixelitor.io.OpenSaveManager;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CompositionCreationTest {

    @Test
    public void testNewImage() {
        Composition comp = NewImage.createNewComposition(FillType.WHITE, 20, 20, "New Image");
        comp.checkInvariant();
        assertThat(comp.getNrLayers()).isEqualTo(1);
        assertThat(comp.getCanvasWidth()).isEqualTo(20);
        assertThat(comp.getCanvasHeight()).isEqualTo(20);
        assertThat(comp.getCompositeImage()).isNotNull();
    }

    private static void testSingleLayerRead(File f) {
        Composition comp = OpenSaveManager.createCompositionFromFile(f);
        comp.checkInvariant();
        assertThat(comp.getNrLayers()).isEqualTo(1);
        assertThat(comp.getCanvasWidth()).isEqualTo(10);
        assertThat(comp.getCanvasHeight()).isEqualTo(10);
        assertThat(comp.getCompositeImage()).isNotNull();
    }

    private static Composition testMultiLayerRead(File f, Consumer<Layer> checkSecondLayer) {
        Composition comp = OpenSaveManager.createCompositionFromFile(f);
        comp.checkInvariant();
        assertThat(comp.getNrLayers()).isEqualTo(2);
        assertThat(comp.getCanvasWidth()).isEqualTo(10);
        assertThat(comp.getCanvasHeight()).isEqualTo(10);
        assertThat(comp.getCompositeImage()).isNotNull();

        Layer secondLayer = comp.getLayer(1);
        checkSecondLayer.accept(secondLayer);

        return comp;
    }


    @Test
    public void testReadJPEG() {
        File f = new File("src/test/resources/jpeg_test_input.jpg");
        testSingleLayerRead(f);
    }

    @Test
    public void testReadPNG() {
        File f = new File("src/test/resources/png_test_input.png");
        testSingleLayerRead(f);
    }

    @Test
    public void testReadBMP() {
        File f = new File("src/test/resources/bmp_test_input.bmp");
        testSingleLayerRead(f);
    }

    @Test
    public void testReadGIF() {
        File f = new File("src/test/resources/gif_test_input.gif");
        testSingleLayerRead(f);
    }

    @Test
    public void testReadWritePXC() {
        // read and test
        String[] fileNames = {
                "src/test/resources/pxc_test_input.pxc",
                "src/test/resources/pxc_file_w_layer_mask.pxc",
                "src/test/resources/pxc_file_w_text_layer.pxc",
                "src/test/resources/pxc_file_w_adj_layer.pxc",
        };

        List<Consumer<Layer>> extraChecks = new ArrayList<>();
        // extra check for simple pxc file
        extraChecks.add(secondLayer -> {
            assert secondLayer instanceof ImageLayer;
            assertSame(BlendingMode.MULTIPLY, secondLayer.getBlendingMode());
            assertEquals(0.75, secondLayer.getOpacity(), 0.0001);
        });
        // extra check for pxc with layer mask
        extraChecks.add(secondLayer -> {
            assert secondLayer instanceof ImageLayer;
            assert secondLayer.hasMask();
            assert secondLayer.getMask().isLinked();
            assert secondLayer.isMaskEnabled();
        });
        // extra check for pxc with text layer
        extraChecks.add(secondLayer -> {
            assert !secondLayer.hasMask();
            assert secondLayer instanceof TextLayer;
            assertThat(((TextLayer) secondLayer).getSettings().getText()).isEqualTo("T");
        });
        // extra check for pxc with adjustment layer
        extraChecks.add(secondLayer -> {
            assert !secondLayer.hasMask();
            assert secondLayer instanceof AdjustmentLayer;
        });

        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            try {
                File f = new File(fileName);
                Composition comp = testMultiLayerRead(f, extraChecks.get(i));

                // write to tmp file
                File tmp = File.createTempFile("pix_tmp", ".pxc");
                OpenSaveManager.serializePXC(comp, tmp);

                // read back and test
                testMultiLayerRead(tmp, extraChecks.get(i));

                tmp.delete();
            } catch (Exception e) {
                throw new IllegalStateException("Error while testing " + fileName, e);
            }
        }
    }

    @Test
    public void testReadWriteORA() throws IOException {
        Consumer<Layer> extraCheck = secondLayer -> {
            assert secondLayer instanceof ImageLayer;
            assertSame(BlendingMode.MULTIPLY, secondLayer.getBlendingMode());
            assertEquals(0.75, secondLayer.getOpacity(), 0.0001);
        };

        // read and test
        File f = new File("src/test/resources/gimp_ora_test_input.ora");
        Composition comp = testMultiLayerRead(f, extraCheck);

        File tmp = File.createTempFile("pix_tmp", ".ora");
        OpenRaster.writeOpenRaster(comp, tmp, true);

        // read back and test
        testMultiLayerRead(tmp, extraCheck);

        tmp.delete();
    }

}