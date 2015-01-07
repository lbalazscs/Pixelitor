/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
import pixelitor.io.OpenRaster;
import pixelitor.io.OpenSaveManager;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Layer;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class CompositionCreationTest {

    @Test
    public void testNewImage() {
        Composition comp = NewImage.createNewComposition(FillType.WHITE, 20, 20, "New Image");
        comp.checkInvariant();
        assertEquals(1, comp.getNrLayers());
        assertEquals(20, comp.getCanvasWidth());
        assertEquals(20, comp.getCanvasHeight());
        assertNotNull(comp.getCompositeImage());
    }

    private static void testSingleLayerRead(File f) {
        Composition comp = OpenSaveManager.createCompositionFromFile(f);
        comp.checkInvariant();
        assertEquals(1, comp.getNrLayers());
        assertEquals(10, comp.getCanvasWidth());
        assertEquals(10, comp.getCanvasHeight());
        assertNotNull(comp.getCompositeImage());
    }

    private static Composition testMultiLayerRead(File f) {
        Composition comp = OpenSaveManager.createCompositionFromFile(f);
        comp.checkInvariant();
        assertEquals(2, comp.getNrLayers());
        assertEquals(10, comp.getCanvasWidth());
        assertEquals(10, comp.getCanvasHeight());
        assertNotNull(comp.getCompositeImage());
        Layer secondLayer = comp.getLayer(1);

        assertSame(BlendingMode.MULTIPLY, secondLayer.getBlendingMode());
        assertEquals(0.75, secondLayer.getOpacity(), 0.0001);

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
    public void testReadWritePXC() throws IOException {
        // read and test
        File f = new File("src/test/resources/pxc_test_input.pxc");
        Composition comp = testMultiLayerRead(f);

        // write to tmp file
        File tmp = File.createTempFile("pix_tmp", ".pxc");
        OpenSaveManager.serializePXC(comp, tmp);

        // read back and test
        testMultiLayerRead(tmp);

        tmp.delete();
    }

    @Test
    public void testReadWriteORA() throws IOException {
        // read and test
        File f = new File("src/test/resources/gimp_ora_test_input.ora");
        Composition comp = testMultiLayerRead(f);

        File tmp = File.createTempFile("pix_tmp", ".ora");
        OpenRaster.writeOpenRaster(comp, tmp, true);

        // read back and test
        testMultiLayerRead(tmp);

        tmp.delete();
    }

}