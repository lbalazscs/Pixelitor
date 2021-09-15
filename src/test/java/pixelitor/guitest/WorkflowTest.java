/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest;

import org.assertj.core.util.DoubleComparator;
import org.assertj.swing.data.Index;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import pixelitor.Composition;
import pixelitor.gui.GUIText;
import pixelitor.gui.ImageArea;
import pixelitor.gui.TabsUI;
import pixelitor.guitest.AppRunner.Randomize;
import pixelitor.guitest.AppRunner.Reseed;
import pixelitor.guitest.AppRunner.ShowOriginal;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.TextLayer;
import pixelitor.tools.BrushType;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.shapes.TwoPointPaintType;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.io.File;
import java.util.function.Consumer;

import static java.awt.event.KeyEvent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.guitest.AJSUtils.findButtonByText;
import static pixelitor.selection.SelectionModifyType.EXPAND;
import static pixelitor.tools.DragToolState.NO_INTERACTION;
import static pixelitor.tools.move.MoveMode.MOVE_SELECTION_ONLY;

/**
 * A workflow test is an Assertj-Swing regression test, where an
 * image is created from scratch using a longer workflow, and then
 * it is visually compared to a reference image saved earlier.
 * It's not a unit test.
 */
public class WorkflowTest {
    private final AppRunner app;
    private final Mouse mouse;
    private final FrameFixture pw;
    private final Keyboard keyboard;

    private static final int INITIAL_WIDTH = 700;
    private static final int INITIAL_HEIGHT = 500;
    private static final int EXTRA_HEIGHT = 20;
    private static final int EXTRA_WIDTH = 50;

    public static void main(String[] args) {
        Utils.makeSureAssertionsAreEnabled();
        FailOnThreadViolationRepaintManager.install();

        new WorkflowTest();
    }

    private WorkflowTest() {
        app = new AppRunner(null);
        mouse = app.getMouse();
        pw = app.getPW();
        keyboard = app.getKeyboard();

        wfTest1();
        wfTest2();
    }

    private void wfTest1() {
        app.createNewImage(INITIAL_WIDTH, INITIAL_HEIGHT, "wf test 1");
        addGuide();
        runFilterWithDialog("Wood", null);
        app.addTextLayer("Wood", null, "Pixelitor");

        app.editTextLayer(dialog -> {
            dialog.textBox("textTF").requireText("Wood");
            dialog.slider("fontSize").slideTo(200);
        });

        rasterizeThenUndo();
        selectionFromText();
        deleteTextLayer();
        rotate90();
        invertSelection();
        deselect();
        rotate270();
        drawTransparentZigzagRectangle();
        enlargeCanvas();
        app.addEmptyImageLayer(true);
        renderCaustics();
        selectWoodLayer();
        addHeartShapedHoleToTheWoodLayer();
        runFilterWithDialog("Drop Shadow", null);
        app.mergeDown();
        createEllipseSelection();
        expandSelection();
        selectionToPath();
        flipHorizontal();
        tracePath(BrushType.WOBBLE);
        pathToSelection();
        copySelection();
        moveSelection(-100);
        pasteSelection();
        moveSelection(50);
        selectionToPath();
        app.swapColors();
        tracePath(BrushType.SHAPE);
        flipHorizontal();
        clearGuides();
        selectBrushTool();
        loadReferenceImage("wf1_reference.png");
    }

    private void wfTest2() {
        app.createNewImage(INITIAL_WIDTH, INITIAL_HEIGHT, "wf test 2");
        runFilterWithDialog("Spider Web", null);
        app.runMenuCommand("Duplicate");

        app.addTextLayer("Spider", dialog -> {
            dialog.comboBox("hAlignmentCB").selectItem("Left");
            dialog.comboBox("vAlignmentCB").selectItem("Top");
        }, "Pixelitor");
        app.addTextLayer("Web", dialog -> {
            dialog.comboBox("hAlignmentCB").selectItem("Right");
            dialog.comboBox("vAlignmentCB").selectItem("Top");
        }, "Pixelitor");

        // switch back to the main tab
        pw.tabbedPane().selectTab("wf test 2");
        int mainTabIndex = EDT.call(() -> ((TabsUI) ImageArea.getUI()).getSelectedIndex());

        runFilterWithDialog("Clouds", null);
        app.addEmptyImageLayer(false);
        runFilterWithDialog("Fractal Tree",
            dialog -> dialog.slider("Age (Iterations)").slideTo(14));
        app.changeLayerBlendingMode("Hard Light");
        app.mergeDown();

        app.addGradientFillLayer("CW Spiral");
        app.changeLayerBlendingMode("Multiply");
        app.mergeDown();

        Consumer<DialogFixture> customizer = dialog ->
            dialog.comboBox("Bump Map").selectItem("wf test 2 copy");
        runFilterWithDialog("Bump Map", customizer);

        // close the temporary tab
        pw.tabbedPane().selectTab("wf test 2 copy");
        app.runMenuCommand("Close");
        app.closeDoYouWantToSaveChangesDialog();

        // now the main tab should be the active one
        pw.tabbedPane().requireSelectedTab(Index.atIndex(mainTabIndex));

        app.addColorFillLayer(Color.BLUE);
        app.changeLayerBlendingMode("Hue");
        app.changeLayerOpacity(0.5f);

        app.runMenuCommand("Rasterize Color Fill Layer");
        keyboard.undoRedo("Rasterize Color Fill Layer");

        app.mergeDown();

        app.addTextLayer("TEXT", null, "Pixelitor");
        app.runMenuCommand("Convert to Smart Object");
        keyboard.undoRedo("Convert to Smart Object");

        app.runMenuCommand("Edit Contents");
        app.editTextLayer(dialog -> {
            dialog.textBox("textTF")
                .requireText("TEXT")
                .deleteText()
                .enterText("WARPED TEXT");
        });

        app.addLayerMask();
        app.drawGradient("Radial");
        app.runMenuCommand("Close");

        loadReferenceImage("wf2_reference.pxc");
    }

    private void addGuide() {
        app.runMenuCommand("Add Horizontal Guide...");
        var dialog = app.findDialogByTitle("Add Horizontal Guide");
        dialog.slider("Percent").slideTo(60);
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getGuides().getHorizontals())
            .usingComparatorForType(new DoubleComparator(0.001), Double.class)
            .containsExactly(0.6);
        assertThat(EDT.getGuides().getVerticals()).isEmpty();
    }

    private void runFilterWithDialog(String filterName, Consumer<DialogFixture> customizer) {
        app.runFilterWithDialog(filterName, Randomize.NO, Reseed.NO, ShowOriginal.NO, false, customizer);
        keyboard.undoRedo(filterName);
    }

    private void rasterizeThenUndo() {
        EDT.assertActiveLayerTypeIs(TextLayer.class);
        EDT.assertNumLayersIs(2);

        app.runMenuCommand("Rasterize Text Layer");

        EDT.assertActiveLayerTypeIs(ImageLayer.class);
        EDT.assertNumLayersIs(2);

        keyboard.undoRedoUndo("Rasterize Text Layer");

        EDT.assertActiveLayerTypeIs(TextLayer.class);
        EDT.assertNumLayersIs(2);
    }

    private void selectionFromText() {
        EDT.assertThereIsNoSelection();

        app.runMenuCommand("Selection from Text");
        EDT.assertThereIsSelection();

        keyboard.undo("Create Selection");
        EDT.assertThereIsNoSelection();

        keyboard.redo("Create Selection");
        EDT.assertThereIsSelection();
    }

    private void deleteTextLayer() {
        app.checkNumLayersIs(2);

        pw.button("deleteLayer").click();
        app.checkNumLayersIs(1);

        keyboard.undo("Delete Layer");
        app.checkNumLayersIs(2);

        keyboard.redo("Delete Layer");
        app.checkNumLayersIs(1);
    }

    private void rotate90() {
        app.runMenuCommand("Rotate 90° CW");
        keyboard.undoRedo("Rotate 90° CW");
    }

    private void invertSelection() {
        app.runMenuCommand("Invert");
        keyboard.undoRedo("Invert");
    }

    private void deselect() {
        app.runMenuCommand("Deselect");
        keyboard.undoRedo("Deselect");
    }

    private void rotate270() {
        app.runMenuCommand("Rotate 90° CCW");
        keyboard.undoRedo("Rotate 90° CCW");
    }

    private void drawTransparentZigzagRectangle() {
        app.clickTool(Tools.SHAPES);

        app.runMenuCommand("100%");
        mouse.recalcCanvasBounds();

        pw.comboBox("shapeTypeCB").selectItem(ShapeType.RECTANGLE.toString());
        pw.comboBox("fillPaintCB").selectItem(TwoPointPaintType.NONE.toString());
        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.TRANSPARENT.toString());
        EDT.assertShapesToolStateIs(NO_INTERACTION);
        pw.button("convertToSelection").requireDisabled();

        findButtonByText(pw, "Stroke Settings...")
            .requireEnabled()
            .click();
        var dialog = app.findDialogByTitle("Stroke Settings");
        dialog.slider().slideTo(10);
        dialog.comboBox("strokeType").selectItem(StrokeType.ZIGZAG.toString());
        dialog.button("ok").click();
        dialog.requireNotVisible();

        int margin = 25;
        mouse.moveToCanvas(margin, margin);
        mouse.dragToCanvas(INITIAL_WIDTH - margin, INITIAL_HEIGHT - margin);

        keyboard.undoRedo("Create Shape");
        keyboard.pressEsc();
    }

    private void enlargeCanvas() {
        app.enlargeCanvas(EXTRA_HEIGHT, EXTRA_WIDTH, EXTRA_WIDTH, EXTRA_HEIGHT);
        keyboard.undoRedo("Enlarge Canvas");
    }

    private void renderCaustics() {
        pw.pressKey(VK_F3);

        var searchDialog = app.findDialogByTitle("Filter Search");
        searchDialog.releaseKey(VK_F3);

        searchDialog.textBox()
            .requireEmpty()
            .enterText("caus")
            .pressKey(VK_DOWN);

        searchDialog.list()
            .requireFocused()
            .releaseKey(VK_DOWN)
            .pressKey(VK_ENTER);

        searchDialog.requireNotVisible();

        var filterDialog = app.findFilterDialog();
        filterDialog.releaseKey(VK_ENTER);
        filterDialog.button("ok").click();
        filterDialog.requireNotVisible();

        keyboard.undoRedo("Caustics");
    }

    private void selectWoodLayer() {
        app.runMenuCommand("Raise Layer Selection");
        keyboard.undoRedo("Raise Layer Selection");
    }

    private void addHeartShapedHoleToTheWoodLayer() {
        app.setDefaultColors();
        app.addLayerMask();
        app.clickTool(Tools.SHAPES);

        pw.comboBox("shapeTypeCB").selectItem(ShapeType.HEART.toString());
        pw.comboBox("fillPaintCB").selectItem(TwoPointPaintType.FOREGROUND.toString());
        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.NONE.toString());
        mouse.moveToCanvas(340, 100);
        mouse.dragToCanvas(440, 200);
        keyboard.undoRedo("Create Shape");
        keyboard.pressEsc(); // finalize the shape

        app.runMenuCommand("Delete");
        keyboard.undoRedoUndo("Delete Layer Mask");

        app.runMenuCommand("Apply");
        keyboard.undoRedo("Apply Layer Mask");
    }

    private void createEllipseSelection() {
        app.clickTool(Tools.SELECTION);
        pw.comboBox("typeCB").selectItem("Ellipse");
        pw.button("toPathButton").requireDisabled();

        int canvasWidth = INITIAL_WIDTH + 2 * EXTRA_WIDTH;
        int canvasHeight = INITIAL_HEIGHT + 2 * EXTRA_HEIGHT;
        assertThat(EDT.active(Composition::getCanvasWidth)).isEqualTo(canvasWidth);
        assertThat(EDT.active(Composition::getCanvasHeight)).isEqualTo(canvasHeight);

        mouse.recalcCanvasBounds();
        int margin = 100;
        mouse.moveToCanvas(margin, margin);
        mouse.dragToCanvas(canvasWidth - margin, canvasHeight - margin);
    }

    private void expandSelection() {
        app.runModifySelection(50, EXPAND, 3);
        keyboard.undoRedo("Modify Selection");
    }

    private void selectionToPath() {
        app.clickTool(Tools.SELECTION);
        pw.button("toPathButton")
            .requireEnabled()
            .click();
        Utils.sleep(200, MILLISECONDS);
        EDT.assertActiveToolIs(Tools.PEN);

        keyboard.undoRedo("Convert Selection to Path");
    }

    private void tracePath(BrushType brushType) {
        app.clickTool(Tools.BRUSH);
        pw.comboBox("typeCB").selectItem(brushType.toString());

        app.clickTool(Tools.PEN);
        pw.button("toSelectionButton").requireEnabled();

        pw.button("traceWithBrush")
            .requireEnabled()
            .click();
        keyboard.undoRedo(GUIText.BRUSH);
    }

    private void pathToSelection() {
        pw.button("toSelectionButton")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Convert Path to Selection");
    }

    private void copySelection() {
        app.runMenuCommand("Copy Selection");
    }

    private void moveSelection(int dy) {
        app.clickTool(Tools.MOVE);
        pw.comboBox("modeSelector").selectItem(MOVE_SELECTION_ONLY.toString());
        mouse.moveToCanvas(400, 400);
        mouse.dragToCanvas(400, 400 + dy);
    }

    private void pasteSelection() {
        app.runMenuCommand("Paste Selection");
        var dialog = app.findDialogByTitle("Existing Selection");
        findButtonByText(dialog, "Intersect").click();
    }

    private void flipHorizontal() {
        app.runMenuCommand("Flip Horizontal");
        keyboard.undoRedo("Flip Horizontal");
    }

    private void clearGuides() {
        app.runMenuCommand("Clear Guides");
    }

    private void selectBrushTool() {
        app.clickTool(Tools.BRUSH);
    }

    private void loadReferenceImage(String fileName) {
        File f = new File("C:\\pix_tests\\reference_images\\" + fileName);
        app.openFileWithDialog(f.getParentFile(), f.getName());
    }
}
