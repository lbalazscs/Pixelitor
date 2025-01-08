/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition;
import pixelitor.CopyType;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.history.LayerOpacityEdit;
import pixelitor.layers.LayerMaskActions.EnableDisableMaskAction;
import pixelitor.layers.LayerMaskActions.LinkUnlinkMaskAction;
import pixelitor.testutils.WithMask;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static pixelitor.TestHelper.createEmptyImageLayer;
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

    private IconUpdateChecker iconChecker;
    private ImageLayer layer2;

    @Parameters(name = "{index}: {0}, mask = {1}")
    public static Collection<Object[]> instancesToTest() {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        return Arrays.asList(new Object[][]{
            {ImageLayer.class, WithMask.NO},
            {ImageLayer.class, WithMask.YES},
            {TextLayer.class, WithMask.NO},
            {TextLayer.class, WithMask.YES},
            {ShapesLayer.class, WithMask.NO},
            {ShapesLayer.class, WithMask.YES},
            {GradientFillLayer.class, WithMask.NO},
            {GradientFillLayer.class, WithMask.YES},
            {ColorFillLayer.class, WithMask.NO},
            {ColorFillLayer.class, WithMask.YES},
            {SmartObject.class, WithMask.NO},
            {SmartObject.class, WithMask.YES},
            {AdjustmentLayer.class, WithMask.NO},
            {AdjustmentLayer.class, WithMask.YES},
            {SmartFilter.class, WithMask.NO},
            {SmartFilter.class, WithMask.YES},
        });
    }

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Before
    public void beforeEachTest() {
        comp = TestHelper.createEmptyComp("LayerTest");
        // make sure each test runs with a fresh Layer
        layer = TestHelper.createLayer(layerClass, comp);

        // for smart filters add their smart object
        comp.addLayerWithoutUI(layer.getTopLevelLayer());

        layer2 = createEmptyImageLayer(comp, "LayerTest layer 2");
        comp.addLayerWithoutUI(layer2);

        withMask.configure(layer);
        LayerMask mask = null;
        if (withMask.isTrue()) {
            mask = layer.getMask();
        }

        iconChecker = new IconUpdateChecker(layer, mask);

        comp.setActiveLayer(layer, true, null);

        assert comp.getNumLayers() == 2 : "found " + comp.getNumLayers() + " layers";

        History.clear();
    }

    @Test
    public void hidingAndShowing() {
        checkShown(layer);

        // hide the previously shown layer
        layer.setVisible(false, true, true);
        checkHidden(layer);

        String expectedEditName = "Hide " + layer.getTypeString();
        History.undo(expectedEditName);
        checkShown(layer);

        History.redo(expectedEditName);
        checkHidden(layer);

        // show the previously hidden layer
        layer.setVisible(true, true, true);
        checkShown(layer);

        expectedEditName = "Show " + layer.getTypeString();
        History.undo(expectedEditName);
        checkHidden(layer);

        History.redo(expectedEditName);
        checkShown(layer);

        History.assertNumEditsIs(2);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void isolating() {
        checkShown(layer);
        checkShown(layer2);

        layer.isolate();

        checkShown(layer);
        checkHidden(layer2);

        layer.isolate();

        checkShown(layer);
        checkShown(layer2);
    }

    private static void checkShown(Layer layer) {
        assertThat(layer)
            .isVisible()
            .uiIsVisible();
    }

    private static void checkHidden(Layer layer) {
        assertThat(layer)
            .isNotVisible()
            .uiIsNotVisible();
    }

    @Test
    public void duplicating() {
        Layer copy = layer.copy(CopyType.DUPLICATE_LAYER, true, comp);
        checkCopy(copy, "layer 1 copy");

        Layer copy2 = copy.copy(CopyType.DUPLICATE_LAYER, true, comp);
        checkCopy(copy2, "layer 1 copy 2");

        Layer copy3 = copy2.copy(CopyType.DUPLICATE_LAYER, true, comp);
        checkCopy(copy3, "layer 1 copy 3");

        // in this case the name shouldn't change
        Layer exactCopy = layer.copy(CopyType.UNDO, true, comp);
        checkCopy(exactCopy, "layer 1");

        iconChecker.verifyUpdateCounts(0, 0);
    }

    private void checkCopy(Layer copy, String expectedName) {
        copy.createUI();
        assertThat(copy)
            .nameIs(expectedName)
            .classIs(layer.getClass())
            .uiIsVisible();

        assertEquals(copy.hasMask(), withMask.isTrue());
    }

    @Test
    public void changingTheOpacity() {
        float oldValue = 1.0f;
        float newValue = 0.7f;
        assertThat(layer).opacityIs(oldValue);

        // change the opacity of the layer
        layer.setOpacity(newValue, true, true);
        assertThat(layer).opacityIs(newValue);
        var lastEdit = (LayerOpacityEdit) History.getLastEdit();
        assertSame(layer, lastEdit.getLayer());

        History.undo("Layer Opacity Change");
        assertThat(layer).opacityIs(oldValue);

        History.redo("Layer Opacity Change");
        assertThat(layer).opacityIs(newValue);

        History.assertNumEditsIs(1);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void changingTheBlendingMode() {
        assertThat(layer).blendingModeIs(NORMAL);

        layer.setBlendingMode(DIFFERENCE, true, true);
        assertThat(layer).blendingModeIs(DIFFERENCE);

        History.undo("Blending Mode Change");
        assertThat(layer).blendingModeIs(NORMAL);

        History.redo("Blending Mode Change");
        assertThat(layer).blendingModeIs(DIFFERENCE);

        History.assertNumEditsIs(1);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void renaming() {
        assertThat(layer).nameIs("layer 1");

        layer.setName("newName", true);
        assertThat(layer).nameIs("newName").uiNameIs("newName");

        History.undo("Rename Layer to \"newName\"");
        assertThat(layer).nameIs("layer 1").uiNameIs("layer 1");

        History.redo("Rename Layer to \"newName\"");
        assertThat(layer).nameIs("newName").uiNameIs("newName");

        History.assertNumEditsIs(1);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void activating() {
        assertThat(layer2).isNotActive();

        layer2.activate(); // doesn't add history
        assertThat(layer2).isActive();
        layer.activate();
        assertThat(layer).isActive();
        assertThat(History.getLastEdit()).isNull();

        // now try with history
        comp.setActiveLayer(layer2, true, "Layer Selection Change");
        assertThat(layer2).isActive();

        History.undo("Layer Selection Change");
        assertThat(layer2).isNotActive();

        History.redo("Layer Selection Change");
        assertThat(layer2).isActive();

        History.assertNumEditsIs(1);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void resizing() {
        Dimension origSize = layer.getComp().getCanvas().getSize();

        // there is not much to assert, since normally
        // resizing is done at the Composition level
        layer.resize(origSize).join();
        layer.resize(new Dimension(30, 25)).join();
        layer.resize(new Dimension(25, 30)).join();
        layer.resize(new Dimension(10, 1)).join();
        layer.resize(new Dimension(1, 10)).join();
        layer.resize(origSize).join();

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void changeStackIndex() {
        Layer topLevelLayer = layer.getTopLevelLayer();
        assertThat(comp.indexOf(topLevelLayer)).isEqualTo(0);
        topLevelLayer.changeStackIndex(1);
        assertThat(comp.indexOf(topLevelLayer)).isEqualTo(1);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void addingAMask() {
        if (withMask.isFalse()) {
            TestHelper.setSelection(comp, new Rectangle(1, 1, 2, 2));
            LayerMaskAddType[] addTypes = LayerMaskAddType.values();
            for (LayerMaskAddType addType : addTypes) {
                assertThat(layer).hasNoMask();

                layer.addMask(addType);
                assertThat(layer).hasMask();
                History.assertNumEditsIs(1);

                String expectedEditName = addType.needsSelection() ?
                    "Layer Mask from Selection" : "Add Layer Mask";
                History.undo(expectedEditName);
                assertThat(layer).hasNoMask();

                History.redo(expectedEditName);
                assertThat(layer).hasMask();

                // undo again so that it has no mask in the next round
                History.undo(expectedEditName);
                assertThat(layer).hasNoMask();

                iconChecker.verifyUpdateCounts(0, 0);
            }
        }
    }

    @Test
    public void deletingTheMask() {
        if (withMask.isTrue()) {
            assertThat(layer).hasMask();

            layer.deleteMask(true);
            assertThat(layer).hasNoMask();
            History.assertNumEditsIs(1);
            iconChecker.verifyUpdateCounts(0, 0);

            History.undo("Delete Layer Mask");
            assertThat(layer).hasMask();
            iconChecker.verifyUpdateCounts(0, 0);

            History.redo("Delete Layer Mask");
            assertThat(layer).hasNoMask();
            iconChecker.verifyUpdateCounts(0, 0);
        }
    }

    @Test
    public void maskEnablingAndDisabling() {
        if (withMask.isTrue()) {
            var action = new EnableDisableMaskAction(layer);
            checkMaskIsEnabled(action);

            // disable the layer mask
            layer.setMaskEnabled(false, true);
            checkMaskIsDisabled(action);
            History.assertNumEditsIs(1);
            iconChecker.verifyUpdateCounts(0, 1);

            History.undo("Disable Layer Mask");
            checkMaskIsEnabled(action);
            iconChecker.verifyUpdateCounts(0, 2);

            History.redo("Disable Layer Mask");
            checkMaskIsDisabled(action);
            iconChecker.verifyUpdateCounts(0, 3);

            // now test enabling the layer mask
            layer.setMaskEnabled(true, true);
            checkMaskIsEnabled(action);
            iconChecker.verifyUpdateCounts(0, 4);

            History.undo("Enable Layer Mask");
            checkMaskIsDisabled(action);
            iconChecker.verifyUpdateCounts(0, 5);

            History.redo("Enable Layer Mask");
            checkMaskIsEnabled(action);
            iconChecker.verifyUpdateCounts(0, 6);
        }
    }

    private void checkMaskIsEnabled(EnableDisableMaskAction action) {
        assertThat(layer).hasMask().maskIsEnabled();
        assertThat(action).textIs("Disable");
    }

    private void checkMaskIsDisabled(EnableDisableMaskAction action) {
        assertThat(layer).hasMask().maskIsDisabled();
        assertThat(action).textIs("Enable");
    }

    @Test
    public void maskLinking() {
        if (withMask.isTrue()) {
            var linkAction = new LinkUnlinkMaskAction(layer);
            checkMaskIsLinked(linkAction);

            // unlink the layer mask
            layer.getMask().setLinked(false, true);
            checkMaskIsNotLinked(linkAction);

            History.undo("Unlink Layer Mask");
            checkMaskIsLinked(linkAction);

            History.redo("Unlink Layer Mask");
            checkMaskIsNotLinked(linkAction);

            // now link back the layer mask
            layer.getMask().setLinked(true, true);
            checkMaskIsLinked(linkAction);

            History.undo("Link Layer Mask");
            checkMaskIsNotLinked(linkAction);

            History.redo("Link Layer Mask");
            checkMaskIsLinked(linkAction);

            History.assertNumEditsIs(2);
            iconChecker.verifyUpdateCounts(0, 0);
        }
    }

    private void checkMaskIsLinked(LinkUnlinkMaskAction linkAction) {
        assertThat(layer).maskIsLinked();
        assertThat(linkAction).textIs("Unlink");
    }

    private void checkMaskIsNotLinked(LinkUnlinkMaskAction linkAction) {
        assertThat(layer).hasMask().maskIsNotLinked();
        assertThat(linkAction).textIs("Link");
    }
}
