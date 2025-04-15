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
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.LayerMaskActions.EnableDisableMaskAction;
import pixelitor.layers.LayerMaskActions.LinkUnlinkMaskAction;
import pixelitor.testutils.WithMask;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertSame;
import static pixelitor.TestHelper.createEmptyImageLayer;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.layers.BlendingMode.DIFFERENCE;
import static pixelitor.layers.BlendingMode.NORMAL;

/**
 * Tests the functionality common to all Layer subclasses.
 */
@RunWith(Parameterized.class)
public class LayerTest {
    private Composition comp;
    private Layer layer;  // the primary tested layer
    private ImageLayer layer2; // a secondary layer for context

    @Parameter
    public Class<? extends Layer> layerClass;

    @Parameter(value = 1)
    public WithMask withMask;

    private IconUpdateChecker iconChecker;

    @Parameters(name = "{index}: {0}, mask = {1}")
    public static Collection<Object[]> instancesToTest() {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        // define the combinations of layer types and mask presence to test
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
        layer = TestHelper.createLayer(layerClass, comp);

        // Add the layer to the new composition.
        // For smart filters their owning smart object will be added.
        comp.addLayerWithoutUI(layer.getTopLevelLayer());

        layer2 = createEmptyImageLayer(comp, "LayerTest layer 2");
        comp.addLayerWithoutUI(layer2);

        withMask.configure(layer);
        LayerMask mask = layer.getMask();

        iconChecker = new IconUpdateChecker(layer, mask);

        comp.setActiveLayer(layer, false, null);

        assertThat(comp)
            .numLayersIs(2)
            .activeLayerIs(layer)
            .invariantsAreOK();

        History.clear();
    }

    @Test
    public void hidingAndShowing() {
        checkShown(layer);

        // hide the layer
        layer.setVisible(false, true, true);
        checkHidden(layer);

        String hideEditName = "Hide " + layer.getTypeString();
        History.undo(hideEditName);
        checkShown(layer);

        History.redo(hideEditName);
        checkHidden(layer);

        // show hidden layer again
        layer.setVisible(true, true, true);
        checkShown(layer);

        String showEditName = "Show " + layer.getTypeString();
        History.undo(showEditName);
        checkHidden(layer);

        History.redo(showEditName);
        checkShown(layer);

        // final verification
        History.assertNumEditsIs(2);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void isolating() {
        checkShown(layer);
        checkShown(layer2);

        layer.isolate(); // isolate the primary layer

        checkShown(layer);
        checkHidden(layer2); // other layers should be hidden
        History.assertNumEditsIs(1);

        layer.isolate(); // toggle isolation off, this is an undo

        checkShown(layer);
        checkShown(layer2);
        History.assertNumEditsIs(1);

        History.redo("Isolate");
        checkShown(layer);
        checkHidden(layer2);
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
            .isInstanceOf(layer.getClass())
            .uiIsVisible()
            .hasMask(withMask.isTrue());
    }

    @Test
    public void opacity() {
        float oldValue = 1.0f;
        float newValue = 0.7f;
        assertThat(layer).opacityIs(oldValue);

        // change the opacity of the layer
        layer.setOpacity(newValue, true, true);
        assertThat(layer).opacityIs(newValue);

        PixelitorEdit lastEdit = History.getLastEdit();
        assertThat(lastEdit)
            .isInstanceOf(LayerOpacityEdit.class)
            .nameIs("Layer Opacity Change");
        var opacityEdit = (LayerOpacityEdit) lastEdit;
        assertSame(layer, opacityEdit.getLayer());

        History.undo("Layer Opacity Change");
        assertThat(layer).opacityIs(oldValue);

        History.redo("Layer Opacity Change");
        assertThat(layer).opacityIs(newValue);

        History.assertNumEditsIs(1);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void blendingMode() {
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
        String initialName = "layer 1";
        String newName = "newName";
        String editName = "Rename Layer to \"" + newName + "\"";
        assertThat(layer).nameIs(initialName);

        // rename the layer
        layer.setName(newName, true);
        checkName(newName);

        History.undo(editName);
        checkName(initialName);

        History.redo(editName);
        checkName(newName);

        History.assertNumEditsIs(1);
        iconChecker.verifyUpdateCounts(0, 0); // Renaming shouldn't affect icons
    }

    private void checkName(String newName) {
        assertThat(layer).nameIs(newName).uiNameIs(newName);
    }

    @Test
    public void activating() {
        assertThat(layer).isActive();
        assertThat(layer2).isNotActive();

        layer2.activate(); // doesn't add history
        assertThat(layer2).isActive();
        layer.activate();
        assertThat(layer).isActive();
        History.assertNumEditsIs(0);

        // now with history
        comp.setActiveLayer(layer2, true, "Layer Selection Change");
        assertThat(layer2).isActive();
        History.assertNumEditsIs(1);

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

        History.undo("Layer Reordering");
        assertThat(comp.indexOf(topLevelLayer)).isEqualTo(0);

        History.redo("Layer Reordering");
        assertThat(comp.indexOf(topLevelLayer)).isEqualTo(1);

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void addMask() {
        if (withMask.isTrue()) {
            return; // only test adding if no mask exists initially
        }

        // set up a dummy selection for mask types that require it
        TestHelper.setSelection(comp, new Rectangle(1, 1, 2, 2));
        for (LayerMaskAddType addType : LayerMaskAddType.values()) {
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

            // undo again to reset state for the next mask type in the loop
            History.undo(expectedEditName);
            assertThat(layer).hasNoMask();

            iconChecker.verifyUpdateCounts(0, 0);
        }
    }

    @Test
    public void deleteMask() {
        if (withMask.isFalse()) {
            return;  // only test deleting if a mask exists
        }
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

    @Test
    public void maskEnableToggle() {
        if (withMask.isFalse()) {
            return; // only test if a mask exists
        }
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

    private void checkMaskIsEnabled(EnableDisableMaskAction action) {
        assertThat(layer).hasMask().maskIsEnabled();
        assertThat(action).textIs("Disable"); // the next possible action
    }

    private void checkMaskIsDisabled(EnableDisableMaskAction action) {
        assertThat(layer).hasMask().maskIsDisabled();
        assertThat(action).textIs("Enable"); // the next possible action
    }

    @Test
    public void maskLinking() {
        if (withMask.isFalse()) {
            return; // only test if a mask exists
        }
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

    private void checkMaskIsLinked(LinkUnlinkMaskAction linkAction) {
        assertThat(layer).maskIsLinked();
        assertThat(linkAction).textIs("Unlink"); // the next possible action
    }

    private void checkMaskIsNotLinked(LinkUnlinkMaskAction linkAction) {
        assertThat(layer).hasMask().maskIsNotLinked();
        assertThat(linkAction).textIs("Link"); // the next possible action
    }

    @Test
    public void maskEditing() {
        if (withMask.isFalse()) {
            return; // only test if a mask exists
        }

        assertThat(layer).isNotMaskEditing();

        layer.setMaskEditing(true);
        assertThat(layer).isMaskEditing();

        layer.setMaskEditing(false);
        assertThat(layer).isNotMaskEditing();

        iconChecker.verifyUpdateCounts(0, 0);
        History.assertNumEditsIs(0);
    }
}
