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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertSame;

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
        assertThat(layer1.isVisible()).isTrue();
        assertThat(layerButton.isVisibilityChecked()).isTrue();

        layer1.setVisible(false, AddToHistory.YES);
        assertThat(layer1.isVisible()).isFalse();
        assertThat(layerButton.isVisibilityChecked()).isFalse();

        History.undo();
        assertThat(layer1.isVisible()).isTrue();
        assertThat(layerButton.isVisibilityChecked()).isTrue();

        History.redo();
        assertThat(layer1.isVisible()).isFalse();
        assertThat(layerButton.isVisibilityChecked()).isFalse();
    }

    @Test
    public void testGetDuplicateLayerName() {
        String name = layer1.getDuplicateLayerName();
        assertThat(name).isEqualTo("layer 1 copy");
    }

    @Test
    public void testSetOpacity() {
        assertThat(layer1.getOpacity()).isEqualTo(1.0f);

        layer1.setOpacity(0.7f, UpdateGUI.YES, AddToHistory.YES, true);
        assertThat(layer1.getOpacity()).isEqualTo(0.7f);

        History.undo();
        assertThat(layer1.getOpacity()).isEqualTo(1.0f);

        History.redo();
        assertThat(layer1.getOpacity()).isEqualTo(0.7f);
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
        assertThat(layer1.getName()).isEqualTo("layer 1");

        layer1.setName("newName", AddToHistory.YES);
        assertThat(layer1.getName()).isEqualTo("newName");

        History.undo();
        assertThat(layer1.getName()).isEqualTo("layer 1");
        History.redo();
        assertThat(layer1.getName()).isEqualTo("newName");
    }

    @Test
    public void testMakeActive() {
        assertThat(layer1.isActive()).isFalse();
        layer1.makeActive(AddToHistory.YES);
        assertThat(layer1.isActive()).isTrue();

        History.undo();
        assertThat(layer1.isActive()).isFalse();
        History.redo();
        assertThat(layer1.isActive()).isTrue();
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
        assertThat(comp.getLayerIndex(layer1)).isEqualTo(0);
        layer1.dragFinished(1);
        assertThat(comp.getLayerIndex(layer1)).isEqualTo(1);
    }
}