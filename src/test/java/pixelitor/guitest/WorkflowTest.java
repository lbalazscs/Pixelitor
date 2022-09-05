/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.filters.Starburst;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.gui.ImageArea;
import pixelitor.gui.TabsUI;
import pixelitor.guitest.AppRunner.Randomize;
import pixelitor.guitest.AppRunner.Reseed;
import pixelitor.guitest.AppRunner.ShowOriginal;
import pixelitor.layers.*;
import pixelitor.tools.BrushType;
import pixelitor.tools.Tools;
import pixelitor.tools.move.MoveMode;
import pixelitor.tools.shapes.StrokeType;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import static java.awt.event.KeyEvent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.guitest.AJSUtils.findButtonByText;
import static pixelitor.guitest.AppRunner.clickPopupMenu;
import static pixelitor.layers.MaskViewMode.*;
import static pixelitor.selection.SelectionModifyType.EXPAND;
import static pixelitor.tools.DragToolState.NO_INTERACTION;
import static pixelitor.tools.gradient.GradientColorType.FG_TO_BG;
import static pixelitor.tools.gradient.GradientType.*;
import static pixelitor.tools.move.MoveMode.MOVE_LAYER_ONLY;
import static pixelitor.tools.move.MoveMode.MOVE_SELECTION_ONLY;
import static pixelitor.tools.shapes.ShapeType.*;
import static pixelitor.tools.shapes.TwoPointPaintType.*;

/**
 * A workflow test is an Assertj-Swing regression test, where an
 * image is created from scratch using a longer workflow, and then
 * it is visually compared to a reference image saved earlier.
 * It's not a unit test.
 * <p>
 * Assertj-Swing requires using the following VM option:
 * --add-opens java.base/java.util=ALL-UNNAMED
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

    private static File referenceImagesDir;

    /**
     * Enables running all tests within a group
     */
    enum WithinGroup {
        NO_GROUP("") {
            @Override
            public void group(AppRunner app) {
                // do nothing
            }
        }, PASS_THROUGH("P") {
            @Override
            public void group(AppRunner app) {
                app.runMenuCommand("Convert Visible to Group");
                app.selectActiveLayer("layer 1");
            }
        }, ISOLATED("I") {
            @Override
            public void group(AppRunner app) {
                app.runMenuCommand("Convert Visible to Group");
                app.changeLayerBlendingMode(BlendingMode.NORMAL);
                app.selectActiveLayer("layer 1");
            }
        }, DOUBLE_PP("PP") {
            @Override
            public void group(AppRunner app) {
                // two nested pass-through groups
                app.runMenuCommand("Convert Visible to Group");
                app.runMenuCommand("Convert Visible to Group");
                app.selectActiveLayer("layer 1");
            }
        }, DOUBLE_II("II") {
            @Override
            public void group(AppRunner app) {
                // two nested isolated groups
                app.runMenuCommand("Convert Visible to Group");
                app.changeLayerBlendingMode(BlendingMode.NORMAL);
                app.runMenuCommand("Convert Visible to Group");
                app.changeLayerBlendingMode(BlendingMode.NORMAL);
                app.selectActiveLayer("layer 1");
            }
        }, DOUBLE_PI("PI") {
            // inner pass-through, outer isolated
            @Override
            public void group(AppRunner app) {
                app.runMenuCommand("Convert Visible to Group");
                app.runMenuCommand("Convert Visible to Group");
                app.changeLayerBlendingMode(BlendingMode.NORMAL);
                app.selectActiveLayer("layer 1");
            }
        }, DOUBLE_IP("IP") {
            // inner isolated, outer pass-through
            @Override
            public void group(AppRunner app) {
                app.runMenuCommand("Convert Visible to Group");
                app.changeLayerBlendingMode(BlendingMode.NORMAL);
                app.runMenuCommand("Convert Visible to Group");
                app.selectActiveLayer("layer 1");
            }
        };

        private final String nameSuffix;

        WithinGroup(String nameSuffix) {
            this.nameSuffix = nameSuffix;
        }

        public abstract void group(AppRunner app);

        public String getNameSuffix() {
            return nameSuffix;
        }
    }

    public static void main(String[] args) {
        System.out.println("WorkflowTest: started at " + AppRunner.getCurrentTimeHM());
        Utils.makeSureAssertionsAreEnabled();
        FailOnThreadViolationRepaintManager.install();

        assert args.length == 2;
        referenceImagesDir = new File(args[0]);
        assert referenceImagesDir.exists();

        new WorkflowTest(args[1]);
    }

    private WorkflowTest(String arg) {
        boolean experimentalWasEnabled = EDT.call(() -> AppContext.enableExperimentalFeatures);
        // enable it before building the menus so that shortcuts work
        EDT.run(() -> AppContext.enableExperimentalFeatures = true);

        app = new AppRunner(null);
        mouse = app.getMouse();
        pw = app.getPW();
        keyboard = app.getKeyboard();
//        app.runSlowly();

        List<WithinGroup> groupSettings = List.of(
            WithinGroup.values()
//            WithinGroup.NO_GROUP,
//            WithinGroup.PASS_THROUGH,
//            WithinGroup.ISOLATED
        );

        switch (arg) {
            case "all" -> {
                groupSettings.forEach(this::wfTest1);
                groupSettings.forEach(this::wfTest2);
                groupSettings.forEach(this::wfTest3);
                groupSettings.forEach(this::wfTest4);
                groupSettings.forEach(this::wfTest5);
            }
            case "1" -> groupSettings.forEach(this::wfTest1);
            case "2" -> groupSettings.forEach(this::wfTest2);
            case "3" -> groupSettings.forEach(this::wfTest3);
            case "4" -> groupSettings.forEach(this::wfTest4);
            case "5" -> groupSettings.forEach(this::wfTest5);
            default -> throw new IllegalArgumentException("arg = " + arg);
        }

        if (!experimentalWasEnabled) {
            EDT.run(() -> AppContext.enableExperimentalFeatures = false);
        }

        System.out.println("WorkflowTest: finished at " + AppRunner.getCurrentTimeHM());
    }

    private void wfTest1(WithinGroup withinGroup) {
        String compName = createCompName(1, withinGroup);
        app.createNewImage(INITIAL_WIDTH, INITIAL_HEIGHT, compName);

        withinGroup.group(app);

        addGuide();
        runFilterWithDialog("Wood");
        duplicateLayerThenUndo(ImageLayer.class);

        app.addTextLayer("Wood", null, "Pixelitor");
        app.editTextLayer(dialog -> {
            dialog.textBox("textTF").requireText("Wood");
            dialog.slider("fontSize").slideTo(200);
        });
        duplicateLayerThenUndo(TextLayer.class);
        rasterizeThenUndo(TextLayer.class);
        selectionFromText();
        deleteActiveLayer(TextLayer.class);
        rotate90();
        invertSelection();
        deselect();
        rotate270();
        drawTransparentZigzagRectangle();
        enlargeCanvas();

        app.addEmptyImageLayer(true);
        renderCaustics();
        app.selectLayerAbove(); // select the wood layer
        addHeartShapedHoleToTheWoodLayer();
        runFilterWithDialog("Drop Shadow");
        app.mergeDown();
        createEllipseSelection();
        expandSelection();
        selectionToPath();
        flipHorizontal();
        tracePath(BrushType.WOBBLE);
        pathToSelection();
        copySelection();
        move(MOVE_SELECTION_ONLY, 0, -100);
        pasteSelection();
        move(MOVE_SELECTION_ONLY, 0, 50);
        selectionToPath();
        app.swapColors();
        tracePath(BrushType.SHAPE);
        flipHorizontal();
        clearGuides();
        app.clickTool(Tools.BRUSH);

        loadReferenceImage("wf1.png");
    }

    private static String createCompName(int testNr, WithinGroup withinGroup) {
        String compName = "wf " + testNr + withinGroup.getNameSuffix();
        return compName;
    }

    private void ungroup() {
        app.runMenuCommand("Ungroup");
    }

    private void wfTest2(WithinGroup withinGroup) {
        String compName = createCompName(2, withinGroup);
        app.createNewImage(INITIAL_WIDTH, INITIAL_HEIGHT, compName);

        withinGroup.group(app);

        runFilterWithDialog("Spider Web");
        editSpiderWebImage(compName);

        app.addTextLayer("TEXT", null, "Pixelitor");
        convertLayerToSmartObject();

        app.runMenuCommand("Edit Contents");
        app.editTextLayer(dialog -> dialog.textBox("textTF")
            .requireText("TEXT")
            .deleteText()
            .enterText("WARPED TEXT"));

        app.addLayerMask();
        app.drawGradient(RADIAL);
        app.closeCurrentView();

        runFilterWithDialog("Magnify",
            dialog -> {
                dialog.slider("Magnification (%)").slideTo(250);
                dialog.slider("Horizontal").slideTo(250);
            });
        runFilterWithDialog("Frosted Glass");

        app.runMenuCommand("Lower Layer");
        app.runMenuCommand("Raise Layer");
        keyboard.undo("Move Frosted Glass Up");
        keyboard.undo("Move Frosted Glass Down");
        keyboard.redo("Move Frosted Glass Down");
        keyboard.redo("Move Frosted Glass Up");

        app.selectActiveLayer("smart TEXT");
        duplicateLayerThenUndo(SmartObject.class);
        Utils.sleep(1, SECONDS);
        assert EDT.active(Composition::checkInvariants);

        rasterizeThenUndo(SmartObject.class);

        int catMargin = 20;
        int catSize = 100;
        CanvasDrag firstCatShapeLoc = new CanvasDrag(catMargin, INITIAL_HEIGHT - catMargin - catSize, catSize);

        app.addShapesLayer(CAT, firstCatShapeLoc);
        duplicateLayerThenUndo(ShapesLayer.class);
        rasterizeThenUndo(ShapesLayer.class);

        CanvasDrag secondCatLoc = new CanvasDrag(INITIAL_WIDTH - catMargin, INITIAL_HEIGHT - catMargin - catSize, INITIAL_WIDTH - catMargin - catSize, INITIAL_HEIGHT - catMargin);
        app.addShapesLayer(CAT, secondCatLoc);

        // ensure that the first layer is selected and the box is not shown
        app.selectLayerBellow();
        app.selectLayerBellow();

        // double the text
        app.clickLayerPopup("smart TEXT", "Edit Contents");
        app.runMenuCommand("Duplicate Layer");
        move(MOVE_LAYER_ONLY, 0, -25);
        app.runMenuCommand("Lower Layer Selection");
        move(MOVE_LAYER_ONLY, 0, 50);
        app.closeCurrentView();

        loadReferenceImage("wf2.png");
    }

    private void editSpiderWebImage(String compName) {
        app.runMenuCommand("Duplicate");

        addTextLayer("Spider", "Top", "Left");
        addTextLayer("Web", "Top", "Right");

        // switch back to the main tab
        pw.tabbedPane().selectTab(compName);
        int mainTabIndex = EDT.call(() -> ((TabsUI) ImageArea.getUI()).getSelectedIndex());

        runFilterWithDialog("Clouds");
        app.addEmptyImageLayer(false);
        runFilterWithDialog("Fractal Tree",
            dialog -> dialog.slider("Age (Iterations)").slideTo(14));
        app.changeLayerBlendingMode(BlendingMode.HARD_LIGHT);
        app.mergeDown();

        app.addGradientFillLayer(SPIRAL_CW);
        app.changeLayerBlendingMode(BlendingMode.MULTIPLY);
        duplicateLayerThenUndo(GradientFillLayer.class);
        rasterizeThenUndo(GradientFillLayer.class);
        app.mergeDown();

        runFilterWithDialog("Bump Map", dialog ->
            dialog.comboBox("Bump Map").selectItem(compName + " copy"));

        // close the temporary tab
        pw.tabbedPane().selectTab(compName + " copy");
        app.closeCurrentView();
        app.closeDoYouWantToSaveChangesDialog();

        // now the main tab should be the active one
        pw.tabbedPane().requireSelectedTab(Index.atIndex(mainTabIndex));

        app.addColorFillLayer(Color.BLUE);
        app.changeLayerBlendingMode(BlendingMode.HUE);
        app.changeLayerOpacity(0.5f);

        duplicateLayerThenUndo(ColorFillLayer.class);
        rasterizeThenUndo(ColorFillLayer.class);

        app.mergeDown();
    }

    private void wfTest3(WithinGroup withinGroup) {
        String compName = createCompName(3, withinGroup);
        app.createNewImage(INITIAL_WIDTH, INITIAL_HEIGHT, compName);

        withinGroup.group(app);

        runFilterWithDialog("Spirograph");

        app.addTextLayer("Cutout",
            dialog -> dialog.slider("fontSize").slideTo(200), "Pixelitor");

        convertLayerToSmartObject();
        app.runMenuCommand("Edit Contents");

        app.addEmptyImageLayer(false);
        app.runMenuCommand("Lower Layer");
        keyboard.undoRedo("Lower Layer");

        runFilterWithDialog("Plasma");

        // select the text layer
        app.selectLayerAbove();

        // set the text layer's blending mode to erase
        app.changeLayerBlendingMode(BlendingMode.ERASE);

        app.addAdjustmentLayer(ColorBalance.NAME, dialog ->
            dialog.slider("Cyan-Red").slideTo(50));
        app.addLayerMask();
        runFilterWithDialog(Starburst.NAME, dialog ->
            dialog.slider("Spiral").slideTo(100));

//        app.addShapesLayer(ShapeType.HEART, 300, 70);
//        app.runMenuCommand("Rasterize Shape Layer");
//        app.changeLayerBlendingMode(BlendingMode.ERASE);

        app.closeCurrentView();

        // duplicate the whole smart object
        app.runMenuCommand("Duplicate Layer");

        // add a smart filter to the original smart object and copy it
        app.selectLayerBellow();
        runFilterWithDialog("Colorize");
        app.clickLayerPopup("Colorize", "Copy Colorize");

        // paste the smart filter into the copy of the smart object
        app.selectLayerAbove(); // select the whole first smart object
        app.selectLayerAbove(); // select the copy smart object
        app.clickLayerPopup("smart Cutout copy", "Paste Colorize");
        keyboard.undoRedoUndo("Add Smart Colorize copy");

        // rasterize, then delete the copy of the smart object
        rasterizeThenUndo(SmartObject.class);
        deleteActiveLayer(SmartObject.class);

        // delete the Colorize smart object
        app.findLayerIconByLayerName("Colorize").click();
        deleteActiveLayer(SmartFilter.class);

        loadReferenceImage("wf3.png");
    }

    private void wfTest4(WithinGroup withinGroup) {
        String compName = createCompName(4, withinGroup);
        app.createNewImage(INITIAL_WIDTH, INITIAL_HEIGHT, compName);

        withinGroup.group(app);

        runFilterWithDialog("Checker Pattern", dialog -> {
            dialog.slider("Width").slideTo(25);
            dialog.slider("Amount").slideTo(17);
        });

        // add a layer mask with a kiwi image
        app.addLayerMask();
        CanvasDrag kiwiSize = new CanvasDrag(150, 50, 400);
        app.drawShape(KIWI, FOREGROUND, NONE, kiwiSize, true);
        app.runMenuCommand("Invert");

        // edit the checkers pattern inside a smart object's contents
        convertLayerToSmartObject();
        app.runMenuCommand("Edit Contents");
        app.addGradientFillLayer(SPIRAL_CW);
        app.changeLayerBlendingMode(BlendingMode.MULTIPLY);
        app.closeCurrentView();

        // add some smart filters to the checkers kiwi image
        keyboard.pressCtrlOne(); // go back to layer editing mode
        runFilterWithDialog("Glass Tiles");
        runFilterWithDialog("Color Balance", dialog ->
            dialog.slider("Yellow-Blue").slideTo(-45));

        app.selectActiveLayer("smart layer 1");

        duplicateLayerThenUndo(SmartObject.class);
        cloneSmartObjectThenUndo();

        // add a smart object bellow it
        app.addColorFillLayer(Color.BLUE);
        convertLayerToSmartObject();
        app.runMenuCommand("Lower Layer");

        // add a smart filter to the smart object
        runFilterWithDialog("Caustics", dialog -> {
            dialog.slider("Zoom (%)").slideTo(50);
            dialog.slider("Samples (Quality)").slideTo(10);
        });

        // add a layer mask with a vertical gradient to the smart filter
        var popup = app.findLayerIconByLayerName("Caustics").showPopupMenu();
        clickPopupMenu(popup, "Add Layer Mask");

        app.drawGradient(LINEAR, FG_TO_BG, new CanvasDrag(100, 0, 100, INITIAL_HEIGHT), Color.BLACK, Color.WHITE);

        app.setMaskViewModeViaRightClick("Caustics", NORMAL);
        app.setMaskViewModeViaRightClick("Caustics", RUBYLITH);
        app.setMaskViewModeViaRightClick("Caustics", SHOW_MASK);
        app.setMaskViewModeViaRightClick("Caustics", EDIT_MASK);

        app.deleteMaskViaRightClick("Caustics", true);
        app.disableMaskViaRightClick("Caustics");
        app.enableMaskViaRightClick("Caustics");

        app.changeLayerBlendingMode(BlendingMode.DIFFERENCE);

//        app.runMenuCommand("Raise Layer Selection");
//        app.runMenuCommand("Delete Layer");

        app.resize(300);
        app.resize(5);
        keyboard.undoRedoUndo("Resize");
        keyboard.undoRedoUndo("Resize");

        loadReferenceImage("wf4.png");
    }

    private void wfTest5(WithinGroup withinGroup) {
        String compName = createCompName(5, withinGroup);
        app.createNewImage(INITIAL_WIDTH, INITIAL_HEIGHT, compName);

        withinGroup.group(app);

        runFilterWithDialog("Marble");
        convertLayerToSmartObject();
        runFilterWithDialog("Crystallize");
        app.resize(600);
    }

    private void addTextLayer(String text, String vAlignment, String hAlignment) {
        app.addTextLayer(text, dialog -> {
            dialog.comboBox("hAlignmentCB").selectItem(hAlignment);
            dialog.comboBox("vAlignmentCB").selectItem(vAlignment);
        }, "Pixelitor");
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

    private void runFilterWithDialog(String filterName) {
        runFilterWithDialog(filterName, null);
    }

    private void runFilterWithDialog(String filterName, Consumer<DialogFixture> customizer) {
        app.runFilterWithDialog(filterName, Randomize.NO, Reseed.NO, ShowOriginal.NO, false, customizer);

        boolean activeIsSmart = EDT.activeLayer(layer ->
            (layer instanceof SmartObject) || (layer instanceof SmartFilter));
        String expectedEditName = activeIsSmart ? "Add Smart " + filterName : filterName;
        keyboard.undoRedo(expectedEditName);
    }

    private void duplicateLayerThenUndo(Class<? extends Layer> expectedLayerType) {
        int numLayers = EDT.getNumLayersInActiveHolder();
        EDT.assertActiveLayerTypeIs(expectedLayerType);

        app.runMenuCommand("Duplicate Layer");

        EDT.assertNumLayersIs(numLayers + 1);
        EDT.assertActiveLayerTypeIs(expectedLayerType);
        assert EDT.active(Composition::checkInvariants);

        keyboard.undoRedoUndo("Duplicate Layer");

        EDT.assertNumLayersIs(numLayers);
        EDT.assertActiveLayerTypeIs(expectedLayerType);
        assert EDT.active(Composition::checkInvariants);
    }

    private void cloneSmartObjectThenUndo() {
        int numLayers = EDT.getNumLayersInActiveHolder();
        EDT.assertActiveLayerTypeIs(SmartObject.class);

        app.runMenuCommand("Clone");

        EDT.assertNumLayersIs(numLayers + 1);
        EDT.assertActiveLayerTypeIs(SmartObject.class);

        keyboard.undoRedoUndo("Clone");

        EDT.assertNumLayersIs(numLayers);
        EDT.assertActiveLayerTypeIs(SmartObject.class);
    }

    private void rasterizeThenUndo(Class<? extends Layer> expectedLayerType) {
        int numLayers = EDT.getNumLayersInActiveHolder();
        EDT.assertActiveLayerTypeIs(expectedLayerType);
        Layer layer = EDT.getActiveLayer();

        app.runMenuCommand("Rasterize " + layer.getTypeString());
        Utils.sleep(1, SECONDS);

        EDT.assertNumLayersIs(numLayers);
        EDT.assertActiveLayerTypeIs(ImageLayer.class);
        assert EDT.active(Composition::checkInvariants);

        keyboard.undoRedoUndo("Rasterize " + layer.getTypeString());

        EDT.assertActiveLayerTypeIs(expectedLayerType);
        EDT.assertNumLayersIs(numLayers);
        assert EDT.active(Composition::checkInvariants);
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

    private void deleteActiveLayer(Class<? extends Layer> expectedLayerType) {
        String expectedEditName = "Delete " + EDT.active(comp -> comp.getActiveLayer().getName());

        // save a reference, because after the deleting the last child, 
        // it would stop being the active holder
        LayerHolder holder = EDT.active(Composition::getActiveLayerHolder);

        int numLayers = EDT.call(holder::getNumLayers);

        EDT.assertActiveLayerTypeIs(expectedLayerType);
        app.checkNumLayersIs(numLayers);

        pw.button("deleteLayer").click();
        assert EDT.call(holder::getNumLayers) == numLayers - 1;

        keyboard.undo(expectedEditName);
        assert EDT.call(holder::getNumLayers) == numLayers;

        keyboard.redo(expectedEditName);
        assert EDT.call(holder::getNumLayers) == numLayers - 1;
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

        app.runMenuCommand("Actual Pixels");
        mouse.recalcCanvasBounds();

        pw.comboBox("shapeTypeCB").selectItem(RECTANGLE.toString());
        pw.comboBox("fillPaintCB").selectItem(NONE.toString());
        pw.comboBox("strokePaintCB").selectItem(TRANSPARENT.toString());
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

        var searchDialog = app.findDialogByTitle("Find Filter");
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

    private void addHeartShapedHoleToTheWoodLayer() {
        app.setDefaultColors();
        app.addLayerMask();
        CanvasDrag heartLocation = new CanvasDrag(340, 100, 100);
        app.drawShape(HEART, FOREGROUND, NONE, heartLocation, true);

        app.runMenuCommand("Delete"); // in the Layer Mask submenu
        keyboard.undoRedoUndo("Delete Layer Mask");

        app.runMenuCommand("Apply"); // in the Layer Mask submenu
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
        keyboard.undoRedo("Brush Tool");
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

    private void move(MoveMode moveMode, int dx, int dy) {
        app.clickTool(Tools.MOVE);
        pw.comboBox("modeSelector").selectItem(moveMode.toString());
        int startX = INITIAL_WIDTH / 2;
        int startY = INITIAL_HEIGHT / 2;
        mouse.moveToCanvas(startX, startY);
        mouse.dragToCanvas(startX + dx, startY + dy);
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

    private void convertLayerToSmartObject() {
        app.runMenuCommand("Convert Layer to Smart Object");
        keyboard.undoRedo("Convert to Smart Object");
        duplicateLayerThenUndo(SmartObject.class);
    }

    private void loadReferenceImage(String fileName) {
//        app.openFileWithDialog("Open...", referenceImagesDir, fileName);
    }
}
