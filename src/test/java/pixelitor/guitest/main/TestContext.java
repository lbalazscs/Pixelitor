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
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.assertions.PixelitorAssertions;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.EDT;
import pixelitor.guitest.Keyboard;
import pixelitor.guitest.Mouse;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.GradientType;
import pixelitor.utils.Rnd;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static pixelitor.guitest.AppRunner.getCurrentTimeHM;

public final class TestContext {
    private final MaskMode maskMode;
    private final Keyboard keyboard;
    private final Mouse mouse;
    private final FrameFixture pw;
    private final AppRunner app;
    private final TestConfig config;

    public TestContext(MaskMode maskMode,
                       AppRunner app,
                       TestConfig config) {
        this.maskMode = maskMode;
        this.app = app;
        this.config = config;

        this.pw = app.getPW();
        this.keyboard = app.getKeyboard();
        this.mouse = app.getMouse();
    }

    public void log(int indent, String msg) {
        for (int i = 0; i < indent; i++) {
            System.out.print("    ");
        }
        String fullMsg = "%s: %s (%s)".formatted(
            getCurrentTimeHM(), msg, maskMode);
        System.out.println(fullMsg);
        EDT.run(() -> PixelitorWindow.get().setTitle(fullMsg));
    }

    // decide whether some test(s) should be skipped in quick mode
    public boolean skip() {
        return skip(0.1); // only execute 10% of the time
    }

    public boolean skip(double threshold) {
        if (config.isQuick()) {
            return Rnd.nextDouble() > threshold;
        } else {
            return false;
        }
    }

    public void afterTestActions() {
        app.verifyAndClearHistory();
        checkConsistency();
    }

    public void checkConsistency() {
        checkConsistency(0);
    }

    private void checkConsistency(int expectedDialogNesting) {
        GlobalEvents.assertDialogNestingIs(expectedDialogNesting);

        Layer layer = EDT.getActiveLayer();
        if (layer == null) { // no open image
            return;
        }

        maskMode.check();
    }

    public void testColorPaletteDialog(String dialogTitle) {
        var dialog = app.findDialogByTitle(dialogTitle);
        if (dialogTitle.contains("Foreground")) {
            dialog.resizeTo(new Dimension(500, 500));
        } else {
            dialog.resizeTo(new Dimension(700, 500));
        }
        dialog.close();
        dialog.requireNotVisible();
    }

    public void testRotateFlip(boolean entireComp) {
        String prefix = entireComp ? "comp" : "layer";
        int indent = entireComp ? 2 : 1;
        log(indent, prefix + " rotate and flip");

        app.runMenuCommandByName(prefix + "_rot_90");
        keyboard.undoRedoUndo("Rotate 90° CW");

        app.runMenuCommandByName(prefix + "_rot_180");
        keyboard.undoRedoUndo("Rotate 180°");

        app.runMenuCommandByName(prefix + "_rot_270");
        keyboard.undoRedoUndo("Rotate 90° CCW");

        app.runMenuCommandByName(prefix + "_flip_hor");
        keyboard.undoRedoUndo("Flip Horizontal");

        app.runMenuCommandByName(prefix + "_flip_ver");
        keyboard.undoRedoUndo("Flip Vertical");
    }

    public void addLayerMask(boolean allowExistingMask) {
        if (EDT.activeLayerHasMask()) {
            pw.button("addLayerMask").requireDisabled();
            if (!allowExistingMask) {
                throw new IllegalStateException("already has mask");
            }
        } else {
            app.addLayerMask();
            app.drawGradientFromCenter(GradientType.RADIAL);
        }
    }

    public void openFileWithDialog(File inputDir, String fileName) {
        app.openFileWithDialog("Open...", inputDir, fileName);
        maskMode.apply(this);
    }


    public void clickAndResetRectSelectTool() {
        app.clickTool(Tools.RECTANGLE_SELECTION);
        pw.comboBox("combinatorCB").selectItem("Replace");
    }

    public void testCropSelection(Runnable cropTask,
                                  boolean assumeNonRectangular,
                                  double expectedSelWidth,
                                  double expectedSelHeight) {
        EDT.requireSelection();

        Selection selection = EDT.getActiveSelection();
        boolean rectangular = selection.getShape() instanceof Rectangle2D;
        assert rectangular == !assumeNonRectangular;

        Rectangle2D selectionBounds = selection.getShapeBounds2D();
        double selWidth = selectionBounds.getWidth();
        double selHeight = selectionBounds.getHeight();

        // the values can be off by one due to rounding errors
        assertThat(selWidth).isCloseTo(expectedSelWidth, within(2.0));
        assertThat(selHeight).isCloseTo(expectedSelHeight, within(2.0));

        Canvas canvas = EDT.queryActiveComp(Composition::getCanvas);
        int origCanvasWidth = canvas.getWidth();
        int origCanvasHeight = canvas.getHeight();

        cropTask.run();

        if (rectangular) {
            undoRedoUndoSimpleSelectionCrop(origCanvasWidth, origCanvasHeight, selWidth, selHeight);
            return;
        }

        // not rectangular: test choosing "Only Crop"
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Only Crop").click();
        undoRedoUndoSimpleSelectionCrop(origCanvasWidth, origCanvasHeight, selWidth, selHeight);

        if (skip(0.5)) {
            return;
        }

        // not rectangular: test choosing "Only Hide"
        cropTask.run();
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Only Hide").click();

        EDT.requireNoSelection();
        EDT.assertCanvasSizeIs(origCanvasWidth, origCanvasHeight);
        EDT.assertActiveLayerHasMask();

        keyboard.undoRedoUndo("Add Hiding Mask");

        if (skip(0.5)) {
            return;
        }

        // not rectangular: test choosing "Crop and Hide"
        cropTask.run();
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Crop and Hide").click();
        checkAfterSelectionCrop(selWidth, selHeight);
        EDT.assertActiveLayerHasMask();

        keyboard.undoRedoUndo("Crop and Hide");

        if (skip(0.5)) {
            return;
        }

        // not rectangular: test choosing "Cancel"
        cropTask.run();
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Cancel").click();

        EDT.requireSelection();
        EDT.assertCanvasSizeIs(origCanvasWidth, origCanvasHeight);
    }

    private void undoRedoUndoSimpleSelectionCrop(
        int origCanvasWidth, int origCanvasHeight,
        double selWidth, double selHeight) {
        checkAfterSelectionCrop(selWidth, selHeight);

        keyboard.undo("Crop");
        checkAfterSelectionCropUndone(origCanvasWidth, origCanvasHeight);

        keyboard.redo("Crop");
        checkAfterSelectionCrop(selWidth, selHeight);

        keyboard.undo("Crop");
        checkAfterSelectionCropUndone(origCanvasWidth, origCanvasHeight);
    }

    private static void checkAfterSelectionCrop(double selWidth, double selHeight) {
        EDT.assertCanvasSizeIs((int) (selWidth + 0.5), (int) (selHeight + 0.5));
        EDT.requireNoSelection();
    }

    private static void checkAfterSelectionCropUndone(int origCanvasWidth, int origCanvasHeight) {
        PixelitorAssertions.assertThat(EDT.getActiveSelection())
            .isNotNull()
            .isValid()
            .isMarching();
        EDT.assertCanvasSizeIs(origCanvasWidth, origCanvasHeight);
    }

    public void closeOneOfTwoViews() {
        log(1, "testing close one of two views");

        int numOpenImages = EDT.call(Views::getNumViews);
        if (numOpenImages == 1) {
            app.createNewImage(400, 400, null);
        }

        EDT.assertNumViewsIs(2);

        app.closeCurrentView(AppRunner.ExpectConfirmation.UNKNOWN);

        EDT.assertNumViewsIs(1);

        maskMode.apply(this);
        afterTestActions();
    }

    public MaskMode maskMode() {
        return maskMode;
    }

    public Keyboard keyboard() {
        return keyboard;
    }

    public Mouse mouse() {
        return mouse;
    }

    public FrameFixture pw() {
        return pw;
    }

    public AppRunner app() {
        return app;
    }

    public TestConfig config() {
        return config;
    }
}
