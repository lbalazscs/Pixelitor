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

package pixelitor.layers;

import org.junit.Before;
import org.junit.Test;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.filters.comp.Flip;
import pixelitor.history.TranslateEdit;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ContentLayerTest {
    private Composition comp;
    private ContentLayer layer;

    @Before
    public void setUp() {
        comp = TestHelper.createEmptyTestComposition();
        layer = TestHelper.createTestImageLayer("imageLayer 1", comp);
    }

    @Test
    public void testLayerMovingMethods() {
        assertEquals(0, layer.getTranslationX());
        assertEquals(0, layer.getTranslationY());

        layer.startTranslation();

        assertEquals(0, layer.getTranslationX());
        assertEquals(0, layer.getTranslationY());

        layer.moveLayerRelative(2, 2);

        assertEquals(2, layer.getTranslationX());
        assertEquals(2, layer.getTranslationY());

        layer.moveLayerRelative(3, 3);

        assertEquals(3, layer.getTranslationX());
        assertEquals(3, layer.getTranslationY());

        layer.endTranslation();

        // the layer was enlarged in endTranslation, and the translation reset to 0, 0
        assertEquals(0, layer.getTranslationX());
        assertEquals(0, layer.getTranslationY());

        layer.startTranslation();
        layer.moveLayerRelative(-1, -2);

        assertEquals(-1, layer.getTranslationX());
        assertEquals(-2, layer.getTranslationY());

        layer.endTranslation();

        // this time the layer was not enlarged
        assertEquals(-1, layer.getTranslationX());
        assertEquals(-2, layer.getTranslationY());
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testCreateTranslateEdit() {
        TranslateEdit edit = layer.createTranslateEdit(5, 5);
        assertNotNull(edit);
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testFlip() {
        layer.flip(Flip.Direction.HORIZONTAL);
        layer.flip(Flip.Direction.VERTICAL);
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testRotate() {
        layer.rotate(90);
        layer.rotate(180);
        layer.rotate(270);
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testEnlargeCanvas() {
        layer.enlargeCanvas(5, 5, 5, 10);
    }

    @Test
    public void testPaintLayer() {
        Graphics2D g2 = TestHelper.createGraphics();
        BufferedImage image = TestHelper.createTestImage();

        BufferedImage resultImage = layer.paintLayer(g2, true, image);
        assertNull(resultImage); // content layers return null

        resultImage = layer.paintLayer(g2, false, image);
        assertNull(resultImage); // content layers return null
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testPaintLayerOnGraphics() {
        Graphics2D g2 = TestHelper.createGraphics();
        layer.paintLayerOnGraphics(g2, true);
        layer.paintLayerOnGraphics(g2, false);
    }

    @Test(expected = NullPointerException.class)
    public void testGetMaskedImage_Fail() {
        BufferedImage maskedImage = layer.getMaskedImage(true);
        assertNotNull(maskedImage);
    }

    @Test
    public void testGetMaskedImage_OK() {
        // setup a layer mask
        layer.addTestLayerMask();

        BufferedImage maskedImage = layer.getMaskedImage(true);
        assertNotNull(maskedImage);
        maskedImage = layer.getMaskedImage(false);
        assertNotNull(maskedImage);
    }


    @Test
    public void testSetupDrawingComposite() {
        Graphics2D g = TestHelper.createGraphics();
        layer.setupDrawingComposite(g, true);
        layer.setupDrawingComposite(g, false);
    }
}