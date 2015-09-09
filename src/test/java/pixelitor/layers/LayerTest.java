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
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.ImageDisplay;
import pixelitor.ImageDisplayStub;
import pixelitor.TestHelper;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.LayerOpacityEdit;
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
    private Composition comp;
    private Layer layer;

    @Parameter
    public Class layerClass;

    @Parameter(value = 1)
    public WithMask withMask;

    @Parameters(name = "{index}: {0}, mask = {1}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {ImageLayer.class, WithMask.NO},
                {ImageLayer.class, WithMask.YES},
                {TextLayer.class, WithMask.NO},
                {TextLayer.class, WithMask.YES},
                {AdjustmentLayer.class, WithMask.NO},
                {AdjustmentLayer.class, WithMask.YES},
        });
    }

    @Before
    public void setUp() {
        comp = TestHelper.createEmptyComposition();
        // make sure each test runs with a fresh Layer
        layer = TestHelper.createLayerOfClass(layerClass, comp);

        comp.addLayerNoGUI(layer);

        ImageLayer layer2 = TestHelper.createImageLayer("LayerTest layer 2", comp);
        comp.addLayerNoGUI(layer2);

        withMask.init(layer);

        comp.setActiveLayer(layer, AddToHistory.YES);

        assert comp.getNrLayers() == 2 : "found " + comp.getNrLayers() + " layers";

        // TODO this should be automatic for all tests
        // or should be avoidable
        ImageDisplay ic = new ImageDisplayStub();
        ImageComponents.setActiveIC(ic, false);
        ((ImageDisplayStub) ic).setComp(comp);
    }

    @Test
    public void testSetVisible() {
        LayerUI layerUI = layer.getUI();
        assertThat(layer.isVisible()).isTrue();
        assertThat(layerUI.isVisibilityChecked()).isTrue();

        layer.setVisible(false, AddToHistory.YES);
        assertThat(layer.isVisible()).isFalse();
        assertThat(layerUI.isVisibilityChecked()).isFalse();

        History.undo();
        assertThat(layer.isVisible()).isTrue();
        assertThat(layerUI.isVisibilityChecked()).isTrue();

        History.redo();
        assertThat(layer.isVisible()).isFalse();
        assertThat(layerUI.isVisibilityChecked()).isFalse();
    }

    @Test
    public void testDuplicate() {
        Layer copy = layer.duplicate();
        assertThat(copy.getName()).isEqualTo("layer 1 copy");
        assertThat(copy.getClass()).isEqualTo(layer.getClass());

        Layer copy2 = copy.duplicate();
        assertThat(copy2.getName()).isEqualTo("layer 1 copy 2");

        Layer copy3 = copy2.duplicate();
        assertThat(copy3.getName()).isEqualTo("layer 1 copy 3");
    }

    @Test
    public void testOpacityChange() {
        float oldValue = 1.0f;
        float newValue = 0.7f;
        assertThat(layer.getOpacity()).isEqualTo(oldValue);

        layer.setOpacity(newValue, UpdateGUI.YES, AddToHistory.YES, true);
        assertThat(layer.getOpacity()).isEqualTo(newValue);

        LayerOpacityEdit lastEdit = (LayerOpacityEdit) History.getLastEdit();
        assertSame(layer, lastEdit.getLayer());

        History.undo();
        float opacity = layer.getOpacity();
        assertThat(opacity).isEqualTo(oldValue);

        History.redo();
        assertThat(layer.getOpacity()).isEqualTo(newValue);
    }

    @Test
    public void testBlendingModeChange() {
        assertSame(BlendingMode.NORMAL, layer.getBlendingMode());

        layer.setBlendingMode(BlendingMode.DIFFERENCE, UpdateGUI.YES, AddToHistory.YES, true);
        assertSame(BlendingMode.DIFFERENCE, layer.getBlendingMode());

        History.undo();
        assertSame(BlendingMode.NORMAL, layer.getBlendingMode());

        History.redo();
        assertSame(BlendingMode.DIFFERENCE, layer.getBlendingMode());
    }

    @Test
    public void testNameChange() {
        assertThat(layer.getName()).isEqualTo("layer 1");

        layer.setName("newName", AddToHistory.YES);
        assertThat(layer.getName()).isEqualTo("newName");
        assertThat(layer.getUI().getLayerName()).isEqualTo("newName");

        History.undo();
        assertThat(layer.getName()).isEqualTo("layer 1");
        assertThat(layer.getUI().getLayerName()).isEqualTo("layer 1");

        History.redo();
        assertThat(layer.getName()).isEqualTo("newName");
        assertThat(layer.getUI().getLayerName()).isEqualTo("newName");
    }

    @Test
    public void testMergeDownOn() {
        ImageLayer lower = TestHelper.createImageLayer("lower", comp);
        layer.mergeDownOn(lower);
    }

    @Test
    public void testMakeActive() {
        Layer layer2 = comp.getLayer(1);

        assertThat(layer2.isActive()).isFalse();
        layer2.makeActive(AddToHistory.YES);
        assertThat(layer2.isActive()).isTrue();

        History.undo();
        assertThat(layer2.isActive()).isFalse();
        History.redo();
        assertThat(layer2.isActive()).isTrue();
    }

    @Test
    public void testResize() {
        Canvas canvas = layer.getComp().getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        layer.resize(canvasWidth, canvasHeight, true);

        layer.resize(30, 25, true);
        layer.resize(25, 30, false);

        layer.resize(canvasWidth, canvasHeight, true);
    }

    @Test
    public void testCrop() {
        layer.crop(new Rectangle(3, 3, 5, 5));
    }

    @Test
    public void testDragFinished() {
        assertThat(comp.getLayerIndex(layer)).isEqualTo(0);
        layer.dragFinished(1);
        assertThat(comp.getLayerIndex(layer)).isEqualTo(1);
    }

    @Test
    public void testAddMask() {
        if (withMask == WithMask.NO) {
            assertThat(layer.hasMask()).isFalse();
            layer.addMask(LayerMaskAddType.REVEAL_ALL);
            assertThat(layer.hasMask()).isTrue();

            History.undo();
            assertThat(layer.hasMask()).isFalse();

            History.redo();
            assertThat(layer.hasMask()).isTrue();
        }
    }

    @Test
    public void testDeleteMask() {
        if (withMask == WithMask.YES) {
            assertThat(layer.hasMask()).isTrue();
            layer.deleteMask(AddToHistory.YES, false);
            assertThat(layer.hasMask()).isFalse();

            History.undo();
            assertThat(layer.hasMask()).isTrue();

            History.redo();
            assertThat(layer.hasMask()).isFalse();
        }
    }

    @Test
    public void testSetMaskEnabled() {
        if (withMask == WithMask.YES) {
            assertThat(layer.hasMask()).isTrue();
            assertThat(layer.isMaskEnabled()).isTrue();

            layer.setMaskEnabled(false, AddToHistory.YES);
            assertThat(layer.isMaskEnabled()).isFalse();

            History.undo();
            assertThat(layer.isMaskEnabled()).isTrue();

            History.redo();
            assertThat(layer.isMaskEnabled()).isFalse();
        }
    }

    @Test
    public void testMaskLinking() {
        if (withMask == WithMask.YES) {
            assertThat(layer.hasMask()).isTrue();
            LayerMask mask = layer.getMask();
            assertThat(mask.isLinked()).isTrue();

            mask.setLinked(false, AddToHistory.YES);
            assertThat(mask.isLinked()).isFalse();

            History.undo();
            assertThat(mask.isLinked()).isTrue();

            History.redo();
            assertThat(mask.isLinked()).isFalse();
        }
    }
}
