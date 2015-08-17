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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.filters.Invert;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;
import pixelitor.utils.UpdateGUI;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertSame;

/**
 * Tests the functionality common to all Layer subclasses
 */
@RunWith(Parameterized.class)
public class LayerTest {
    private static Composition comp;

    private Layer layer1;
    private WithMask withMask;

    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() {
        // this method is called first of all methods,
        // therefore instantiate comp here
        comp = TestHelper.createEmptyComposition();

        return Arrays.asList(new Object[][]{
                {new ImageLayer(comp, "layer 1"), WithMask.NO},
                {new ImageLayer(comp, "layer 1"), WithMask.YES},
                {TestHelper.createTextLayer(comp, "layer 1"), WithMask.NO},
                {TestHelper.createTextLayer(comp, "layer 1"), WithMask.YES},
                {new AdjustmentLayer(comp, "layer 1", new Invert()), WithMask.NO},
                {new AdjustmentLayer(comp, "layer 1", new Invert()), WithMask.YES},
        });
    }

    public LayerTest(Layer layer, WithMask withMask) {
        this.layer1 = layer;
        this.withMask = withMask;
    }

    @Before
    public void setUp() {
        comp.testOnlyRemoveAllLayers();
        comp.addLayerNoGUI(layer1);

        ImageLayer layer2 = TestHelper.createImageLayer("layer 2", comp);
        comp.addLayerNoGUI(layer2);

        withMask.init(layer1);

        assert comp.getNrLayers() == 2 : "found " + comp.getNrLayers() + " layers";
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

        History.undo();
    }

    @Test
    public void testDuplicate() {
        Layer copy = layer1.duplicate();
        assertThat(copy.getName())
                .as(layer1.getClass().getSimpleName())
                .isEqualTo("layer 1 copy");
        assertThat(copy.getClass()).isEqualTo(layer1.getClass());

        Layer copy2 = copy.duplicate();
        assertThat(copy2.getName()).isEqualTo("layer 1 copy 2");

        Layer copy3 = copy2.duplicate();
        assertThat(copy3.getName()).isEqualTo("layer 1 copy 3");
    }

    @Test
    public void testOpacityChange() {
        assertThat(layer1.getOpacity()).isEqualTo(1.0f);

        layer1.setOpacity(0.7f, UpdateGUI.YES, AddToHistory.YES, true);
        assertThat(layer1.getOpacity()).isEqualTo(0.7f);

        History.undo();
        assertThat(layer1.getOpacity()).isEqualTo(1.0f);

        History.redo();
        assertThat(layer1.getOpacity()).isEqualTo(0.7f);

        History.undo();
    }

    @Test
    public void testBlendingModeChange() {
        assertSame(BlendingMode.NORMAL, layer1.getBlendingMode());

        layer1.setBlendingMode(BlendingMode.DIFFERENCE, UpdateGUI.YES, AddToHistory.YES, true);
        assertSame(BlendingMode.DIFFERENCE, layer1.getBlendingMode());

        History.undo();
        assertSame(BlendingMode.NORMAL, layer1.getBlendingMode());

        History.redo();
        assertSame(BlendingMode.DIFFERENCE, layer1.getBlendingMode());

        History.undo();
    }

    @Test
    public void testNameChange() {
        assertThat(layer1.getName()).isEqualTo("layer 1");

        layer1.setName("newName", AddToHistory.YES);
        assertThat(layer1.getName()).isEqualTo("newName");
        assertThat(layer1.getLayerButton().getLayerName()).isEqualTo("newName");

        History.undo();
        assertThat(layer1.getName()).isEqualTo("layer 1");
        assertThat(layer1.getLayerButton().getLayerName()).isEqualTo("layer 1");

        History.redo();
        assertThat(layer1.getName()).isEqualTo("newName");
        assertThat(layer1.getLayerButton().getLayerName()).isEqualTo("newName");

        History.undo();
    }

    @Test
    public void testMergeDownOn() {
        ImageLayer lower = TestHelper.createImageLayer("lower", comp);
        layer1.mergeDownOn(lower);
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
        History.undo();
    }

    @Test
    public void testResize() {
        Canvas canvas = layer1.getComp().getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        layer1.resize(canvasWidth, canvasHeight, true);

        layer1.resize(30, 25, true);
        layer1.resize(25, 30, false);

        layer1.resize(canvasWidth, canvasHeight, true);
    }

    @Test
    public void testCrop() {
        layer1.crop(new Rectangle(3, 3, 5, 5));
    }

    @Test
    public void testDragFinished() {
        assertThat(comp.getLayerIndex(layer1)).isEqualTo(0);
        layer1.dragFinished(1);
        assertThat(comp.getLayerIndex(layer1)).isEqualTo(1);
    }

    @Test
    public void testAddMask() {
        if (withMask == WithMask.NO) {
            assertThat(layer1.hasMask()).isFalse();
            layer1.addMask(LayerMaskAddType.REVEAL_ALL);
            assertThat(layer1.hasMask()).isTrue();

            History.undo();
            assertThat(layer1.hasMask()).isFalse();

            History.redo();
            assertThat(layer1.hasMask()).isTrue();

            History.undo();
        }
    }

    @Test
    public void testDeleteMask() {
        if (withMask == WithMask.YES) {
            assertThat(layer1.hasMask()).isTrue();
            layer1.deleteMask(AddToHistory.YES, false);
            assertThat(layer1.hasMask()).isFalse();

            History.undo();
            assertThat(layer1.hasMask()).isTrue();

            History.redo();
            assertThat(layer1.hasMask()).isFalse();

            History.undo();
        }
    }

    @Test
    public void testSetMaskEnabled() {
        if (withMask == WithMask.YES) {
            assertThat(layer1.hasMask()).isTrue();
            assertThat(layer1.isMaskEnabled()).isTrue();

            layer1.setMaskEnabled(false, AddToHistory.YES);
            assertThat(layer1.isMaskEnabled()).isFalse();

            History.undo();
            assertThat(layer1.isMaskEnabled()).isTrue();

            History.redo();
            assertThat(layer1.isMaskEnabled()).isFalse();

            History.undo();
        }
    }
}