/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest.main;

import org.assertj.swing.fixture.FrameFixture;
import pixelitor.Features;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.Keyboard;
import pixelitor.guitest.Mouse;
import pixelitor.history.History;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.gradient.GradientType;
import pixelitor.utils.Rnd;

import static org.junit.Assert.assertFalse;
import static pixelitor.guitest.main.MaskMode.NO_MASK;

public class LayerTests {
    private static boolean maskFromColorRangeTested = false;

    private final Keyboard keyboard;
    private final Mouse mouse;
    private final AppRunner app;
    private final MaskMode maskMode;
    private final FrameFixture pw;

    private final TestContext context;

    public LayerTests(TestContext context) {
        this.context = context;

        this.keyboard = context.keyboard();
        this.mouse = context.mouse();
        this.app = context.app();
        this.maskMode = context.maskMode();
        this.pw = context.pw();
    }

    /**
     * Tests layer-related operations from the layers panel and menus.
     */
    void start() {
        context.log(0, "layers");
        maskMode.apply(context);

        testChangeLayerOpacityAndBM();

        testAddLayer();
        testDeleteLayer();
        testDuplicateLayer();

        testLayerVisibilityChange();

        testLayerOrderChangeFromMenu();
        testActiveLayerChangeFromMenu();
        testLayerToCanvasSize();
        testLayerMenusChangingNumLayers();

        context.testRotateFlip(false);

        testLayerMasks();
        testTextLayers();
        testMaskFromColorRange();

        if (Features.enableExperimental) {
            testAdjLayers();
        }

        context.afterTestActions();
    }

    /**
     * Tests adding a new empty image layer.
     */
    private void testAddLayer() {
        context.log(1, "add layer");

        app.checkLayerNamesAre("layer 1");
        var layer1Button = app.findLayerButton("layer 1");
        layer1Button.requireSelected();

        var addEmptyLayerButton = pw.button("addLayer");

        // add layer
        addEmptyLayerButton.click();

        app.checkLayerNamesAre("layer 1", "layer 2");
        var layer2Button = app.findLayerButton("layer 2");
        layer2Button.requireSelected();

        keyboard.undo("New Empty Layer");
        app.checkLayerNamesAre("layer 1");
        layer1Button.requireSelected();

        keyboard.redo("New Empty Layer");
        app.checkLayerNamesAre("layer 1", "layer 2");
        layer2Button.requireSelected();
        maskMode.apply(context);

        app.drawGradientFromCenter(GradientType.SPIRAL_CW);
    }

    private void testChangeLayerOpacityAndBM() {
        context.log(1, "change layer opacity and blending mode");

        app.changeLayerOpacity(0.75f);
        context.checkConsistency();

        app.changeLayerBlendingMode(Rnd.chooseFrom(BlendingMode.LAYER_MODES));
        context.checkConsistency();
    }

    private void testDeleteLayer() {
        context.log(1, "delete layer");

        var layer1Button = app.findLayerButton("layer 1");
        var layer2Button = app.findLayerButton("layer 2");

        app.checkLayerNamesAre("layer 1", "layer 2");
        layer2Button.requireSelected();

        // delete layer 2
        pw.button("deleteLayer")
            .requireEnabled()
            .click();
        app.checkLayerNamesAre("layer 1");
        layer1Button.requireSelected();

        // undo delete
        keyboard.undo("Delete layer 2");
        app.checkLayerNamesAre("layer 1", "layer 2");
        layer2Button = app.findLayerButton("layer 2");
        layer2Button.requireSelected();

        // redo delete
        keyboard.redo("Delete layer 2");
        app.checkLayerNamesAre("layer 1");
        layer1Button.requireSelected();

        maskMode.apply(context);
    }

    private void testDuplicateLayer() {
        context.log(1, "duplicate layer");

        app.checkLayerNamesAre("layer 1");
        pw.button("duplicateLayer").click();

        app.findLayerButton("layer 1 copy").requireSelected();
        app.checkLayerNamesAre("layer 1", "layer 1 copy");

        keyboard.undo("Duplicate Layer");
        app.checkLayerNamesAre("layer 1");
        app.findLayerButton("layer 1").requireSelected();

        keyboard.redo("Duplicate Layer");
        app.checkLayerNamesAre("layer 1", "layer 1 copy");
        app.findLayerButton("layer 1 copy").requireSelected();

        maskMode.apply(context);
    }

    private void testLayerVisibilityChange() {
        context.log(1, "layer visibility change");

        var layer1CopyButton = app.findLayerButton("layer 1 copy");
        layer1CopyButton.requireOpenEye();

        layer1CopyButton.setOpenEye(false);
        layer1CopyButton.requireClosedEye();

        keyboard.undo("Hide Image Layer");
        layer1CopyButton.requireOpenEye();

        keyboard.redo("Hide Image Layer");
        layer1CopyButton.requireClosedEye();

        keyboard.undo("Hide Image Layer");
        layer1CopyButton.requireOpenEye();
    }

    private void testLayerOrderChangeFromMenu() {
        context.log(1, "layer order change from menu");

        app.runMenuCommand("Lower Layer");
        keyboard.undoRedo("Lower Layer");

        app.runMenuCommand("Raise Layer");
        keyboard.undoRedo("Raise Layer");

        app.runMenuCommand("Layer to Bottom");
        keyboard.undoRedo("Layer to Bottom");

        app.runMenuCommand("Layer to Top");
        keyboard.undoRedo("Layer to Top");
    }

    private void testActiveLayerChangeFromMenu() {
        context.log(1, "active layer change from menu");

        app.runMenuCommand("Lower Layer Selection");
        keyboard.undoRedo("Lower Layer Selection");

        app.runMenuCommand("Raise Layer Selection");
        keyboard.undoRedo("Raise Layer Selection");
    }

    private void testLayerToCanvasSize() {
        context.log(1, "layer to canvas size");

        // add a translation to make it a big layer,
        // otherwise "layer to canvas size" has no effect
        app.addTranslation();

        app.runMenuCommand("Layer to Canvas Size");
        keyboard.undoRedo("Layer to Canvas Size");
    }

    private void testLayerMenusChangingNumLayers() {
        context.log(1, "layer menus changing the number of layers");

        app.runMenuCommand("New from Visible");
        keyboard.undoRedo("New Layer from Visible");
        maskMode.apply(context);

        app.mergeDown();

        app.duplicateLayer(ImageLayer.class);
        maskMode.apply(context);

        app.runMenuCommand("New Layer");
        keyboard.undoRedo("New Empty Layer");
        maskMode.apply(context);

        app.runMenuCommand("Delete Layer");
        keyboard.undoRedo("Delete layer 3");
        maskMode.apply(context);

        app.runMenuCommand("Flatten Image");
        assertFalse(History.canUndo());
        maskMode.apply(context);
    }

    private void testLayerMasks() {
        context.log(1, "layer masks");

        boolean allowExistingMask = maskMode != NO_MASK;
        context.addLayerMask(allowExistingMask);

        testLayerMaskIconPopupMenus();

        app.deleteLayerMask();

        maskMode.apply(context);

        context.checkConsistency();
    }

    private void testLayerMaskIconPopupMenus() {
        // test simple undoable actions
        testUndoablePopupAction("Delete", "Delete Layer Mask");
        testUndoablePopupAction("Apply", "Apply Layer Mask");

        // test toggleable actions
        testToggleablePopupAction("Disable", "Disable Layer Mask", "Enable", "Enable Layer Mask");
        testToggleablePopupAction("Unlink", "Unlink Layer Mask", "Link", "Link Layer Mask");
    }

    /**
     * Tests a simple, non-toggleable action from the layer mask popup menu.
     */
    private void testUndoablePopupAction(String action, String historyName) {
        var popupMenu = pw.label("maskIcon").showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, action);
        keyboard.undoRedoUndo(historyName);
    }

    /**
     * Tests a pair of toggleable actions from the layer mask popup menu.
     */
    private void testToggleablePopupAction(String action1, String history1, String action2, String history2) {
        // test the first action (e.g., Disable) and its undo/redo
        var popupMenu = pw.label("maskIcon").showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, action1);
        keyboard.undoRedo(history1);

        // test the second action (e.g., Enable) and its undo/redo
        popupMenu = pw.label("maskIcon").showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, action2);
        keyboard.undoRedo(history2);
    }

    private void testMaskFromColorRange() {
        if (maskFromColorRangeTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        context.log(1, "mask from color range");

        app.runMenuCommand("Mask from Color Range...");

        var dialog = app.findDialogByTitle("Mask from Color Range");

        mouse.moveTo(dialog, 100, 100);
        mouse.click();

        dialog.slider("toleranceSlider").slideTo(20);
        dialog.slider("softnessSlider").slideTo(20);
        dialog.checkBox("invertMaskCheckBox").check();
        dialog.comboBox("distMetricCombo").selectItem("RGB");

        dialog.button("ok").click();
        dialog.requireNotVisible();
        keyboard.undoRedo("Mask from Color Range");

        if (maskMode == NO_MASK) {
            app.deleteLayerMask();
        }
        maskMode.apply(context);

        maskFromColorRangeTested = true;
    }

    private void testTextLayers() {
        context.log(1, "text layers");

        context.checkConsistency();

        String text = "some text";
        app.addTextLayer(text, null, "Pixelitor");
        maskMode.apply(context);

        app.editTextLayer(dialog -> app.testTextDialog(dialog, text));

        context.checkConsistency();

        app.runMenuCommand("Rasterize Text Layer");
        keyboard.undoRedoUndo("Rasterize Text Layer");

        context.checkConsistency();

        app.mergeDown();

        maskMode.apply(context);
        context.checkConsistency();
    }

    private void testAdjLayers() {
        context.log(1, "adj. layers");

        addAdjustmentLayer();

        context.checkConsistency();
    }

    private void addAdjustmentLayer() {
        pw.button("addAdjLayer").click();
    }
}
