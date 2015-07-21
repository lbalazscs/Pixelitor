/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.utils.UpdateGUI;

import java.awt.Rectangle;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LayerTest {
    private Composition comp;
    private Layer layer1;

    @Before
    public void setUp() {
        comp = TestHelper.create2LayerTestComposition(false);
        layer1 = comp.getLayer(0);
    }

    @Test
    public void testSetVisible() {
        LayerButton layerButton = layer1.getLayerButton();
        assertThat(layer1.isVisible(), is(true));
        assertThat(layerButton.isVisibilityChecked(), is(true));

        layer1.setVisible(false, AddToHistory.YES);
        assertThat(layer1.isVisible(), is(false));
        assertThat(layerButton.isVisibilityChecked(), is(false));

        History.undo();
        assertThat(layer1.isVisible(), is(true));
        assertThat(layerButton.isVisibilityChecked(), is(true));

        History.redo();
        assertThat(layer1.isVisible(), is(false));
        assertThat(layerButton.isVisibilityChecked(), is(false));
    }

    @Test
    public void testGetDuplicateLayerName() {
        String name = layer1.getDuplicateLayerName();
        assertEquals("layer 1 copy", name);
    }

    @Test
    public void testSetOpacity() {
        assertThat(layer1.getOpacity(), is(1.0f));

        layer1.setOpacity(0.7f, UpdateGUI.YES, AddToHistory.YES, true);
        assertThat(layer1.getOpacity(), is(0.7f));

        History.undo();
        assertThat(layer1.getOpacity(), is(1.0f));

        History.redo();
        assertThat(layer1.getOpacity(), is(0.7f));
    }

    @Test
    public void testSetBlendingMode() {
        assertSame(BlendingMode.NORMAL, layer1.getBlendingMode());

        layer1.setBlendingMode(BlendingMode.DIFFERENCE, UpdateGUI.YES, AddToHistory.YES, true);
        assertSame(BlendingMode.DIFFERENCE, layer1.getBlendingMode());

        History.undo();
        assertSame(BlendingMode.NORMAL, layer1.getBlendingMode());

        History.redo();
        assertSame(BlendingMode.DIFFERENCE, layer1.getBlendingMode());
    }

    @Test
    public void testSetName() {
        assertEquals("layer 1", layer1.getName());

        layer1.setName("newName", AddToHistory.YES);
        assertEquals("newName", layer1.getName());

        History.undo();
        assertEquals("layer 1", layer1.getName());
        History.redo();
        assertEquals("newName", layer1.getName());
    }

    @Test
    public void testMakeActive() {
        assertFalse(layer1.isActive());
        layer1.makeActive(AddToHistory.YES);
        assertTrue(layer1.isActive());

        History.undo();
        assertFalse(layer1.isActive());
        History.redo();
        assertTrue(layer1.isActive());
    }

    @Test
    // abstract in Layer!
    public void testResize() {
        layer1.resize(20, 20, true);
        layer1.resize(20, 20, false);

        layer1.resize(30, 25, true);
        layer1.resize(25, 30, false);

        layer1.resize(5, 5, true);
        layer1.resize(20, 20, false);
    }

    @Test
    // abstract in Layer!
    public void testCrop() {
        layer1.crop(new Rectangle(3, 3, 5, 5));
    }

    @Test
    public void testDragFinished() {
        assertEquals(0, comp.getLayerIndex(layer1));
        layer1.dragFinished(1);
        assertEquals(1, comp.getLayerIndex(layer1));
    }
}