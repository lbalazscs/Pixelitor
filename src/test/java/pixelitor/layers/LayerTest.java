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

package pixelitor.layers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Canvas;
import pixelitor.Composition;
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
import static org.mockito.Mockito.spy;

/**
 * Tests the functionality common to all Layer subclasses
 */
@RunWith(Parameterized.class)
public class LayerTest {
    private Composition comp;
    private Layer layer;

    @Parameter
    public Class<Layer> layerClass;

    @Parameter(value = 1)
    public WithMask withMask;

    private IconUpdateChecker iconUpdates;

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

//        LayerUI ui = mock(LayerUI.class);
        LayerGUI ui = spy(layer.getUI());
        layer.setUI(ui);

        comp.addLayerNoGUI(layer);

        ImageLayer layer2 = TestHelper.createImageLayer("LayerTest layer 2", comp);
        comp.addLayerNoGUI(layer2);

        withMask.init(layer);
        LayerMask mask = null;
        if (withMask.isYes()) {
            mask = layer.getMask();
        }

        iconUpdates = new IconUpdateChecker(ui, layer, mask, 0, 1);

        comp.setActiveLayer(layer, AddToHistory.YES);

        assert comp.getNrLayers() == 2 : "found " + comp.getNrLayers() + " layers";

        // TODO this should be automatic for all tests
        // or should be avoidable
        TestHelper.setupAnActiveICFor(comp);

        History.clear();
    }

    @Test
    public void test_setVisible() {
        LayerGUI layerUI = layer.getUI();
        assertThat(layer.isVisible()).isTrue();
        assertThat(layerUI.isVisibilityChecked()).isTrue();

        layer.setVisible(false, AddToHistory.YES);

        assertThat(layer.isVisible()).isFalse();
        assertThat(layerUI.isVisibilityChecked()).isFalse();
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Hide Layer");

        History.undo();
        assertThat(layer.isVisible()).isTrue();
        assertThat(layerUI.isVisibilityChecked()).isTrue();

        History.redo();
        assertThat(layer.isVisible()).isFalse();
        assertThat(layerUI.isVisibilityChecked()).isFalse();

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_duplicate() {
        Layer copy = layer.duplicate(false);
        assertThat(copy.getName()).isEqualTo("layer 1 copy");
        assertThat(copy.getClass()).isEqualTo(layer.getClass());

        Layer copy2 = copy.duplicate(false);
        assertThat(copy2.getName()).isEqualTo("layer 1 copy 2");

        Layer copy3 = copy2.duplicate(false);
        assertThat(copy3.getName()).isEqualTo("layer 1 copy 3");

        Layer exactCopy = layer.duplicate(true);
        assertThat(exactCopy.getName()).isEqualTo("layer 1");

        iconUpdates.check(0, 0);
    }

    @Test
    public void testOpacityChange() {
        float oldValue = 1.0f;
        float newValue = 0.7f;
        assertThat(layer.getOpacity()).isEqualTo(oldValue);

        layer.setOpacity(newValue, UpdateGUI.YES, AddToHistory.YES, true);

        assertThat(layer.getOpacity()).isEqualTo(newValue);
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Layer Opacity Change");
        LayerOpacityEdit lastEdit = (LayerOpacityEdit) History.getLastEdit();
        assertSame(layer, lastEdit.getLayer());

        History.undo();
        float opacity = layer.getOpacity();
        assertThat(opacity).isEqualTo(oldValue);

        History.redo();
        assertThat(layer.getOpacity()).isEqualTo(newValue);

        iconUpdates.check(0, 0);
    }

    @Test
    public void testBlendingModeChange() {
        assertSame(BlendingMode.NORMAL, layer.getBlendingMode());

        layer.setBlendingMode(BlendingMode.DIFFERENCE, UpdateGUI.YES, AddToHistory.YES, true);

        assertSame(BlendingMode.DIFFERENCE, layer.getBlendingMode());
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Blending Mode Change");

        History.undo();
        assertSame(BlendingMode.NORMAL, layer.getBlendingMode());

        History.redo();
        assertSame(BlendingMode.DIFFERENCE, layer.getBlendingMode());

        iconUpdates.check(0, 0);
    }

    @Test
    public void testNameChange() {
        assertThat(layer.getName()).isEqualTo("layer 1");

        layer.setName("newName", AddToHistory.YES);

        assertThat(layer.getName()).isEqualTo("newName");
        assertThat(layer.getUI().getLayerName()).isEqualTo("newName");
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Rename Layer to \"newName\"");

        History.undo();
        assertThat(layer.getName()).isEqualTo("layer 1");
        assertThat(layer.getUI().getLayerName()).isEqualTo("layer 1");

        History.redo();
        assertThat(layer.getName()).isEqualTo("newName");
        assertThat(layer.getUI().getLayerName()).isEqualTo("newName");

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_mergeDownOn() {
        ImageLayer lower = TestHelper.createImageLayer("lower", comp);

        layer.mergeDownOn(lower);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_makeActive() {
        Layer layer2 = comp.getLayer(1);
        assertThat(layer2.isActive()).isFalse();

        layer2.makeActive(AddToHistory.YES);

        assertThat(layer2.isActive()).isTrue();
        History.assertNumEditsIs(1);
        History.assertLastEditNameIs("Layer Selection Change");

        History.undo();
        assertThat(layer2.isActive()).isFalse();

        History.redo();
        assertThat(layer2.isActive()).isTrue();

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_resize() {
        Canvas canvas = layer.getComp().getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        layer.resize(canvasWidth, canvasHeight, true);

        layer.resize(30, 25, true);
        layer.resize(25, 30, false);

        layer.resize(canvasWidth, canvasHeight, true);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_crop() {
        layer.crop(new Rectangle(3, 3, 5, 5));
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_dragFinished() {
        assertThat(comp.getLayerIndex(layer)).isEqualTo(0);
        layer.dragFinished(1);
        assertThat(comp.getLayerIndex(layer)).isEqualTo(1);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_addMask() {
        if (!withMask.isYes()) {
            assertThat(layer.hasMask()).isFalse();

            layer.addMask(LayerMaskAddType.REVEAL_ALL);

            assertThat(layer.hasMask()).isTrue();
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Add Layer Mask");

            History.undo();
            assertThat(layer.hasMask()).isFalse();

            History.redo();
            assertThat(layer.hasMask()).isTrue();

            iconUpdates.check(0, 0);
        }
    }

    @Test
    public void test_deleteMask() {
        if (withMask.isYes()) {
            assertThat(layer.hasMask()).isTrue();

            layer.deleteMask(AddToHistory.YES);
            assertThat(layer.hasMask()).isFalse();
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Delete Layer Mask");
            iconUpdates.check(0, 0);

            History.undo();
            assertThat(layer.hasMask()).isTrue();
            iconUpdates.check(0, 1);

            History.redo();
            assertThat(layer.hasMask()).isFalse();
            iconUpdates.check(0, 1);
        }
    }

    @Test
    public void test_setMaskEnabled() {
        if (withMask.isYes()) {
            assertThat(layer.hasMask()).isTrue();
            assertThat(layer.isMaskEnabled()).isTrue();
            LayerMaskActions.EnableDisableMaskAction enableAction = new LayerMaskActions.EnableDisableMaskAction(layer);
            assertThat(enableAction.getName()).isEqualTo("Disable");

            layer.setMaskEnabled(false, AddToHistory.YES);

            assertThat(layer.isMaskEnabled()).isFalse();
            assertThat(enableAction.getName()).isEqualTo("Enable");
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Disable Layer Mask");
            iconUpdates.check(0, 1);

            History.undo();
            assertThat(layer.isMaskEnabled()).isTrue();
            assertThat(enableAction.getName()).isEqualTo("Disable");
            iconUpdates.check(0, 2);

            History.redo();
            assertThat(layer.isMaskEnabled()).isFalse();
            assertThat(enableAction.getName()).isEqualTo("Enable");
            iconUpdates.check(0, 3);
        }
    }

    @Test
    public void testMaskLinking() {
        if (withMask.isYes()) {
            assertThat(layer.hasMask()).isTrue();
            LayerMask mask = layer.getMask();
            assertThat(mask.isLinked()).isTrue();
            LayerMaskActions.LinkUnlinkMaskAction linkAction = new LayerMaskActions.LinkUnlinkMaskAction(layer);
            assertThat(linkAction.getName()).isEqualTo("Unlink");

            mask.setLinked(false, AddToHistory.YES);

            assertThat(mask.isLinked()).isFalse();
            assertThat(linkAction.getName()).isEqualTo("Link");
            History.assertNumEditsIs(1);
            History.assertLastEditNameIs("Unlink Layer Mask");

            History.undo();
            assertThat(mask.isLinked()).isTrue();
            assertThat(linkAction.getName()).isEqualTo("Unlink");

            History.redo();
            assertThat(mask.isLinked()).isFalse();
            assertThat(linkAction.getName()).isEqualTo("Link");

            iconUpdates.check(0, 0);
        }
    }
}
