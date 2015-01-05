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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class LayerTest {
    private Composition comp;
    private ImageLayer layer;

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerTestComposition();
        layer = (ImageLayer) comp.getLayer(0);
    }

    @Test
    public void testSetVisible() {
        LayerButton layerButton = layer.getLayerButton();
        assertThat(layer.isVisible(), is(true));
        assertThat(layerButton.isVisibilityChecked(), is(true));

        layer.setVisible(false, true);
        assertThat(layer.isVisible(), is(false));
        assertThat(layerButton.isVisibilityChecked(), is(false));
    }

    @Test
    public void testGetDuplicateLayerName() {
        String name = layer.getDuplicateLayerName();
        assertEquals("layer 1 copy", name);
    }

    @Test
    // this is abstract in Layer, each subclass implements differently!
    public void testDuplicate() {
        ImageLayer duplicate = layer.duplicate();
        assertNotNull(duplicate);

        BufferedImage image = layer.getImage();
        BufferedImage duplicateImage = duplicate.getImage();

        assertNotSame(duplicateImage, image);
        assertEquals(duplicateImage.getWidth(), image.getWidth());
        assertEquals(duplicateImage.getHeight(), image.getHeight());

        assertEquals(layer.getBounds(), duplicate.getBounds());
        assertSame(layer.getBlendingMode(), duplicate.getBlendingMode());
        assertThat(duplicate.getOpacity(), is(layer.getOpacity()));
    }

    @Test
    public void testSetOpacity() {
        layer.setOpacity(0.7f, true, true, true);
        assertThat(layer.getOpacity(), is(0.7f));
    }

    @Test
    public void testSetBlendingMode() {
        layer.setBlendingMode(BlendingMode.DIFFERENCE, true, true, true);
        assertSame(BlendingMode.DIFFERENCE, layer.getBlendingMode());
    }

    @Test
    public void testSetName() {
        layer.setName("newName", true);
        assertEquals("newName", layer.getName());
    }

    @Test
    // this method is abstract in Layer, defined in ContentLayer and overridden in ImageLayer!
    public void testMergeDownOn() {
        layer.mergeDownOn(TestHelper.createTestImageLayer("layer 2", comp));
    }

    @Test
    public void testMakeActive() {
        layer.makeActive(true);
        assertEquals(true, layer.isActiveLayer());
    }

    @Test
    // abstract in Layer!
    public void testResize() {
        layer.resize(20, 20, true);
        layer.resize(20, 20, false);

        layer.resize(30, 25, true);
        layer.resize(25, 30, false);

        layer.resize(5, 5, true);
        layer.resize(20, 20, false);
    }

    @Test
    // abstract in Layer!
    public void testCrop() {
        layer.crop(new Rectangle(3, 3, 5, 5));
    }

    @Test
    public void testDragFinished() {
        assertEquals(0, comp.getLayerPosition(layer));
        layer.dragFinished(1);
        assertEquals(1, comp.getLayerPosition(layer));
    }
}