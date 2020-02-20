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

package pixelitor.layers;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Build;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.history.LayerOpacityEdit;
import pixelitor.layers.LayerMaskActions.EnableDisableMaskAction;
import pixelitor.layers.LayerMaskActions.LinkUnlinkMaskAction;
import pixelitor.testutils.WithMask;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertSame;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.layers.BlendingMode.DIFFERENCE;
import static pixelitor.layers.BlendingMode.NORMAL;

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

    @BeforeClass
    public static void beforeAllTests() {
        Build.setUnitTestingMode();
    }

    @Before
    public void beforeEachTest() {
        comp = TestHelper.createEmptyComposition();
        // make sure each test runs with a fresh Layer
        layer = TestHelper.createLayerOfClass(layerClass, comp);

        comp.addLayerInInitMode(layer);

        var layer2 = TestHelper.createImageLayer("LayerTest layer 2", comp);
        comp.addLayerInInitMode(layer2);

        withMask.setupFor(layer);
        LayerMask mask = null;
        if (withMask.isYes()) {
            mask = layer.getMask();
        }

        iconUpdates = new IconUpdateChecker(layer, mask, 0, 1);

        comp.setActiveLayer(layer, true, true, null);

        assert comp.getNumLayers() == 2 : "found " + comp.getNumLayers() + " layers";

        History.clear();
    }

    @Test
    public void test_setVisible() {
        assertThat(layer)
                .isVisible()
                .uiIsVisible();

        layer.setVisible(false, true);
        assertThat(layer)
                .isNotVisible()
                .uiIsNotVisible();
        History.assertNumEditsIs(1);

        History.undo("Hide Layer");
        assertThat(layer)
                .isVisible()
                .uiIsVisible();

        History.redo("Hide Layer");
        assertThat(layer)
                .isNotVisible()
                .uiIsNotVisible();

        iconUpdates.check(0, 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_duplicate() {
        Layer copy = layer.duplicate(false);
        assertThat(copy)
                .nameIs("layer 1 copy")
                .classIs(layer.getClass());

        Layer copy2 = copy.duplicate(false);
        assertThat(copy2)
                .nameIs("layer 1 copy 2")
                .classIs(layer.getClass());

        Layer copy3 = copy2.duplicate(false);
        assertThat(copy3)
                .nameIs("layer 1 copy 3")
                .classIs(layer.getClass());

        Layer exactCopy = layer.duplicate(true);
        assertThat(exactCopy)
                .nameIs("layer 1")
                .classIs(layer.getClass());

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_setOpacity() {
        float oldValue = 1.0f;
        float newValue = 0.7f;
        assertThat(layer).opacityIs(oldValue);

        layer.setOpacity(newValue, true, true, true);

        assertThat(layer).opacityIs(newValue);
        History.assertNumEditsIs(1);
        var lastEdit = (LayerOpacityEdit) History.getLastEdit();
        assertSame(layer, lastEdit.getLayer());

        History.undo("Layer Opacity Change");
        assertThat(layer).opacityIs(oldValue);

        History.redo("Layer Opacity Change");
        assertThat(layer).opacityIs(newValue);

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_setBlendingMode() {
        assertThat(layer).blendingModeIs(NORMAL);

        layer.setBlendingMode(DIFFERENCE, true, true, true);

        assertThat(layer).blendingModeIs(DIFFERENCE);
        History.assertNumEditsIs(1);

        History.undo("Blending Mode Change");
        assertThat(layer).blendingModeIs(NORMAL);

        History.redo("Blending Mode Change");
        assertThat(layer).blendingModeIs(DIFFERENCE);

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_setName() {
        assertThat(layer).nameIs("layer 1");

        layer.setName("newName", true);
        assertThat(layer).nameIs("newName").uiNameIs("newName");
        History.assertNumEditsIs(1);

        History.undo("Rename Layer to \"newName\"");
        assertThat(layer).nameIs("layer 1").uiNameIs("layer 1");

        History.redo("Rename Layer to \"newName\"");
        assertThat(layer).nameIs("newName").uiNameIs("newName");

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_makeActive() {
        Layer layer2 = comp.getLayer(1);
        assertThat(layer2).isNotActive();

        layer2.makeActive(true);
        assertThat(layer2).isActive();
        History.assertNumEditsIs(1);

        History.undo("Layer Selection Change");
        assertThat(layer2).isNotActive();

        History.redo("Layer Selection Change");
        assertThat(layer2).isActive();

        iconUpdates.check(0, 0);
    }

    @Test
    public void test_resize() {
        Canvas canvas = layer.getComp().getCanvas();
        Dimension origSize = canvas.getImSize();

        layer.resize(origSize).join();
        layer.resize(new Dimension(30, 25)).join();
        layer.resize(new Dimension(25, 30)).join();
        layer.resize(new Dimension(10, 1)).join();
        layer.resize(new Dimension(1, 10)).join();
        layer.resize(origSize).join();

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
            assertThat(layer).hasNoMask();

            layer.addMask(LayerMaskAddType.REVEAL_ALL);
            assertThat(layer).hasMask();
            History.assertNumEditsIs(1);

            History.undo("Add Layer Mask");
            assertThat(layer).hasNoMask();

            History.redo("Add Layer Mask");
            assertThat(layer).hasMask();

            iconUpdates.check(0, 0);
        }
    }

    @Test
    public void test_deleteMask() {
        if (withMask.isYes()) {
            assertThat(layer).hasMask();

            layer.deleteMask(true);
            assertThat(layer).hasNoMask();
            History.assertNumEditsIs(1);
            iconUpdates.check(0, 0);

            History.undo("Delete Layer Mask");
            assertThat(layer).hasMask();
            iconUpdates.check(0, 1);

            History.redo("Delete Layer Mask");
            assertThat(layer).hasNoMask();
            iconUpdates.check(0, 1);
        }
    }

    @Test
    public void test_setMaskEnabled() {
        if (withMask.isYes()) {
            assertThat(layer)
                    .hasMask()
                    .maskIsEnabled();

            var enableAction = new EnableDisableMaskAction(layer);
            assertThat(enableAction).nameIs("Disable");

            // test disabling the layer mask
            layer.setMaskEnabled(false, true);
            assertThat(layer).maskIsDisabled();
            assertThat(enableAction).nameIs("Enable");
            History.assertNumEditsIs(1);
            iconUpdates.check(0, 1);

            History.undo("Disable Layer Mask");
            assertThat(layer).maskIsEnabled();
            assertThat(enableAction).nameIs("Disable");
            iconUpdates.check(0, 2);

            History.redo("Disable Layer Mask");
            assertThat(layer).maskIsDisabled();
            assertThat(enableAction).nameIs("Enable");
            iconUpdates.check(0, 3);

            // now test enabling the layer mask
            layer.setMaskEnabled(true, true);
            assertThat(layer).maskIsEnabled();
            assertThat(enableAction).nameIs("Disable");
            iconUpdates.check(0, 4);

            History.undo("Enable Layer Mask");
            assertThat(layer).maskIsDisabled();
            assertThat(enableAction).nameIs("Enable");
            iconUpdates.check(0, 5);

            History.redo("Enable Layer Mask");
            assertThat(layer).maskIsEnabled();
            assertThat(enableAction).nameIs("Disable");
            iconUpdates.check(0, 6);
        }
    }

    @Test
    public void testMaskLinking() {
        if (withMask.isYes()) {
            assertThat(layer)
                    .hasMask()
                    .maskIsLinked();
            var linkAction = new LinkUnlinkMaskAction(layer);
            assertThat(linkAction).nameIs("Unlink");

            // test unlinking the layer mask
            layer.getMask().setLinked(false, true);
            assertThat(layer).maskIsNotLinked();
            assertThat(linkAction).nameIs("Link");
            History.assertNumEditsIs(1);

            History.undo("Unlink Layer Mask");
            assertThat(layer).maskIsLinked();
            assertThat(linkAction).nameIs("Unlink");

            History.redo("Unlink Layer Mask");
            assertThat(layer).maskIsNotLinked();
            assertThat(linkAction).nameIs("Link");

            // now test linking back the layer mask
            layer.getMask().setLinked(true, true);
            assertThat(layer).maskIsLinked();
            assertThat(linkAction).nameIs("Unlink");
            History.assertNumEditsIs(2);

            History.undo("Link Layer Mask");
            assertThat(layer).maskIsNotLinked();
            assertThat(linkAction).nameIs("Link");

            History.redo("Link Layer Mask");
            assertThat(layer).maskIsLinked();
            assertThat(linkAction).nameIs("Unlink");

            iconUpdates.check(0, 0);
        }
    }
}
