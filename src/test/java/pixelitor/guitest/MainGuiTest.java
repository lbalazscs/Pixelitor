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

package pixelitor.guitest;

import com.bric.util.JVM;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.*;
import org.fest.util.Files;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Features;
import pixelitor.Views;
import pixelitor.automate.AutoPaint;
import pixelitor.automate.AutoPaintPanel;
import pixelitor.colors.FgBgColorSelector;
import pixelitor.filters.*;
import pixelitor.filters.jhlabsproxies.JHKaleidoscope;
import pixelitor.filters.jhlabsproxies.JHPolarCoordinates;
import pixelitor.filters.levels.Levels;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.*;
import pixelitor.guides.GuideStrokeType;
import pixelitor.history.History;
import pixelitor.history.HistoryChecker;
import pixelitor.io.Dirs;
import pixelitor.io.FileChoosers;
import pixelitor.io.FileFormat;
import pixelitor.io.FileUtils;
import pixelitor.layers.*;
import pixelitor.menus.view.ZoomControl;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.Selection;
import pixelitor.tools.BrushType;
import pixelitor.tools.Symmetry;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.GradientColorType;
import pixelitor.tools.gradient.GradientTool;
import pixelitor.tools.gradient.GradientType;
import pixelitor.tools.move.MoveMode;
import pixelitor.tools.pen.PathTool;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.TwoPointPaintType;
import pixelitor.tools.transform.TransformBox;
import pixelitor.utils.Geometry;
import pixelitor.utils.Rnd;
import pixelitor.utils.Texts;
import pixelitor.utils.Utils;
import pixelitor.utils.input.Modifiers;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_F6;
import static java.awt.event.KeyEvent.VK_F7;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.swing.core.matcher.JButtonMatcher.withText;
import static org.junit.Assert.assertFalse;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.guitest.AppRunner.ExpectConfirmation;
import static pixelitor.guitest.AppRunner.FilterOptions;
import static pixelitor.guitest.AppRunner.clickPopupMenu;
import static pixelitor.guitest.AppRunner.getCurrentTimeHM;
import static pixelitor.guitest.GUITestUtils.change;
import static pixelitor.guitest.GUITestUtils.checkRandomly;
import static pixelitor.guitest.GUITestUtils.findButtonByText;
import static pixelitor.guitest.MaskMode.NO_MASK;
import static pixelitor.menus.view.ZoomLevel.zoomLevels;
import static pixelitor.selection.SelectionModifyType.EXPAND;
import static pixelitor.selection.ShapeCombinator.ADD;
import static pixelitor.selection.ShapeCombinator.REPLACE;
import static pixelitor.tools.DragToolState.IDLE;
import static pixelitor.tools.DragToolState.TRANSFORM;

/**
 * An automated GUI test which uses AssertJ-Swing.
 * This is not a unit test: the app as a whole is tested from the user
 * perspective, and depending on the configuration, it could run for hours.
 * <p>
 * AssertJ-Swing requires using the following VM option:
 * --add-opens java.base/java.util=ALL-UNNAMED
 */
public class MainGuiTest {
    // in quick mode some tests are skipped
    private static boolean quick = false;

    private static File baseDir;
    private static File cleanerScript;

    private static File inputDir;
    private static File batchResizeOutputDir;
    private static File batchFilterOutputDir;
    private static File svgOutputDir;

    private final Robot robot;
    private final FrameFixture pw;

    private final AppRunner app;
    private final Keyboard keyboard;
    private final Mouse mouse;

    private final Random random = new Random();

    private MaskMode maskMode = NO_MASK;

    // whether to expect the default text
    private boolean textFilterTested = false;

    // these don't have to be tested for every mask mode
    private boolean maskIndependentToolsTested = false;
    private boolean fileMenuTested = false;
    private boolean preferencesTested = false;
    private boolean maskFromColorRangeTested = false;
    private boolean helpMenuTested = false;
    private boolean colorsTested = false;
    private boolean viewMenuTested = false;

    // whether filters should be tested with images with a width or height of 1 pixel
    private static final boolean FILTER_TESTS_WITH_HEIGHT_1 = false;
    private static final boolean FILTER_TESTS_WITH_WIDTH_1 = false;

    /**
     * The entry point for this GUI test.
     */
    public static void main(String[] args) {
        Texts.init();
        Utils.ensureAssertionsEnabled();
        FailOnThreadViolationRepaintManager.install();

        // enable quick mode with -Dquick=true
        quick = "true".equals(System.getProperty("quick"));

        processCLArguments(args);

//        GlobalKeyboardWatch.registerDebugMouseWatching(false);

        new MainGuiTest();
    }

    private MainGuiTest() {
        long startMillis = System.currentTimeMillis();

        app = new AppRunner(new HistoryChecker(), inputDir, svgOutputDir, "a.jpg");

        robot = app.getRobot();
        pw = app.getPW();
        keyboard = app.getKeyboard();
        mouse = app.getMouse();

        if (EDT.call(FileChoosers::useNativeDialogs)) {
            // we can't test if native file choosers are enabled
            System.out.println("MainGuiTest: native dialogs, exiting");
            System.exit(0);
        }

        boolean testOneThingSlowly = false;
        if (testOneThingSlowly) {
            app.runSlowly();

            testFilterWithDialog("Rose", FilterOptions.SHAPES);
        } else {
            app.runMenuCommand("Reset Workspace");

            MaskMode[] maskModes = MaskMode.load();
            TestSuite testSuite = TestSuite.load();
            printTestConfiguration(testSuite, maskModes);

            for (int i = 0; i < maskModes.length; i++) {
                MaskMode mode = maskModes[i];
                runTests(testSuite, mode);

                if (i < maskModes.length - 1) {
                    resetState();
                }
            }
        }

        long totalTimeMillis = System.currentTimeMillis() - startMillis;
        System.out.printf("MainGuiTest: finished at %s after %s, exiting in ",
            getCurrentTimeHM(), Utils.formatDuration(totalTimeMillis));

        app.exit();
    }

    private static void printTestConfiguration(TestSuite testSuite, MaskMode[] maskModes) {
        System.out.println("Quick = " + quick
            + ", test suite = " + testSuite
            + ", mask modes = " + Arrays.toString(maskModes));
    }

    /**
     * Resets the application state to a known baseline for a new test run.
     */
    private void resetState() {
        if (EDT.call(Views::getNumViews) > 0) {
            app.closeAll();
        }
        openFileWithDialog(inputDir, "a.jpg");

        clickAndResetRectSelectTool();
        clickAndResetShapesTool();
    }

    private void openFileWithDialog(File inputDir, String fileName) {
        app.openFileWithDialog("Open...", inputDir, fileName);
        maskMode.apply(this);
    }

    private void clickAndResetRectSelectTool() {
        app.clickTool(Tools.RECTANGLE_SELECTION);
        pw.comboBox("combinatorCB").selectItem("Replace");
    }

    private void clickAndResetShapesTool() {
        app.clickTool(Tools.SHAPES);
    }

    /**
     * Configures and runs a specific test suite with a given mask mode.
     */
    private void runTests(TestSuite testSuite, MaskMode maskMode) {
        this.maskMode = maskMode;
        maskMode.apply(this);
        app.configureRobotDelay();

        System.out.printf("MainGuiTest: testSuite = %s, testingMode = %s, started at %s%n",
            testSuite, maskMode, getCurrentTimeHM());

        app.runTests(() -> testSuite.run(this));
    }

    /**
     * Runs all tests.
     */
    void testAll() {
        testTools();
        testFileMenu();
        testAutoPaint();
        testEditMenu();
        testImageMenu();
        testFilters();
        testViewMenu();
        testHelpMenu();
        testColors();
        testLayers();
    }

    /**
     * Tests all tools.
     */
    void testTools() {
        log(0, "tools");

        // make sure we have a big enough canvas for the tool tests
        keyboard.actualPixels();

        // test mask-independent tools only once, or randomly 5% of the time, to save time
        if (!maskIndependentToolsTested || Rnd.nextDouble() < 0.05) {
            testMoveTool();
            testCropTool();
            testSelectionToolsAndMenus();

            testPathTools();
            testHandTool();
            testZooming();
            testColorSelector();
            maskIndependentToolsTested = true;
        }

        testBrushTool();
        testCloneTool();
        testEraserTool();
        testSmudgeTool();
        testGradientTool();
        testPaintBucketTool();
        testColorPickerTool();
        testShapesTool();

        app.reload();

        maskMode.apply(this);
        afterTestActions();
    }

    /**
     * Tests layer-related operations from the layers panel and menus.
     */
    void testLayers() {
        log(0, "layers");
        maskMode.apply(this);

        testChangeLayerOpacityAndBM();

        testAddLayer();
        testDeleteLayer();
        testDuplicateLayer();

        testLayerVisibilityChange();

        testLayerOrderChangeFromMenu();
        testActiveLayerChangeFromMenu();
        testLayerToCanvasSize();
        testLayerMenusChangingNumLayers();

        testRotateFlip(false);

        testLayerMasks();
        testTextLayers();
        testMaskFromColorRange();

        if (Features.enableExperimental) {
            testAdjLayers();
        }

        afterTestActions();
    }

    /**
     * Tests adding a new empty image layer.
     */
    private void testAddLayer() {
        log(1, "add layer");

        app.checkLayerNamesAre("layer 1");
        var layer1Button = findLayerButton("layer 1");
        layer1Button.requireSelected();

        var addEmptyLayerButton = pw.button("addLayer");

        // add layer
        addEmptyLayerButton.click();

        app.checkLayerNamesAre("layer 1", "layer 2");
        var layer2Button = findLayerButton("layer 2");
        layer2Button.requireSelected();

        keyboard.undo("New Empty Layer");
        app.checkLayerNamesAre("layer 1");
        layer1Button.requireSelected();

        keyboard.redo("New Empty Layer");
        app.checkLayerNamesAre("layer 1", "layer 2");
        layer2Button.requireSelected();
        maskMode.apply(this);

        app.drawGradientFromCenter(GradientType.SPIRAL_CW);
    }

    private void testChangeLayerOpacityAndBM() {
        log(1, "change layer opacity and blending mode");

        app.changeLayerOpacity(0.75f);
        checkConsistency();

        app.changeLayerBlendingMode(Rnd.chooseFrom(BlendingMode.LAYER_MODES));
        checkConsistency();
    }

    private void testDeleteLayer() {
        log(1, "delete layer");

        var layer1Button = findLayerButton("layer 1");
        var layer2Button = findLayerButton("layer 2");

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
        layer2Button = findLayerButton("layer 2");
        layer2Button.requireSelected();

        // redo delete
        keyboard.redo("Delete layer 2");
        app.checkLayerNamesAre("layer 1");
        layer1Button.requireSelected();

        maskMode.apply(this);
    }

    private void testDuplicateLayer() {
        log(1, "duplicate layer");

        app.checkLayerNamesAre("layer 1");
        pw.button("duplicateLayer").click();

        findLayerButton("layer 1 copy").requireSelected();
        app.checkLayerNamesAre("layer 1", "layer 1 copy");

        keyboard.undo("Duplicate Layer");
        app.checkLayerNamesAre("layer 1");
        findLayerButton("layer 1").requireSelected();

        keyboard.redo("Duplicate Layer");
        app.checkLayerNamesAre("layer 1", "layer 1 copy");
        findLayerButton("layer 1 copy").requireSelected();

        maskMode.apply(this);
    }

    private void testLayerVisibilityChange() {
        log(1, "layer visibility change");

        var layer1CopyButton = findLayerButton("layer 1 copy");
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
        log(1, "layer order change from menu");

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
        log(1, "active layer change from menu");

        app.runMenuCommand("Lower Layer Selection");
        keyboard.undoRedo("Lower Layer Selection");

        app.runMenuCommand("Raise Layer Selection");
        keyboard.undoRedo("Raise Layer Selection");
    }

    private void testLayerToCanvasSize() {
        log(1, "layer to canvas size");

        // add a translation to make it a big layer,
        // otherwise "layer to canvas size" has no effect
        addTranslation();

        app.runMenuCommand("Layer to Canvas Size");
        keyboard.undoRedo("Layer to Canvas Size");
    }

    private void testLayerMenusChangingNumLayers() {
        log(1, "layer menus changing the number of layers");

        app.runMenuCommand("New from Visible");
        keyboard.undoRedo("New Layer from Visible");
        maskMode.apply(this);

        app.mergeDown();

        app.duplicateLayer(ImageLayer.class);
        maskMode.apply(this);

        app.runMenuCommand("New Layer");
        keyboard.undoRedo("New Empty Layer");
        maskMode.apply(this);

        app.runMenuCommand("Delete Layer");
        keyboard.undoRedo("Delete layer 3");
        maskMode.apply(this);

        app.runMenuCommand("Flatten Image");
        assertFalse(History.canUndo());
        maskMode.apply(this);
    }

    private void testLayerMasks() {
        log(1, "layer masks");

        boolean allowExistingMask = maskMode != NO_MASK;
        addLayerMask(allowExistingMask);

        testLayerMaskIconPopupMenus();

        app.deleteLayerMask();

        maskMode.apply(this);

        checkConsistency();
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
        clickPopupMenu(popupMenu, action);
        keyboard.undoRedoUndo(historyName);
    }

    /**
     * Tests a pair of toggleable actions from the layer mask popup menu.
     */
    private void testToggleablePopupAction(String action1, String history1, String action2, String history2) {
        // test the first action (e.g., Disable) and its undo/redo
        var popupMenu = pw.label("maskIcon").showPopupMenu();
        clickPopupMenu(popupMenu, action1);
        keyboard.undoRedo(history1);

        // test the second action (e.g., Enable) and its undo/redo
        popupMenu = pw.label("maskIcon").showPopupMenu();
        clickPopupMenu(popupMenu, action2);
        keyboard.undoRedo(history2);
    }

    private void testMaskFromColorRange() {
        if (maskFromColorRangeTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(1, "mask from color range");

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
        maskMode.apply(this);

        maskFromColorRangeTested = true;
    }

    private void testTextLayers() {
        log(1, "text layers");

        checkConsistency();

        String text = "some text";
        app.addTextLayer(text, null, "Pixelitor");
        maskMode.apply(this);

        app.editTextLayer(dialog -> testTextDialog(dialog, text));

        checkConsistency();

        app.runMenuCommand("Rasterize Text Layer");
        keyboard.undoRedoUndo("Rasterize Text Layer");

        checkConsistency();

        app.mergeDown();

        maskMode.apply(this);
        checkConsistency();
    }

    private void testTextDialog(DialogFixture dialog, String expectedText) {
        dialog.textBox("textArea")
            .requireText(expectedText)
            .deleteText()
            .enterText("my text");

        dialog.slider("fontSize").slideTo(250);
        dialog.checkBox("boldCB").check().uncheck();
        dialog.checkBox("italicCB").check().uncheck().check();

        findButtonByText(dialog, "Advanced...").click();

        var advDialog = app.findDialogByTitle("Advanced Text Settings");
        advDialog.checkBox("underlineCB").check().uncheck();
        advDialog.checkBox("strikeThroughCB").check().uncheck();
        advDialog.checkBox("kerningCB").check().uncheck();
        advDialog.checkBox("ligaturesCB").check().uncheck();
        advDialog.slider("trackingGUI").slideTo(10);

        advDialog.button("ok").click();
        advDialog.requireNotVisible();
    }

    private void testAdjLayers() {
        log(1, "adj. layers");

        addAdjustmentLayer();

        checkConsistency();
    }

    /**
     * Finds a layer's UI component by its name.
     */
    private LayerGUIFixture findLayerButton(String layerName) {
        return new LayerGUIFixture(robot, robot.finder()
            .find(new GenericTypeMatcher<>(LayerGUI.class) {
                @Override
                protected boolean isMatching(LayerGUI layerGUI) {
                    return layerGUI.getLayerName().equals(layerName);
                }

                @Override
                public String toString() {
                    return "LayerButton Matcher, layerName = " + layerName;
                }
            }));
    }

//<editor-fold desc="colors">

    void testColors() {
        if (colorsTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(0, "colors");

        testColorPaletteMenu("Foreground...", "Foreground Color Variations");
        testColorPaletteMenu("Background...", "Background Color Variations");

        testColorPaletteMenu("HSB Mix Foreground with Background...", "HSB Mix with Background");
        testColorPaletteMenu("RGB Mix Foreground with Background...", "RGB Mix with Background");
        testColorPaletteMenu("HSB Mix Background with Foreground...", "HSB Mix with Foreground");
        testColorPaletteMenu("RGB Mix Background with Foreground...", "RGB Mix with Foreground");

        testColorPaletteMenu("Color Palette...", "Color Palette");

        afterTestActions();
        colorsTested = true;
    }

    private void testColorPaletteMenu(String menuName, String dialogTitle) {
        app.runMenuCommand(menuName);
        testColorPaletteDialog(dialogTitle);
    }

    private void testColorPaletteDialog(String dialogTitle) {
        var dialog = app.findDialogByTitle(dialogTitle);
        if (dialogTitle.contains("Foreground")) {
            dialog.resizeTo(new Dimension(500, 500));
        } else {
            dialog.resizeTo(new Dimension(700, 500));
        }
        dialog.close();
        dialog.requireNotVisible();
    }

//</editor-fold>
//<editor-fold desc="file menu">

    void testFileMenu() {
        if (fileMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(0, "file menu");

        cleanOutputs();

        testNewImage();
        testSave("png");
        testSave("pxc");
        closeOneOfTwoViews();
        testFileOpen();
        closeOneOfTwoViews();
        testExportOptimizedJPEG();
        testMagick();
        testExportLayerAnimation();
        testExportTweeningAnimation();
        testReload();
        testShowMetadata();
        testBatchResize();
        testBatchFilter();
        testExportLayerToPNG();
        testScreenCapture();
        testCloseAll();

        // open an image for the next test
        openFileWithDialog(inputDir, "a.jpg");

        fileMenuTested = true;
        afterTestActions();
    }

    private void testNewImage() {
        log(1, "new image");

        app.createNewImage(611, 411, null);

        app.closeCurrentView(ExpectConfirmation.NO);
    }

    private void testFileOpen() {
        log(1, "file open");

        app.runMenuCommand("Open...");
        var openDialog = app.findOpenFileChooser();
        openDialog.cancel();

        openFileWithDialog(inputDir, "b.jpg");

        afterTestActions();
    }

    private void testSave(String extension) {
        log(1, "save, ext = " + extension);

        // create a new image to be saved
        app.createNewImage(400, 400, null);
        maskMode.apply(this);

        // the new image is unsaved => has no file
        assertThat(EDT.queryActiveComp(Composition::getFile)).isNull();

        String fileName = "saved." + extension;
        app.runMenuCommand("Save");
        // new unsaved image, will be saved with a file chooser
        app.acceptSaveDialog(baseDir, fileName);

        // now that the file is saved, save again:
        // no file chooser should appear
        app.runMenuCommand("Save");
        Utils.sleep(500, MILLISECONDS);

        // test "Save As"
        app.runMenuCommand("Save As...");
        // there is always a dialog for "Save As"
        app.acceptSaveDialog(baseDir, fileName);

        app.closeCurrentView(ExpectConfirmation.NO);

        openFileWithDialog(baseDir, fileName);
        maskMode.apply(this);

        // can be dirty if a masked mask mode is set
        app.closeCurrentView(ExpectConfirmation.UNKNOWN);

        maskMode.apply(this);
        afterTestActions();
    }

    private void testExportOptimizedJPEG() {
        log(1, "testing export optimized jpeg");

        app.runMenuCommand("Export Optimized JPEG...");

        // wait for the preview to be calculated
        Utils.sleep(2, SECONDS);

        app.findDialogByTitle("Export Optimized JPEG").button("ok").click();
        app.acceptSaveDialog(baseDir, "saved.jpg");

        afterTestActions();
    }

    private void testMagick() {
        log(1, "testing ImageMagick export-import");

        // test importing
        app.openFileWithDialog("Import...", baseDir, "webp_image.webp");

        // test exporting
        app.runMenuCommand("Export...");
        String exportFileName = "saved_image.webp";
        app.acceptSaveDialog(baseDir, exportFileName);
        app.findJOptionPane("WebP Export Options for " + exportFileName)
            .buttonWithText("Export").click();

        app.closeCurrentView(ExpectConfirmation.NO);
        afterTestActions();
    }

    private void testExportLayerAnimation() {
        log(1, "testing exporting layer animation");

        // precondition: the active image has only 1 layer
        app.checkNumLayersIs(1);

        app.runMenuCommand("Export Layer Animation...");
        // error dialog, because there is only one layer
        app.findJOptionPane("Not Enough Layers")
            .okButton().click();

        app.duplicateLayer(ImageLayer.class);
        app.invert();

        // this time it should work
        app.runMenuCommand("Export Layer Animation...");
        app.findDialogByTitle("Export Animated GIF").button("ok").click();

        app.acceptSaveDialog(baseDir, "layeranim.gif");

        afterTestActions();
    }

    private void testExportTweeningAnimation() {
        log(1, "testing export tweening animation");

        assertThat(EDT.getNumViews()).isGreaterThan(0);

        app.runMenuCommand("Export Tweening Animation...");
        var dialog = app.findDialogByTitle("Export Tweening Animation");
        String[] searchTexts = {"wav", "kalei"};
        dialog.textBox("searchTF").enterText(Rnd.chooseFrom(searchTexts));
        dialog.pressKey(VK_DOWN).releaseKey(VK_DOWN)
            .pressKey(VK_DOWN).releaseKey(VK_DOWN);
        dialog.button("ok").click(); // next
        dialog.requireVisible();

        dialog.button(withText("Randomize Settings")).click();
        dialog.button("ok").click(); // next
        dialog.requireVisible();

        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // next
        dialog.requireVisible();

        if (quick) {
            dialog.textBox("numSecondsTF").deleteText().enterText("1");
            dialog.textBox("fpsTF").deleteText().enterText("4");
            dialog.label("numFramesLabel").requireText("4");
        } else {
            dialog.textBox("numSecondsTF").deleteText().enterText("3");
            dialog.textBox("fpsTF").deleteText().enterText("5");
            dialog.label("numFramesLabel").requireText("15");
        }

        dialog.button("ok").click(); // render button
        dialog.requireVisible(); // still visible because of the validation error

        app.findJOptionPane("Folder Not Empty")
            .yesButton().click();
        dialog.requireNotVisible();

        app.waitForProgressMonitorEnd();

        afterTestActions();
    }

    private void closeOneOfTwoViews() {
        log(1, "testing close one of two views");

        int numOpenImages = EDT.call(Views::getNumViews);
        if (numOpenImages == 1) {
            app.createNewImage(400, 400, null);
        }

        EDT.assertNumViewsIs(2);

        app.closeCurrentView(ExpectConfirmation.UNKNOWN);

        EDT.assertNumViewsIs(1);

        maskMode.apply(this);
        afterTestActions();
    }

    private void testCloseAll() {
        log(1, "testing close all");

        assertThat(EDT.getNumViews()).isGreaterThan(0);

        app.closeAll();
        EDT.assertNumViewsIs(0);

        afterTestActions();
    }

    private void testShowMetadata() {
        log(1, "testing show metadata");

        app.runMenuCommand("Show Metadata...");
        String title = "Metadata for "
            + EDT.queryActiveComp(Composition::getName);
        var dialog = app.findDialogByTitle(title);

        dialog.button("expandAllButton").click();
        dialog.button("collapseAllButton").click();

        dialog.button("ok").click();
        dialog.requireNotVisible();

        afterTestActions();
    }

    private void testBatchResize() {
        log(1, "testing batch resize");
        maskMode.apply(this);

        EDT.run(() -> {
            Dirs.setLastOpen(inputDir);
            Dirs.setLastSave(batchResizeOutputDir);
            FileFormat.setLastSaved(FileFormat.JPG);
        });

        app.runMenuCommand("Batch Resize...");
        var dialog = app.findDialogByTitle("Batch Resize");

        dialog.textBox("widthTF").setText("200");
        dialog.textBox("heightTF").setText("200");
        dialog.button("ok").click();
        dialog.requireNotVisible();

        Utils.sleep(5, SECONDS);

        checkOutputFilesWereCreated(batchResizeOutputDir);
        afterTestActions();
    }

    private void testBatchFilter() {
        log(1, "testing batch filter");

        Dirs.setLastOpen(inputDir);
        Dirs.setLastSave(batchFilterOutputDir);

        assertThat(EDT.getNumViews()).isGreaterThan(0);
        maskMode.apply(this);

        app.runMenuCommand("Batch Filter...");
        var dialog = app.findDialogByTitle("Batch Filter");
        dialog.textBox("searchTF").enterText("wav");
        dialog.pressKey(VK_DOWN).releaseKey(VK_DOWN)
            .pressKey(VK_DOWN).releaseKey(VK_DOWN);
        dialog.button("ok").click(); // next

        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // start processing
        dialog.requireNotVisible();

        app.waitForProgressMonitorEnd();

        afterTestActions();

        checkOutputFilesWereCreated(batchFilterOutputDir);
    }

    private static void checkOutputFilesWereCreated(File outputDir) {
        for (File inputFile : FileUtils.listSupportedInputFiles(inputDir)) {
            String fileName = inputFile.getName();

            File outFile = new File(outputDir, fileName);
            assertThat(outFile).exists().isFile();
        }
    }

    private void testExportLayerToPNG() {
        log(1, "testing export layer to png");

        Dirs.setLastSave(baseDir);

        app.duplicateLayer(ImageLayer.class);
        app.invert();
        maskMode.apply(this);

        app.runMenuCommand("Export Layers to PNG...");
        app.findDialogByTitle("Select Output Folder").button("ok").click();
        Utils.sleep(2, SECONDS);

        afterTestActions();
    }

    void testAutoPaint() {
        log(0, "testing AutoPaint");

        runWithSelectionTranslationCombinations(this::testAutoPaintTask);

        afterTestActions();
    }

    private void testAutoPaintTask() {
        for (Tool tool : AutoPaint.SUPPORTED_TOOLS) {
            if (skip()) {
                continue;
            }
            if (tool == Tools.BRUSH) {
                for (String colorMode : AutoPaintPanel.COLOR_MODES) {
                    testAutoPaintWithTool(tool, colorMode);
                }
            } else {
                testAutoPaintWithTool(tool, null);
            }
        }
    }

    private void testAutoPaintWithTool(Tool tool, String colorMode) {
        app.runMenuCommand("Auto Paint...");
        var dialog = app.findDialogByTitle("Auto Paint");

        var toolSelector = dialog.comboBox("toolSelector");
        toolSelector.selectItem(tool.toString());

        var strokeCountTF = dialog.textBox("strokeCountTF");
        String testNumStrokes = "111";
        if (!strokeCountTF.text().equals(testNumStrokes)) {
            strokeCountTF.deleteText();
            strokeCountTF.enterText(testNumStrokes);
        }

        var colorsCB = dialog.comboBox("colorsCB");
        if (colorMode != null) {
            colorsCB.requireEnabled();
            colorsCB.selectItem(colorMode);
        } else {
            colorsCB.requireDisabled();
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();

        keyboard.undoRedoUndo("Auto Paint");
    }

    private void testScreenCapture() {
        log(1, "testing screen capture");

        View prevView = EDT.getActiveView();
        testScreenCapture(true);
        testScreenCapture(false);

        EDT.activate(prevView);

        afterTestActions();
    }

    private void testScreenCapture(boolean hidePixelitor) {
        app.runMenuCommand("Screen Capture...");
        var dialog = app.findDialogByTitle("Screen Capture");
        var cb = dialog.checkBox();
        if (hidePixelitor) {
            cb.check();
        } else {
            cb.uncheck();
        }
        dialog.button("ok").click();
        dialog.requireNotVisible();

        maskMode.apply(this);

        afterTestActions();
    }

    private void testReload() {
        log(1, "testing reload");

        app.reload();
        maskMode.apply(this);

        afterTestActions();
    }
//</editor-fold>
//<editor-fold desc="edit menu">

    void testEditMenu() {
        log(0, "edit menu");

        testMenuUndoRedo();
        testFade();
        testCopyPaste();
        testPreferences();

        afterTestActions();
    }

    private void testMenuUndoRedo() {
        app.invert();
        app.runMenuCommand("Undo Invert");
        app.runMenuCommand("Redo Invert");
    }

    private void testFade() {
        app.runMenuCommand("Fade Invert...");
        var dialog = app.findFilterDialog();

        dialog.slider().slideTo(75); // set opacity to 75%

        dialog.checkBox("show original").check().uncheck();

        dialog.button("ok").click();

        keyboard.undoRedoUndo("Fade");
    }

    private void testCopyPaste() {
        log(1, "copy-paste");

        EDT.assertNumViewsIs(1);
        String existingLayerName = "layer 1";
        String activeCompAtStartName = EDT.queryActiveComp(Composition::getName);
        app.checkLayerNamesAre(existingLayerName);

        app.runMenuCommand("Copy Layer/Mask");

        app.runMenuCommand("Paste as New Layer");
        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        keyboard.undo("New Pasted Layer");
        app.checkLayerNamesAre(existingLayerName);

        keyboard.redo("New Pasted Layer");
        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        app.runMenuCommand("Copy Composite");
        app.runMenuCommand("Paste as New Image");
        assertThat(EDT.getActiveCompName()).startsWith("Pasted Image ");

        // close the pasted image
        app.closeCurrentView(ExpectConfirmation.NO);
        EDT.assertOpenCompNamesAre(activeCompAtStartName);

        // delete the pasted layer
        app.checkLayerNamesAre(existingLayerName, "pasted layer");
        assert DeleteActiveLayerAction.INSTANCE.isEnabled();
        app.runMenuCommand("Delete Layer");
        app.checkLayerNamesAre(existingLayerName);

        keyboard.undo("Delete pasted layer");
        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        keyboard.redo("Delete pasted layer");
        app.checkLayerNamesAre(existingLayerName);

        maskMode.apply(this);
    }

    private void testPreferences() {
        if (preferencesTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(1, "preferences dialog");

        app.runMenuCommand("Preferences...");
        var dialog = app.findDialogByTitle("Preferences");
        if (preferencesTested) {
            dialog.tabbedPane().selectTab("UI");
        }

        // test "Images In"
        testPreferencesUIChooser(dialog);

        // test "Layer/Mask Thumb Sizes"
        var thumbSizeCB = dialog.comboBox("thumbSizeCB");
        thumbSizeCB.selectItem(3);
        thumbSizeCB.selectItem(0);

        // test the Mouse tab

        // test the Guides tab
        dialog.tabbedPane().selectTab("Guides");
        GuideStrokeType[] guideStyles = GuideStrokeType.values();
        dialog.comboBox("guideStyleCB").selectItem(Rnd.chooseFrom(guideStyles).toString());
        dialog.comboBox("cropGuideStyleCB").selectItem(Rnd.chooseFrom(guideStyles).toString());

        // test the Advanced tab
        dialog.tabbedPane().selectTab("Advanced");
        testPreferencesUndoLevels(dialog);

        dialog.button("ok").click();
        // this time the preferences dialog should close
        dialog.requireNotVisible();

        preferencesTested = true;
    }

    private static void testPreferencesUIChooser(DialogFixture dialog) {
        var uiChooser = dialog.comboBox("uiChooser");
        if (EDT.call(() -> ImageArea.isActiveMode(FRAMES))) {
            uiChooser.requireSelection("Internal Windows");
            uiChooser.selectItem("Tabs");
            uiChooser.selectItem("Internal Windows");
        } else {
            uiChooser.requireSelection("Tabs");
            uiChooser.selectItem("Internal Windows");
            uiChooser.selectItem("Tabs");
        }
    }

    private void testPreferencesUndoLevels(DialogFixture dialog) {
        var undoLevelsTF = dialog.textBox("undoLevelsTF");
        boolean undoWas5 = undoLevelsTF.text().equals("5");
        undoLevelsTF.deleteText().enterText("n");

        // try to accept the error dialog
        dialog.button("ok").click();
        app.expectAndCloseErrorDialog();

        // correct the error
        if (undoWas5) {
            undoLevelsTF.deleteText().enterText("6");
        } else {
            undoLevelsTF.deleteText().enterText("5");
        }
    }

//</editor-fold>
//<editor-fold desc="image menu">

    void testImageMenu() {
        log(0, "image menu");

        // image from the previous tests
        EDT.assertNumViewsIs(1);
        app.checkNumLayersIs(1);

        testCropSelection();

        // add more layer types
        // TODO set to false, because currently not all layer types
        //   create undo edits when used with the move tool
        boolean addExtraLayers = false;
        if (addExtraLayers) {
            app.addGradientFillLayer(GradientType.ANGLE);
            app.addColorFillLayer(Color.BLUE);
            app.addShapesLayer(ShapeType.BAT, CanvasDrag.diagonal(20, 380, 100));
        }

        testDuplicateImage();

        // crop is tested with the crop tool

        runWithSelectionTranslationCombinations(() -> {
            testResize();
            testEnlargeCanvas();
            testRotateFlip(true);
        });

        if (addExtraLayers) {
            // delete the 3 extra layers
            pw.button("deleteLayer")
                .requireEnabled()
                .click()
                .click()
                .click();
            app.checkNumLayersIs(1);
        }

        maskMode.apply(this);
        maskMode.check();

        afterTestActions();
    }

    private void testCropSelection() {
        // create the selection that will be cropped
        clickAndResetRectSelectTool();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(400, 400);
        keyboard.undoRedo("Create Selection");
        EDT.requireSelection();

        testCropSelection(() -> app.runMenuCommand("Crop Selection"),
            false, 200.0, 200.0);

        app.deselect();
    }

    private void testDuplicateImage() {
        int numLayers = EDT.getNumLayersInActiveHolder();
        log(1, "image duplication, num layers = " + numLayers);

        EDT.assertNumViewsIs(1);

        app.runMenuCommand("Duplicate");
        EDT.assertNumViewsIs(2);
        EDT.assertNumLayersInActiveHolderIs(numLayers);

        closeOneOfTwoViews();
        EDT.assertNumViewsIs(1);
    }

    private void testResize() {
        log(2, "resize");
        app.resize(622);

        keyboard.undo("Resize");
    }

    private void testEnlargeCanvas() {
        log(2, "enlarge canvas");
        app.enlargeCanvas(100, 100, 100, 100);
        keyboard.undoRedoUndo("Enlarge Canvas");
    }

//</editor-fold>
//<editor-fold desc="view menu">

    void testViewMenu() {
        if (viewMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(0, "view menu");

        EDT.assertNumViewsIs(1);
        app.checkNumLayersIs(1);

        testZoomCommands();
        testHistory();
        testHideShowUI();
        testGuides();

        if (ImageArea.isActiveMode(FRAMES)) {
            app.runMenuCommand("Cascade");
            app.runMenuCommand("Tile");
        }

        afterTestActions();
        viewMenuTested = true;
    }

    private void testZoomCommands() {
        log(1, "zoom commands");

        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        app.runMenuCommand("Zoom In");
        EDT.assertActiveZoomIs(startingZoom.zoomIn());

        app.runMenuCommand("Zoom Out");
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomOut());

        app.runMenuCommand("Fit Space");
        app.runMenuCommand("Fit Width");
        app.runMenuCommand("Fit Height");

        app.runMenuCommand("Actual Pixels");
        EDT.assertActiveZoomIs(ZoomLevel.ACTUAL_SIZE);
    }

    private void testHideShowUI() {
        log(1, "hide/show UI elements");

        app.runMenuCommand("Reset Workspace");
        assert EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert EDT.call(LayersContainer::areLayersShown);
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Hide Status Bar");
        assert !EDT.call(StatusBar::isShown);

        app.runMenuCommand("Show Status Bar");
        assert EDT.call(StatusBar::isShown);

        app.runMenuCommand("Show Histograms");
        assert EDT.call(HistogramsPanel::isShown);

        app.runMenuCommand("Hide Histograms");
        assert !EDT.call(HistogramsPanel::isShown);

        keyboard.press(VK_F6);
        assert EDT.call(HistogramsPanel::isShown);

        keyboard.press(VK_F6);
        assert !EDT.call(HistogramsPanel::isShown);

        app.runMenuCommand("Hide Layers");
        assert !EDT.call(LayersContainer::areLayersShown);

        app.runMenuCommand("Show Layers");
        assert EDT.call(LayersContainer::areLayersShown);

        keyboard.press(VK_F7);
        assert !EDT.call(LayersContainer::areLayersShown);

        keyboard.press(VK_F7);
        assert EDT.call(LayersContainer::areLayersShown);

        app.runMenuCommand("Hide Tools");
        assert !EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Show Tools");
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Hide All");
        assert !EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert !EDT.call(LayersContainer::areLayersShown);
        assert !EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Restore Workspace");
        assert EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert EDT.call(LayersContainer::areLayersShown);
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());
    }

    private void testGuides() {
        log(1, "guides");

        app.runMenuCommand("Add Horizontal Guide...");
        var dialog = app.findDialogByTitle("Add Horizontal Guide");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getActiveGuides().getHorizontals()).containsExactly(0.5);
        assertThat(EDT.getActiveGuides().getVerticals()).isEmpty();
        keyboard.undoRedo("Create Guides");

        app.runMenuCommand("Add Vertical Guide...");
        dialog = app.findDialogByTitle("Add Vertical Guide");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getActiveGuides().getHorizontals()).containsExactly(0.5);
        assertThat(EDT.getActiveGuides().getVerticals()).containsExactly(0.5);
        keyboard.undoRedo("Change Guides");

        app.runMenuCommand("Add Grid Guides...");
        dialog = app.findDialogByTitle("Add Grid Guides");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getActiveGuides().getHorizontals()).containsExactly(0.25, 0.5, 0.75);
        assertThat(EDT.getActiveGuides().getVerticals()).containsExactly(0.25, 0.5, 0.75);
        keyboard.undoRedo("Change Guides");

        app.runMenuCommand("Clear Guides");
        keyboard.undoRedo("Clear Guides");
        assertThat(EDT.getActiveGuides()).isNull();
    }

    private void testHistory() {
        log(1, "history");

        // before testing make sure that we have something
        // in the history even if this is running alone
        app.clickTool(Tools.BRUSH);
        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();
        keyboard.undoRedo("Brush Tool");

        app.clickTool(Tools.ERASER);
        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();
        keyboard.undoRedo("Eraser Tool");

        // now start testing the history window
        app.runMenuCommand("Show History...");
        var dialog = app.findDialogByTitle("History");

        var undoButton = dialog.button("undo");
        var redoButton = dialog.button("redo");

        undoButton.requireEnabled();
        redoButton.requireDisabled();

        undoButton.click();
        redoButton.requireEnabled();
        redoButton.click();

        var list = dialog.list();

        // after clicking the first item,
        // we have one last undo
        list.clickItem(0);
        undoButton.requireEnabled();
        redoButton.requireEnabled();
        undoButton.click();
        // no more undo, the list should contain no selection
        list.requireNoSelection();
        undoButton.requireDisabled();
        redoButton.requireEnabled();

        // after clicking the last item,
        // we have a selection and undo, but no redo
        String[] contents = list.contents();
        int lastIndex = contents.length - 1;
        list.clickItem(lastIndex);
        list.requireSelection(lastIndex);
        undoButton.requireEnabled();
        redoButton.requireDisabled();

        dialog.close();
        dialog.requireNotVisible();
    }

//</editor-fold>
//<editor-fold desc="filters">

    void testFilters() {
        log(0, "filters");
//        app.setIndexedMode();

        EDT.assertNumViewsIs(1);
        app.checkNumLayersIs(1);

        EDT.requireNoSelection();
        EDT.assertNoTranslation();

        boolean squashImage = FILTER_TESTS_WITH_WIDTH_1 || FILTER_TESTS_WITH_HEIGHT_1;
        if (squashImage) {
            if (FILTER_TESTS_WITH_WIDTH_1 && FILTER_TESTS_WITH_HEIGHT_1) {
                app.resize(1, 1);
            } else if (FILTER_TESTS_WITH_WIDTH_1) {
                app.resize(1, 100);
            } else if (FILTER_TESTS_WITH_HEIGHT_1) {
                app.resize(100, 1);
            }
        }

        testRepeatLast();
        testColorFilters(squashImage);
        testArtisticFilters();
        testBlurSharpenFilters();
        testDisplaceFilters();
        testDistortFilters();
        testFindEdgesFilters();
        testGMICFilters();
        testLightFilters();
        testNoiseFilters();
        testOtherFilters();
        testRenderFilters();
        testTransitionsFilters();

        afterTestActions();
    }

    private void testRepeatLast() {
        app.invert();
        app.runMenuCommand("Repeat Invert");
        keyboard.undoRedo("Invert"); // needed by the history checker
    }

    private void testColorFilters(boolean squashedImage) {
        testColorBalance(squashedImage);
        testFilterWithDialog(HueSat.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Colorize.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Levels.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(BrightnessContrast.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Solarize.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Sepia.NAME, FilterOptions.TRIVIAL);
        testInvert(squashedImage);
        testFilterWithDialog("Channel Invert", FilterOptions.TRIVIAL);
        testFilterWithDialog(ChannelMixer.NAME, FilterOptions.STANDARD, "Swap Red-Green", "Swap Red-Blue", "Swap Green-Blue",
            "R -> G -> B -> R", "R -> B -> G -> R",
            "Average BW", "Luminosity BW", "Sepia");
        testFilterWithDialog("Equalize", FilterOptions.STANDARD);
        testFilterWithDialog("Extract Channel", FilterOptions.STANDARD);
        testNoDialogFilter("Luminosity");
        testNoDialogFilter("Value = max(R,G,B)");
        testNoDialogFilter(ExtractChannelFilter.DESATURATE_NAME);
        testNoDialogFilter(GUIText.HUE);
        testNoDialogFilter("Hue (with colors)");
        testNoDialogFilter(ExtractChannelFilter.SATURATION_NAME);
        testFilterWithDialog("Quantize", FilterOptions.STANDARD);
        testFilterWithDialog(Posterize.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Threshold.NAME, FilterOptions.STANDARD);
        testFilterWithDialog("Color Threshold", FilterOptions.STANDARD);
        testFilterWithDialog("Tritone", FilterOptions.STANDARD);
        testFilterWithDialog("Gradient Map", FilterOptions.TRIVIAL);
        testNoDialogFilter(GUIText.FG_COLOR);
        testNoDialogFilter(GUIText.BG_COLOR);
        testNoDialogFilter("Transparent");
        testFilterWithDialog("Color Wheel", FilterOptions.RENDERING);
        testFilterWithDialog("Four Color Gradient", FilterOptions.RENDERING);
    }

    private void testBlurSharpenFilters() {
        testFilterWithDialog("Box Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Focus", FilterOptions.STANDARD);
        testFilterWithDialog("Gaussian Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Lens Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Motion Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Smart Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Spin and Zoom Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Unsharp Mask", FilterOptions.STANDARD);
    }

    private void testDistortFilters() {
        testFilterWithDialog("Swirl, Pinch, Bulge", FilterOptions.STANDARD);
        testFilterWithDialog("Circle to Square", FilterOptions.STANDARD);
        testFilterWithDialog("Perspective", FilterOptions.STANDARD);
        testFilterWithDialog("Lens Over Image", FilterOptions.STANDARD);
        testFilterWithDialog("Magnify", FilterOptions.STANDARD);
        testFilterWithDialog("Turbulent Distortion", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Underwater", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Water Ripple", FilterOptions.STANDARD);
        testFilterWithDialog("Waves", FilterOptions.STANDARD);
        testFilterWithDialog("Angular Waves", FilterOptions.STANDARD);
        testFilterWithDialog("Radial Waves", FilterOptions.STANDARD);
        testFilterWithDialog("Glass Tiles", FilterOptions.STANDARD);
        testFilterWithDialog("Polar Glass Tiles", FilterOptions.STANDARD);
        testFilterWithDialog("Frosted Glass", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog(LittlePlanet.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(JHPolarCoordinates.NAME, FilterOptions.STANDARD);
        testFilterWithDialog("Wrap Around Arc", FilterOptions.STANDARD);
    }

    private void testDisplaceFilters() {
        testFilterWithDialog("Displacement Map", FilterOptions.STANDARD);
        testFilterWithDialog("Drunk Vision", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Grid Kaleidoscope", FilterOptions.STANDARD);
        testFilterWithDialog(JHKaleidoscope.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Mirror.NAME, FilterOptions.STANDARD);
        testFilterWithDialog("Offset", FilterOptions.TRIVIAL);
        testFilterWithDialog("Slice", FilterOptions.STANDARD);
        testFilterWithDialog("Tile Seamless", FilterOptions.STANDARD);
        testFilterWithDialog("Video Feedback", FilterOptions.STANDARD);
    }

    private void testLightFilters() {
        testFilterWithDialog("Bump Map", FilterOptions.STANDARD);
        testFilterWithDialog("Flashlight", FilterOptions.STANDARD);
        testFilterWithDialog("Glint", FilterOptions.STANDARD);
        testFilterWithDialog("Glow", FilterOptions.STANDARD);
        testFilterWithDialog("Rays", FilterOptions.STANDARD);
        testFilterWithDialog("Sparkle", FilterOptions.STANDARD_RESEED);
    }

    private void testNoiseFilters() {
        testFilterWithDialog("Kuwahara", FilterOptions.STANDARD);
        testNoDialogFilter("Reduce Single Pixel Noise");
        testNoDialogFilter("3x3 Median Filter");
        testFilterWithDialog("Add Noise", FilterOptions.STANDARD);
        testFilterWithDialog("Pixelate", FilterOptions.STANDARD);
    }

    private void testRenderFilters() {
        testFilterWithDialog("Clouds", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Plasma", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Value Noise", FilterOptions.RENDERING_RESEED);

        testFilterWithDialog("Abstract Lights", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Brushed Metal", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Caustics", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Cells", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Flow Field", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Marble", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Voronoi Diagram", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Wood", FilterOptions.RENDERING_RESEED);

        // Curves
        testFilterWithDialog("Circle Weave", FilterOptions.SHAPES);
        testFilterWithDialog("Flower of Life", FilterOptions.SHAPES);
        testFilterWithDialog("L-Systems", FilterOptions.SHAPES);
        testFilterWithDialog("Grid", FilterOptions.SHAPES);
        testFilterWithDialog("Lissajous Curve", FilterOptions.SHAPES);
        testFilterWithDialog("Spider Web", FilterOptions.SHAPES);
        testFilterWithDialog("Spiral", FilterOptions.SHAPES);
        testFilterWithDialog("Spirograph", FilterOptions.SHAPES);

        // Fractals
        testFilterWithDialog("Chaos Game", FilterOptions.RENDERING);
        testFilterWithDialog("Fractal Tree", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Julia Set", FilterOptions.RENDERING);
        testFilterWithDialog("Mandelbrot Set", FilterOptions.RENDERING);

        // Geometry
        testFilterWithDialog("Border Mask", FilterOptions.RENDERING);
        testFilterWithDialog("Concentric Shapes", FilterOptions.SHAPES);
        testFilterWithDialog("Checker Pattern", FilterOptions.RENDERING);
        testFilterWithDialog("Cubes Pattern", FilterOptions.SHAPES);
        testFilterWithDialog("Penrose Tiling", FilterOptions.RENDERING);
        testFilterWithDialog("Rose", FilterOptions.SHAPES);
        testFilterWithDialog("Starburst", FilterOptions.SHAPES);
        testFilterWithDialog("Stripes", FilterOptions.SHAPES);
        testFilterWithDialog("Truchet Tiles", FilterOptions.RENDERING);
    }

    private void testArtisticFilters() {
        testFilterWithDialog("Comic Book", FilterOptions.STANDARD);
        testFilterWithDialog("Crystallize", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Pointillize", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Stamp", FilterOptions.STANDARD);
        testFilterWithDialog("Oil Painting", FilterOptions.STANDARD);
        testFilterWithDialog("Spheres", FilterOptions.STANDARD);
        testFilterWithDialog("Smear", FilterOptions.STANDARD);
        testFilterWithDialog("Emboss", FilterOptions.STANDARD);
        testFilterWithDialog("Orton Effect", FilterOptions.STANDARD);
        testFilterWithDialog("Photo Collage", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Weave", FilterOptions.STANDARD);

        testFilterWithDialog("Dots Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Striped Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Concentric Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Color Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Ordered Dithering", FilterOptions.STANDARD);
    }

    private void testFindEdgesFilters() {
        testFilterWithDialog("Canny", FilterOptions.STANDARD);
        testFilterWithDialog("Convolution Edge Detection", FilterOptions.STANDARD);
        testFilterWithDialog("Difference of Gaussians", FilterOptions.STANDARD);
        testNoDialogFilter("Laplacian");
    }

    private void testGMICFilters() {
        app.resize(200);

        // Artistic
        testFilterWithDialog("Bokeh", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Box Fitting", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Brushify", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Cubism", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Huffman Glitches", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Random 3D Objects", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Rodilius", FilterOptions.STANDARD);
        testFilterWithDialog("Voronoi", FilterOptions.STANDARD_RESEED);

        // Blur/Sharpen
        testFilterWithDialog("Anisotropic Smoothing", FilterOptions.STANDARD);
        testFilterWithDialog("Bilateral Smoothing", FilterOptions.STANDARD);

        testFilterWithDialog("G'MIC Command", FilterOptions.STANDARD);
        testFilterWithDialog("Light Glow", FilterOptions.STANDARD);
        testFilterWithDialog("Local Normalization", FilterOptions.STANDARD);
        testFilterWithDialog("Stroke", FilterOptions.STANDARD);
        testFilterWithDialog("Vibrance", FilterOptions.STANDARD);

        // the image was reduced in size at the start of the GMIC filter tests
        app.closeCurrentView(ExpectConfirmation.YES);
        app.openFileWithDialog("Open...", inputDir, "a.jpg");
        maskMode.apply(this);
    }

    private void testOtherFilters() {
        testFilterWithDialog("Drop Shadow", FilterOptions.STANDARD);
        testFilterWithDialog("Morphology", FilterOptions.STANDARD);
//        testRandomFilter();
        testText();
        testFilterWithDialog("Transform Layer", FilterOptions.STANDARD);

        testConvolution();

        testFilterWithDialog("Channel to Transparency", FilterOptions.STANDARD);
        testNoDialogFilter("Invert Transparency");
    }

    private void testConvolution() {
        testFilterWithDialog("Custom 3x3 Convolution", FilterOptions.NONE, "Corner Blur", "\"Gaussian\" Blur", "Mean Blur", "Sharpen",
            "Edge Detection", "Edge Detection 2", "Horizontal Edge Detection",
            "Vertical Edge Detection", "Emboss", "Emboss 2", "Color Emboss",
            "Reset", "Randomize");
        testFilterWithDialog("Custom 5x5 Convolution", FilterOptions.NONE, "Diamond Blur", "Motion Blur",
            "Find Horizontal Edges", "Find Vertical Edges",
            "Find / Edges", "Find \\ Edges", "Sharpen",
            "Reset", "Randomize");
    }

    private void testTransitionsFilters() {
        testFilterWithDialog("2D Transitions", FilterOptions.STANDARD);
        testFilterWithDialog("Blinds Transition", FilterOptions.STANDARD);
        testFilterWithDialog("Checkerboard Transition", FilterOptions.STANDARD);
        testFilterWithDialog("Goo Transition", FilterOptions.STANDARD);
        testFilterWithDialog("Shapes Grid Transition", FilterOptions.STANDARD);
    }

    private void testColorBalance(boolean squashedImage) {
        runWithSelectionTranslationCombinations(squashedImage, () ->
            testFilterWithDialog(ColorBalance.NAME,
                FilterOptions.STANDARD));
    }

    private void testInvert(boolean squashedImage) {
        runWithSelectionTranslationCombinations(squashedImage, () ->
            testNoDialogFilter(Invert.NAME));
    }

    private void testText() {
        if (skip()) {
            return;
        }
        log(1, "filter Text");

        app.runMenuCommand("Text...");
        var dialog = app.findFilterDialog();

        testTextDialog(dialog, textFilterTested ?
            "my text" : TextSettings.DEFAULT_TEXT);

        findButtonByText(dialog, "OK").click();
        afterFilterRunActions("Text");

        textFilterTested = true;
    }

    private void testRandomFilter() {
        log(1, "random filter");

        app.runMenuCommand("Random Filter...");
        var dialog = app.findFilterDialog();
        var nextRandomButton = findButtonByText(dialog, "Next Random Filter");
        var backButton = findButtonByText(dialog, "Back");
        var forwardButton = findButtonByText(dialog, "Forward");

        nextRandomButton.requireEnabled();
        backButton.requireDisabled();
        forwardButton.requireDisabled();

        nextRandomButton.click();
        backButton.requireEnabled();
        forwardButton.requireDisabled();

        nextRandomButton.click();
        backButton.click();
        forwardButton.requireEnabled();

        backButton.click();
        forwardButton.click();
        nextRandomButton.click();

        findButtonByText(dialog, "OK").click();

        afterFilterRunActions("Random Filter");
    }

    private void testNoDialogFilter(String name) {
        if (skip()) {
            return;
        }
        log(1, "filter " + name);

        app.runMenuCommand(name);

        afterFilterRunActions(name);
    }

    private void testFilterWithDialog(String name,
                                      FilterOptions options,
                                      String... extraButtonsToClick) {
        if (skip()) {
            return;
        }
        log(1, "filter " + name);

        boolean testPresets = !quick && maskMode == NO_MASK;

        Consumer<DialogFixture> extraButtonClicker = dialog -> {
            for (String buttonText : extraButtonsToClick) {
                JButtonFixture button = findButtonByText(dialog, buttonText);
                if (button.isEnabled()) { // channel mixer presets might not be enabled
                    button.click();
                }
            }
        };

        app.runFilterWithDialog(name, options, testPresets, extraButtonClicker);

        afterFilterRunActions(name);
    }

    private void afterFilterRunActions(String filterName) {
        // it could happen that a filter returns the source image,
        // and then nothing is put into the history
        if (History.getLastEditName().equals(filterName)) {
            keyboard.undoRedoUndo(filterName);
        }

        checkConsistency();
    }

    private void stressTestFilterWithDialog(String name, FilterOptions options, boolean resizeToSmall) {
        if (resizeToSmall) {
            app.resize(200);
            app.runMenuCommand("Zoom In");
            app.runMenuCommand("Zoom In");
        }

        String nameWithoutDots = name.substring(0, name.length() - 3);
        log(1, "filter " + nameWithoutDots);

        app.runMenuCommand(name);
        var dialog = app.findFilterDialog();

        int max = 1000;
        for (int i = 0; i < max; i++) {
            System.out.println("MainGuiTest stress testing " + nameWithoutDots + ": " + (i + 1) + " of " + max);
            if (options.randomize()) {
                findButtonByText(dialog, "Randomize Settings").click();
            }
            if (options.reseed()) {
                findButtonByText(dialog, "Reseed").click();
                findButtonByText(dialog, "Reseed").click();
                findButtonByText(dialog, "Reseed").click();
            }
        }

        dialog.button("ok").click();
    }

    //</editor-fold>
//<editor-fold desc="help menu">

    void testHelpMenu() {
        if (helpMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(0, "help menu");

        testTipOfTheDay();
        testInternalState();
        testCheckForUpdate();
        testAbout();

        afterTestActions();
        helpMenuTested = true;
    }

    private void testTipOfTheDay() {
        var laf = EDT.call(UIManager::getLookAndFeel);

        app.runMenuCommand("Tip of the Day");
        var dialog = app.findDialogByTitle("Tip of the Day");
        if (laf instanceof NimbusLookAndFeel) {
            findButtonByText(dialog, "Next >").click();
            findButtonByText(dialog, "Next >").click();
            findButtonByText(dialog, "< Back").click();
        } else {
            findButtonByText(dialog, "Next Tip").click();
            findButtonByText(dialog, "Next Tip").click();
        }
        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    private void testInternalState() {
        app.runMenuCommand("Internal State...");
        var dialog = app.findDialogByTitle("Internal State");
        findButtonByText(dialog, "Copy as JSON").click();
        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    private void testCheckForUpdate() {
        app.runMenuCommand("Check for Updates...");
        try {
            // the title is either "Pixelitor Is Up to Date"
            // or "New Version Available"
            app.findJOptionPane(null).buttonWithText("Close").click();
        } catch (ComponentLookupException e) {
            // if a close button was not found, then it must be the up-to-date dialog
            app.findJOptionPane("Pixelitor Is Up to Date").okButton().click();
        }
    }

    private void testAbout() {
        app.runMenuCommand("About Pixelitor");
        var dialog = app.findDialogByTitle("About Pixelitor");

        var tabbedPane = dialog.tabbedPane();
        tabbedPane.requireTabTitles("About", "Credits", "System Info");
        tabbedPane.selectTab("Credits");
        tabbedPane.selectTab("System Info");
        tabbedPane.selectTab("About");

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }
//</editor-fold>
//<editor-fold desc="hand tool">

    private void testHandTool() {
        log(1, "hand tool");

        app.clickTool(Tools.HAND);

        mouse.randomAltClick();

        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();

        testAutoZoomButtons();

        afterTestActions();
    }

//</editor-fold>
//<editor-fold desc="shapes tool">

    private void testShapesTool() {
        log(1, "shapes tool");
        app.clickTool(Tools.SHAPES);

        keyboard.randomizeColors();

        // reset defaults
        pw.comboBox("shapeTypeCB").selectItem(ShapeType.RECTANGLE.toString());
        pw.comboBox("fillPaintCB").selectItem(TwoPointPaintType.RADIAL_GRADIENT.toString());
        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.NONE.toString());

        EDT.assertShapesToolStateIs(IDLE);
        pw.button("convertToSelection").requireDisabled();

        mouse.randomCtrlClick();
        mouse.randomAltClick();
        mouse.randomShiftClick();

        mouse.moveToCanvas(50, 50);
        mouse.dragToCanvas(150, 100);
        keyboard.undoRedo("Create Shape");

        changeShapesToolEffects();
        keyboard.undoRedo("Change Shape Effects");

        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.FOREGROUND.toString());
        keyboard.undoRedo("Change Shape Stroke");

        setupStrokeSettingsDialog();
        keyboard.undoRedo("Change Shape Stroke Settings");

        mouse.moveToCanvas(200, 50);
        app.setMaxUntestedEdits(2); // the drag will create two edits
        mouse.dragToCanvas(300, 100);
        keyboard.undoRedo("Rasterize Shape", "Create Shape");
        app.setMaxUntestedEdits(1); // reset

        pw.comboBox("shapeTypeCB").selectItem(ShapeType.CAT.toString());
        keyboard.undoRedo("Change Shape Type");

        // resize the transform box by the SE handle
        mouse.moveToCanvas(300, 100);
        mouse.dragToCanvas(500, 300);
        keyboard.undoRedo("Change Transform Box");

        ShapeType[] shapeTypes = ShapeType.values();
        for (ShapeType shapeType : shapeTypes) {
            if (shapeType == ShapeType.CAT || skip()) {
                continue;
            }
            pw.comboBox("shapeTypeCB").selectItem(shapeType.toString());
            keyboard.undoRedo("Change Shape Type");
        }

        for (TwoPointPaintType paintType : TwoPointPaintType.values()) {
            if (skip()) {
                continue;
            }
            if (change(pw.comboBox("fillPaintCB"), paintType.toString())) {
                keyboard.undoRedo("Change Shape Fill");
            }
            if (change(pw.comboBox("strokePaintCB"), paintType.toString())) {
                keyboard.undoRedo("Change Shape Stroke");
            }
        }

        EDT.assertShapesToolStateIs(TRANSFORM);
        pw.button("convertToSelection").requireEnabled();

        mouse.clickCanvas(50, 300);

        EDT.assertShapesToolStateIs(IDLE);
        pw.button("convertToSelection").requireDisabled();

        keyboard.undoRedoUndo("Rasterize Shape");

        // test convert to selection
        pw.button("convertToSelection").requireEnabled().click();
        keyboard.undoRedo("Convert Path to Selection");
        app.deselect();

        afterTestActions();
    }

    private void changeShapesToolEffects() {
        findButtonByText(pw, "Effects...")
            .requireEnabled()
            .click();

        var dialog = app.findDialogByTitle("Effects");
        var tabbedPane = dialog.tabbedPane();
        tabbedPane.requireTabTitles(
            EffectsPanel.GLOW_TAB_NAME,
            EffectsPanel.INNER_GLOW_TAB_NAME,
            EffectsPanel.NEON_BORDER_TAB_NAME,
            EffectsPanel.DROP_SHADOW_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.INNER_GLOW_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.NEON_BORDER_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.DROP_SHADOW_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.GLOW_TAB_NAME);

        JCheckBoxFixture enabledCB = dialog.checkBox("enabledCB");
        boolean activeTabEnabled = enabledCB.target().isSelected();
        enabledCB.check(!activeTabEnabled); // toggle in order to force a change

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    private void setupStrokeSettingsDialog() {
        findButtonByText(pw, "Stroke Settings...")
            .requireEnabled()
            .click();
        var dialog = app.findDialogByTitle("Stroke Settings");

        // force a change in the stroke width
        var strokeWidthSlider = dialog.slider();
        int value = strokeWidthSlider.target().getValue();
        if (value == 5) { // 5 is the default stroke width
            strokeWidthSlider.slideTo(20);
        } else {
            strokeWidthSlider.slideTo(5);
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

//</editor-fold>
//<editor-fold desc="color picker tool">

    private void testColorPickerTool() {
        log(1, "color picker tool");

        app.clickTool(Tools.COLOR_PICKER);

        mouse.randomAltClick();

        mouse.moveToCanvas(300, 300);
        pw.click();
        mouse.dragToCanvas(400, 400);

        afterTestActions();
    }

//</editor-fold>
//<editor-fold desc="path tools">

    private void testPathTools() {
        testPenTool();
        testNodeTool();
        testTransformPathTool();

        afterTestActions();
    }

    private void testPenTool() {
        log(1, "pen tool");
        app.clickTool(Tools.PEN);

        pw.button("toSelectionButton").requireDisabled();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(200, 400);
        keyboard.undoRedo("Subpath Start");

        mouse.moveToCanvas(300, 400);
        mouse.dragToCanvas(300, 200);
        keyboard.undoRedo("Add Anchor Point");

        mouse.moveToCanvas(200, 200);
        mouse.click();
        keyboard.undoRedo("Close Subpath");

        assertThat(EDT.getActivePath())
            .isNotNull()
            .numSubPathsIs(1)
            .numAnchorsIs(2);

        keyboard.undo("Close Subpath");
        keyboard.undo("Add Anchor Point");
        keyboard.undo("Subpath Start");

        assertThat(EDT.getActivePath()).isNull();

        keyboard.redo("Subpath Start");
        keyboard.redo("Add Anchor Point");
        keyboard.redo("Close Subpath");

        // add a second subpath, this one will be open and
        // consists of straight segments
        mouse.clickCanvas(600, 200);
        keyboard.undoRedo("Subpath Start");

        mouse.clickCanvas(600, 300);
        keyboard.undoRedo("Add Anchor Point");

        mouse.clickCanvas(700, 300);
        keyboard.undoRedo("Add Anchor Point");

        mouse.clickCanvas(700, 200);
        keyboard.undoRedo("Add Anchor Point");

        mouse.ctrlClickCanvas(700, 150);
        keyboard.undoRedo("Finish Subpath");

        assertThat(EDT.getActivePath())
            .isNotNull()
            .numSubPathsIs(2)
            .numAnchorsIs(6);

        testTraceButtons(Tools.PEN);
    }

    private void testNodeTool() {
        log(1, "node tool");
        app.clickTool(Tools.NODE);

        mouse.moveToCanvas(600, 300);
        mouse.dragToCanvas(500, 400);
        keyboard.undoRedo("Move Anchor Point");

        var popupMenu = mouse.showPopupAtCanvas(500, 400);
        clickPopupMenu(popupMenu, "Delete Point");
        keyboard.undoRedoUndo("Delete Anchor Point");

        popupMenu = mouse.showPopupAtCanvas(500, 400);
        clickPopupMenu(popupMenu, "Delete Subpath");
        keyboard.undoRedoUndo("Delete Subpath");

        popupMenu = mouse.showPopupAtCanvas(500, 400);
        clickPopupMenu(popupMenu, "Delete Path");
        keyboard.undoRedoUndo("Delete Path");

        // drag out handle
        mouse.moveToCanvas(500, 400);
        mouse.altDragToCanvas(600, 500);
        keyboard.undoRedo("Move Control Handle");

        popupMenu = mouse.showPopupAtCanvas(500, 400);
        clickPopupMenu(popupMenu, "Retract Handles");
        keyboard.undoRedo("Retract Handles");

        // test convert to selection
        pw.button("toSelectionButton")
            .requireEnabled()
            .click();
        EDT.assertActiveToolIs(Tools.LASSO_SELECTION);

        keyboard.undo("Convert Path to Selection");
        EDT.assertActiveToolIs(Tools.NODE);

        keyboard.redo("Convert Path to Selection");
        EDT.assertActiveToolIs(Tools.LASSO_SELECTION);

        app.invert();

        pw.button("toPathButton")
            .requireEnabled()
            .click();
        EDT.assertActiveToolIs(Tools.NODE);
        assertThat(EDT.getActivePath()).isNotNull();

        keyboard.undo("Convert Selection to Path");
        EDT.assertActiveToolIs(Tools.LASSO_SELECTION);
        assertThat(EDT.getActivePath()).isNull();

        keyboard.redo("Convert Selection to Path");
        EDT.assertActiveToolIs(Tools.NODE);
        assertThat(EDT.getActivePath()).isNotNull();

        testTraceButtons(Tools.NODE);
    }

    private void testTransformPathTool() {
        log(1, "transform path tool");
        app.clickTool(Tools.TRANSFORM_PATH);

        Point nw = EDT.getTransformPathToolBoxPos(0, TransformBox::getNW);
        mouse.moveToScreen(nw.x, nw.y);
        mouse.dragToScreen(nw.x - 100, nw.y - 50);
        keyboard.undoRedo("Change Transform Box");

        Point rot = EDT.getTransformPathToolBoxPos(1, TransformBox::getRot);
        mouse.moveToScreen(rot.x, rot.y);
        mouse.dragToScreen(rot.x + 100, rot.y + 100);
        keyboard.undoRedo("Change Transform Box");

        testTraceButtons(Tools.TRANSFORM_PATH);
    }

    // tests the trace buttons in one of the path tools
    private void testTraceButtons(PathTool tool) {
        log(2, "test tracing in " + tool.getName());

        pw.button("traceWithSmudge")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Smudge Tool");

        pw.button("traceWithEraser")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Eraser Tool");

        pw.button("traceWithBrush")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Brush Tool");
    }

//</editor-fold>
//<editor-fold desc="paint bucket tool">

    private void testPaintBucketTool() {
        log(1, "paint bucket tool");
        app.clickTool(Tools.PAINT_BUCKET);

        mouse.randomAltClick();
        keyboard.undoRedoUndo("Paint Bucket Tool");

        mouse.moveToCanvas(300, 300);
        pw.click();
        keyboard.undoRedoUndo("Paint Bucket Tool");

        afterTestActions();
    }

//</editor-fold>
//<editor-fold desc="gradient tool">

    private void testGradientTool() {
        log(1, "gradient tool");

        app.clickTool(Tools.GRADIENT);

        if (maskMode.isMaskEditing()) {
            // reset the default colors, otherwise it might be all gray
            keyboard.fgBgDefaults();
        }

        mouse.randomAltClick(); // TODO this adds a history entry
        keyboard.undoRedo("Hide Gradient Handles");

        boolean gradientCreated = false;

        for (GradientType gradientType : GradientType.values()) {
            if (change(pw.comboBox("typeCB"), gradientType.toString()) && gradientCreated) {
                keyboard.undoRedo("Change Gradient Type");
            }

            for (String cycleMethod : GradientTool.CYCLE_METHODS) {
                if (change(pw.comboBox("cycleMethodCB"), cycleMethod) && gradientCreated) {
                    keyboard.undoRedo("Change Gradient Cycling");
                }

                GradientColorType[] gradientColorTypes = GradientColorType.values();
                for (GradientColorType colorType : gradientColorTypes) {
                    if (skip()) {
                        continue;
                    }
                    if (change(pw.comboBox("colorTypeCB"), colorType.toString()) && gradientCreated) {
                        keyboard.undoRedo("Change Gradient Colors");
                    }

                    if (checkRandomly(pw.checkBox("reverseCB")) && gradientCreated) {
                        keyboard.undoRedo("Reverse Gradient");
                    }

                    // drag the gradient
                    Point start = mouse.moveRandomlyWithinCanvas();
                    Point end = mouse.dragRandomlyWithinCanvas();
                    if (!gradientCreated) { // this was the first
                        keyboard.undoRedo("Create Gradient");
                    } else {
                        keyboard.undoRedo("Change Gradient");
                    }

                    // test the handle movement
                    double rd = random.nextDouble();
                    if (rd < 0.33) {
                        // drag the end handle
                        mouse.moveToScreen(end.x, end.y);
                        mouse.dragRandomlyWithinCanvas();
                    } else if (rd > 0.66) {
                        // drag the start handle
                        mouse.moveToScreen(start.x, start.y);
                        mouse.dragRandomlyWithinCanvas();
                    } else {
                        // drag the middle handle
                        Point2D c = Geometry.midPoint(start, end);
                        mouse.moveToScreen((int) c.getX(), (int) c.getY());
                        mouse.dragRandomlyWithinCanvas();
                    }
                    keyboard.undoRedo("Change Gradient");
                    gradientCreated = true;
                }
            }
        }

        if (gradientCreated) { // pretty likely
            keyboard.pressEsc(); // hide the gradient handles
            keyboard.undoRedo("Hide Gradient Handles");
        }
        afterTestActions();
    }

//</editor-fold>
//<editor-fold desc="brush tools">

    private void testEraserTool() {
        log(1, "eraser tool");

        app.clickTool(Tools.ERASER);

        testBrushStrokes(Tools.ERASER);

        afterTestActions();
    }

    private void testBrushTool() {
        log(1, "brush tool");

        app.clickTool(Tools.BRUSH);

        enableLazyMouse(false);
        testBrushStrokes(Tools.BRUSH);

        // TODO this freezes when running with coverage??
        // sometimes also without coverage??
//        enableLazyMouse(true);
//        testBrushStrokes();

        afterTestActions();
    }

    private void enableLazyMouse(boolean b) {
        pw.button("lazyMouseDialogButton")
            .requireEnabled()
            .click();
        var dialog = app.findDialogByTitle("Lazy Mouse Settings");
        if (b) {
            dialog.checkBox().check();
            dialog.slider("distSlider")
                .requireEnabled()
                .slideToMinimum();
        } else {
            dialog.checkBox().uncheck();
            dialog.slider("distSlider").requireDisabled();
        }
        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    private void testBrushStrokes(Tool tool) {
        mouse.randomAltClick();

        for (BrushType brushType : BrushType.values()) {
            pw.comboBox("typeCB").selectItem(brushType.toString());
            app.testBrushSettings(tool, brushType);

            for (Symmetry symmetry : Symmetry.values()) {
                if (skip()) {
                    continue;
                }
                pw.comboBox("symmetrySelector").selectItem(symmetry.toString());
                keyboard.randomizeColors();
                mouse.moveRandomlyWithinCanvas();
                mouse.dragRandomlyWithinCanvas();
                keyboard.undoRedo(tool == Tools.BRUSH ? "Brush Tool" : "Eraser Tool");
            }
        }
    }

    private void testSmudgeTool() {
        log(1, "smudge tool");

        app.clickTool(Tools.SMUDGE);

        mouse.randomAltClick(); // adds no history entry

        mouse.randomClick(); // adds a history entry
        keyboard.undoRedo("Smudge Tool");

        for (int i = 0; i < 3; i++) {
            mouse.shiftMoveClickRandom();
            keyboard.undoRedo("Smudge Tool");

            mouse.moveRandomlyWithinCanvas();
            mouse.dragRandomlyWithinCanvas();
            keyboard.undoRedo("Smudge Tool");
        }

        afterTestActions();
    }

    private void testCloneTool() {
        log(1, "clone tool");

        app.clickTool(Tools.CLONE);

        testClone(false, false, 100);
        testClone(false, true, 200);
        testClone(true, false, 300);
        testClone(true, true, 400);

        afterTestActions();
    }

    private void testClone(boolean aligned, boolean sampleAllLayers, int startX) {
        pw.checkBox("alignedCB").check(aligned);

        pw.checkBox("sampleAllLayersCB").check(sampleAllLayers);

        // set the source point
        mouse.moveToCanvas(300, 300);
        mouse.click(Modifiers.ALT);

        // do some cloning
        mouse.moveToCanvas(startX, 300);
        for (int i = 1; i <= 2; i++) {
            int x = startX + i * 10;
            mouse.dragToCanvas(x, 300);
            keyboard.undoRedo("Clone Stamp Tool");
            mouse.dragToCanvas(x, 400);
            keyboard.undoRedo("Clone Stamp Tool");
        }
    }

//</editor-fold>
//<editor-fold desc="move tool">

    private void testMoveTool() {
        log(1, "move tool");

        addSelection(); // so that the moving of the selection can be tested
        app.clickTool(Tools.MOVE);

        MoveMode[] moveModes = MoveMode.values();
        for (MoveMode mode : moveModes) {
            pw.comboBox("modeSelector").selectItem(mode.toString());

            testMoveToolDrag(mode, false);
            testMoveToolDrag(mode, true);
            app.checkNumLayersIs(1);

            keyboard.nudge();
            keyboard.undoRedoUndo(mode.getEditName());
            app.checkNumLayersIs(1);

            // check that all move-related edits have been undone
            EDT.assertEditToBeUndoneNameIs("Create Selection");

            testMoveToolClick(mode, Modifiers.NONE);
            testMoveToolClick(mode, Modifiers.CTRL);
            testMoveToolClick(mode, Modifiers.SHIFT);
            testMoveToolClick(mode, Modifiers.ALT);
        }

        maskMode.apply(this);
        afterTestActions();
    }

    private void testMoveToolDrag(MoveMode mode, boolean altDrag) {
        mouse.moveToCanvas(400, 400);

        if (altDrag) {
            app.setMaxUntestedEdits(2);

            // adds 2 edits: "Duplicate Layer", "Move"
            mouse.altDragToCanvas(300, 300);

            keyboard.undo(mode.getEditName());
            if (mode.movesLayer()) {
                keyboard.undoRedo("Duplicate Layer");
            }
            keyboard.redo(mode.getEditName());

            app.setMaxUntestedEdits(1);
        } else {
            View view = EDT.getActiveView();
            Drawable dr = view.getComp().getActiveDrawableOrThrow();
            assert dr.getTx() == 0 : "tx = " + dr.getTx();
            assert dr.getTy() == 0 : "ty = " + dr.getTx();

            mouse.dragToCanvas(200, 300);

            if (mode.movesLayer()) {
                // the translations will have these values only if we are at 100% zoom
                assert view.getZoomLevel() == ZoomLevel.ACTUAL_SIZE : "zoom is " + view.getZoomLevel();
                assert dr.getTx() == -200 : "tx = " + dr.getTx();
                assert dr.getTy() == -100 : "ty = " + dr.getTy();
            } else {
                assert dr.getTx() == 0 : "tx = " + dr.getTx();
                assert dr.getTy() == 0 : "ty = " + dr.getTy();
            }
        }

        keyboard.undoRedoUndo(mode.getEditName());

        if (altDrag && mode.movesLayer()) {
            // The alt-dragged movement creates two history edits:
            // a duplicate and a layer move. Now also undo the duplication.
            keyboard.undo("Duplicate Layer");
        }

        // check that all move-related edits have been undone
        EDT.assertEditToBeUndoneNameIs("Create Selection");
    }

    private void testMoveToolClick(MoveMode mode, Modifiers modifiers) {
        app.checkNumLayersIs(1);
        boolean duplicated = modifiers.alt().isDown() && mode.movesLayer();
        if (duplicated) {
            app.setMaxUntestedEdits(2);
        }

        mouse.click(modifiers);
        keyboard.undoRedoUndo(mode.getEditName());

        if (duplicated) {
            app.checkNumLayersIs(2);
            keyboard.undoRedoUndo("Duplicate Layer");
            app.checkNumLayersIs(1);
            app.setMaxUntestedEdits(1);
        }
    }

//</editor-fold>
//<editor-fold desc="crop tool">

    private void testCropTool() {
        log(1, "crop tool");

        app.clickTool(Tools.CROP);

        List<Boolean> checkBoxStates = List.of(Boolean.TRUE, Boolean.FALSE);
        for (Boolean allowGrowing : checkBoxStates) {
            for (Boolean deleteCropped : checkBoxStates) {
                pw.checkBox("allowGrowingCB").check(allowGrowing);
                pw.checkBox("deleteCroppedCB").check(deleteCropped);

                cropFromCropTool();
            }
        }

        afterTestActions();
    }

    private void cropFromCropTool() {
        checkCropBoxDoesNotExist();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(400, 400);
        keyboard.undoRedo("Create Crop Box");
        checkCropBoxExists();

        mouse.dragToCanvas(450, 450);
        keyboard.undoRedo("Modify Crop Box");

        mouse.moveToCanvas(200, 200); // move to the top left corner
        mouse.dragToCanvas(150, 150);
        keyboard.undoRedo("Modify Crop Box");

        keyboard.nudge();
        keyboard.undoRedo("Nudge Crop Box");

        mouse.randomAltClick(); // must be at the end, otherwise it tries to start a rectangle

        findButtonByText(pw, "Crop")
            .requireEnabled()
            .click();
        checkCropBoxDoesNotExist();

        keyboard.undoRedoUndo("Crop");
        // undoing the crop restores the crop box
        checkCropBoxExists();

        findButtonByText(pw, "Cancel")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Dismiss Crop Box");

        checkCropBoxDoesNotExist();
    }

    private static void checkCropBoxExists() {
        assert EDT.call(Tools.CROP::hasCropBox);
    }

    private static void checkCropBoxDoesNotExist() {
        assert !EDT.call(Tools.CROP::hasCropBox);
    }

//</editor-fold>
//<editor-fold desc="selections">

    private void testSelectionToolsAndMenus() {
        log(1, "selection tools and the selection menus");
        boolean hadSelectionAtStart = EDT.getActiveSelection() != null;

        // make sure we are at 100%
        keyboard.actualPixels();

        app.clickTool(Tools.RECTANGLE_SELECTION);
        EDT.assertSelectionCombinatorIs(Tools.RECTANGLE_SELECTION, REPLACE);

        mouse.randomAltClick();
        if (hadSelectionAtStart) {
            // if a previous test left a selection, then this click added a deselect edit
            keyboard.undoRedo("Deselect");
        }

        // the Alt should change the interaction only temporarily, while the mouse is down
        EDT.assertSelectionCombinatorIs(Tools.RECTANGLE_SELECTION, REPLACE);

        // TODO test poly selection
        testWithSimpleSelection();
        testWithTwoEllipseSelections();

        afterTestActions();
    }

    private void testWithSimpleSelection() {
        EDT.requireNoSelection();

        mouse.moveToCanvas(200, 100);
        mouse.dragToCanvas(400, 300);

        EDT.requireSelection();

        keyboard.undo("Create Selection");
        EDT.requireNoSelection();

        keyboard.redo("Create Selection");
        EDT.requireSelection();

        keyboard.nudge();
        EDT.requireSelection();

        keyboard.undoRedoUndo("Nudge Selection");
        EDT.requireSelection();

        app.deselect();
        EDT.requireNoSelection();

        keyboard.undo("Deselect");
        EDT.requireSelection();
    }

    private void testWithTwoEllipseSelections() {
        app.clickTool(Tools.ELLIPSE_SELECTION);
        EDT.assertActiveToolIs(Tools.ELLIPSE_SELECTION);

        pw.comboBox("combinatorCB").selectItem("Replace");
        EDT.assertSelectionCombinatorIs(Tools.ELLIPSE_SELECTION, REPLACE);

        // replace current selection with the first ellipse
        int e1X = 200;
        int e1Y = 100;
        int e1Width = 200;
        int e1Height = 200;
        mouse.moveToCanvas(e1X, e1Y);
        mouse.dragToCanvas(e1X + e1Width, e1Y + e1Height);
        keyboard.undoRedo("Replace Selection");
        EDT.requireSelection();

        // add second ellipse
        pw.comboBox("combinatorCB").selectItem("Add");
        EDT.assertSelectionCombinatorIs(Tools.ELLIPSE_SELECTION, ADD);

        int e2X = 400;
        int e2Y = 100;
        int e2Width = 100;
        int e2Height = 100;
        mouse.moveToCanvas(e2X, e2Y);
        mouse.dragToCanvas(e2X + e2Width, e2Y + e2Height);
        keyboard.undoRedo("Add Selection");

        // test crop selection by clicking on the button
        testCropSelection(() -> findButtonByText(pw, "Crop Selection").requireEnabled().click(),
            true, 300.0, 200.0);

        if (!quick) {
            // test crop selection by using the menu
            testCropSelection(() -> app.runMenuCommand("Crop Selection"),
                true, 300.0, 200.0);
        }

        testSelectionModifyMenu();
        EDT.requireSelection();

        app.runMenuCommand("Invert Selection");
        keyboard.undoRedo("Invert Selection");
        EDT.requireSelection();

        app.runMenuCommand("Deselect");
        EDT.requireNoSelection();

        keyboard.undo("Deselect");
        EDT.requireSelection();

        keyboard.redo("Deselect");
        EDT.requireNoSelection();
    }

    private void testCropSelection(Runnable cropTask,
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
        assertThat(EDT.getActiveSelection())
            .isNotNull()
            .isUsable()
            .isMarching();
        EDT.assertCanvasSizeIs(origCanvasWidth, origCanvasHeight);
    }

    private void testSelectionModifyMenu() {
        app.runModifySelection(EXPAND, 24);
        keyboard.undo("Modify Selection");
    }

//</editor-fold>
//<editor-fold desc="zooming">

    private void testZooming() {
        log(1, "zoom tool");

        app.clickTool(Tools.ZOOM);

        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        mouse.moveToActiveCanvasCenter();

        mouse.click();
        EDT.assertActiveZoomIs(startingZoom.zoomIn());
        mouse.click();
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn());
        mouse.click(Modifiers.ALT);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn().zoomOut());
        mouse.click(Modifiers.ALT);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn().zoomOut().zoomOut());

        testMouseWheelZooming();
        testControlPlusMinusZooming();
        testZoomControlAndNavigatorZooming();
        testNavigatorRightClickPopupMenu();
        testAutoZoomButtons();

        afterTestActions();
    }

    private void testControlPlusMinusZooming() {
        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        Keyboard.pressCtrlPlus(pw, 2);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn());

        Keyboard.pressCtrlMinus(pw, 2);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn().zoomOut().zoomOut());
    }

    private void testZoomControlAndNavigatorZooming() {
        var slider = findZoomControlSlider();

        slider.slideToMinimum();
        EDT.assertActiveZoomIs(zoomLevels[0]);

        findButtonByText(pw, "100%").click();
        EDT.assertActiveZoomIs(ZoomLevel.ACTUAL_SIZE);

        slider.slideToMaximum();
        EDT.assertActiveZoomIs(zoomLevels[zoomLevels.length - 1]);

        findButtonByText(pw, "Fit").click();

        app.runMenuCommand("Show Navigator...");
        var navigator = app.findDialogByTitle("Navigator");
        navigator.resizeTo(new Dimension(500, 400));

        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        Keyboard.pressCtrlPlus(navigator, 4);
        ZoomLevel expectedZoomIn = startingZoom.zoomIn().zoomIn().zoomIn().zoomIn();
        EDT.assertActiveZoomIs(expectedZoomIn);

        Keyboard.pressCtrlMinus(navigator, 2);
        ZoomLevel expectedZoomOut = expectedZoomIn.zoomOut().zoomOut();
        EDT.assertActiveZoomIs(expectedZoomOut);
        findButtonByText(pw, "Fit").click();

        // navigate
        int mouseStartX = navigator.target().getWidth() / 2;
        int mouseStartY = navigator.target().getHeight() / 2;

        mouse.moveTo(navigator, mouseStartX, mouseStartY);
        mouse.dragTo(navigator, mouseStartX - 30, mouseStartY + 30);
        mouse.dragTo(navigator, mouseStartX, mouseStartY);

        navigator.close();
        navigator.requireNotVisible();
    }

    private JSliderFixture findZoomControlSlider() {
        return pw.slider(new GenericTypeMatcher<>(JSlider.class) {
            @Override
            protected boolean isMatching(JSlider s) {
                return s.getParent() == ZoomControl.get();
            }

            @Override
            public String toString() {
                return "Matcher for the ZoomControl's slider ";
            }
        });
    }

    private void testNavigatorRightClickPopupMenu() {
        app.runMenuCommand("Show Navigator...");
        var navigator = app.findDialogByTitle("Navigator");
        navigator.resizeTo(new Dimension(500, 400));

        var popupMenu = navigator.showPopupMenu();
        clickPopupMenu(popupMenu, "Navigator Zoom: 100%");

        popupMenu = navigator.showPopupMenu();
        clickPopupMenu(popupMenu, "Navigator Zoom: 50%");

        popupMenu = navigator.showPopupMenu();
        clickPopupMenu(popupMenu, "Navigator Zoom: 25%");

        popupMenu = navigator.showPopupMenu();
        clickPopupMenu(popupMenu, "Navigator Zoom: 12.5%");

        navigator.resizeTo(new Dimension(500, 400));
        popupMenu = navigator.showPopupMenu();
        clickPopupMenu(popupMenu, "View Box Color...");

        testColorSelectorDialog("Navigator");

        navigator.close();
        navigator.requireNotVisible();
    }

    private void testAutoZoomButtons() {
        findButtonByText(pw, "Fit Space").click();
        findButtonByText(pw, "Fit Width").click();
        findButtonByText(pw, "Fit Height").click();
        findButtonByText(pw, "Actual Pixels").click();
    }

    private void testMouseWheelZooming() {
        pw.pressKey(VK_CONTROL);
        ZoomLevel startingZoom = EDT.getActiveZoomLevel();
        View view = EDT.getActiveView();

        robot.rotateMouseWheel(view, 2);
        if (JVM.isLinux) {
            EDT.assertActiveZoomIs(startingZoom.zoomOut().zoomOut());
        } else {
            EDT.assertActiveZoomIs(startingZoom.zoomOut());
        }

        robot.rotateMouseWheel(view, -2);

        if (JVM.isLinux) {
            EDT.assertActiveZoomIs(startingZoom.zoomOut().zoomOut().zoomIn().zoomIn());
        } else {
            EDT.assertActiveZoomIs(startingZoom.zoomOut().zoomIn());
        }

        pw.releaseKey(VK_CONTROL);
    }
//</editor-fold>
//<editor-fold desc="color selector">

    private void testColorSelector() {
        log(1, "color selector");

        app.setDefaultColors();
        app.swapColors();
        app.randomizeColors();

        var fgButton = pw.button(FgBgColorSelector.FG_BUTTON_NAME);
        fgButton.click();
        testColorSelectorDialog(GUIText.FG_COLOR);

        var bgButton = pw.button(FgBgColorSelector.BG_BUTTON_NAME);
        bgButton.click();
        testColorSelectorDialog(GUIText.BG_COLOR);

        if (!quick) {
            testColorSelectorPopup(fgButton, true);
            testColorSelectorPopup(bgButton, false);
        }

        afterTestActions();
    }

    private void testColorSelectorDialog(String title) {
        var colorSelector = app.findDialogByTitle(title);
        mouse.moveTo(colorSelector, 100, 150);
        mouse.dragTo(colorSelector, Rnd.intInRange(110, 200), Rnd.intInRange(160, 300));
        findButtonByText(colorSelector, "OK").click();
    }

    private void testColorSelectorPopup(JButtonFixture button, boolean isFg) {
        testColorPaletteDialogWithPopup(button,
            isFg ? "Foreground Color Variations"
                : "Background Color Variations");

        testColorPaletteDialogWithPopup(button,
            isFg ? "HSB Mix with Background"
                : "HSB Mix with Foreground");

        testColorPaletteDialogWithPopup(button,
            isFg ? "RGB Mix with Background"
                : "RGB Mix with Foreground");

        testColorPaletteDialogWithPopup(button, "Color History");

        clickPopupMenu(button.showPopupMenu(), "Copy Color");
        clickPopupMenu(button.showPopupMenu(), "Paste Color");
    }

    private void testColorPaletteDialogWithPopup(JButtonFixture button, String dialogName) {
        clickPopupMenu(button.showPopupMenu(), dialogName + "...");
        testColorPaletteDialog(dialogName);
    }
//</editor-fold>

    private void addSelection() {
        app.clickTool(Tools.RECTANGLE_SELECTION);
        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(600, 500);
        keyboard.undoRedo("Create Selection");
    }

    private void addTranslation() {
        app.clickTool(Tools.MOVE);
        pw.comboBox("modeSelector").selectItem(MoveMode.MOVE_LAYER_ONLY.toString());

        mouse.moveToCanvas(400, 400);
        mouse.dragToCanvas(200, 300);

        keyboard.undoRedo(MoveMode.MOVE_LAYER_ONLY.getEditName());
    }

    private void runWithSelectionTranslationCombinations(boolean squashedImage, Runnable task) {
        if (squashedImage) {
            task.run();
        } else {
            runWithSelectionTranslationCombinations(task);
        }
    }

    private void runWithSelectionTranslationCombinations(Runnable task) {
        log(1, "without selection or translation");

        EDT.requireNoSelection();
        EDT.assertNoTranslation();

        task.run();

        if (skip(0.5)) {
            return;
        }

        log(1, "with selection, without translation");
        addSelection();
        task.run();
        app.deselect();

        if (skip(0.5)) {
            return;
        }

        log(1, "without selection, with translation");
        addTranslation();
        task.run();

        if (skip(0.5)) {
            keyboard.undo("Move Layer");
            EDT.assertNoTranslation();
            return;
        }

        log(1, "with selection and translation");
        addSelection();
        task.run();

        keyboard.undo("Create Selection");
        EDT.requireNoSelection();

        keyboard.undo("Move Layer");
        EDT.assertNoTranslation();
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

    private void addAdjustmentLayer() {
        pw.button("addAdjLayer").click();
    }

    private static void processCLArguments(String[] args) {
        if (args.length != 1) {
            System.err.println("Required argument: <base testing directory> or \"help\"");
            System.exit(1);
        }
        if (args[0].equals("help")) {
            System.out.println("Test targets: " + Arrays.toString(TestSuite.values()));
            System.out.println("Mask modes: " + Arrays.toString(MaskMode.values()));

            System.exit(0);
        }
        baseDir = new File(args[0]);
        assertThat(baseDir).exists().isDirectory();

        inputDir = new File(baseDir, "input");
        assertThat(inputDir).exists().isDirectory();

        batchResizeOutputDir = new File(baseDir, "batch_resize_output");
        assertThat(batchResizeOutputDir).exists().isDirectory();

        batchFilterOutputDir = new File(baseDir, "batch_filter_output");
        assertThat(batchFilterOutputDir).exists().isDirectory();

        svgOutputDir = new File(baseDir, "svg_output");
        assertThat(svgOutputDir).exists().isDirectory();

        String cleanerScriptExt;
        if (JVM.isWindows) {
            cleanerScriptExt = ".bat";
        } else {
            cleanerScriptExt = ".sh";
        }
        cleanerScript = new File(baseDir + File.separator
            + "0000_clean_outputs" + cleanerScriptExt);

        if (!cleanerScript.exists()) {
            System.err.printf("Cleaner script %s not found.%n", cleanerScript.getName());
            System.exit(1);
        }
    }

    private static void cleanOutputs() {
        try {
            String cleanerScriptPath = cleanerScript.getCanonicalPath();
            Process process = Runtime.getRuntime().exec(new String[]{cleanerScriptPath});
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                throw new IllegalStateException("Exit value for " + cleanerScriptPath + " was " + exitValue);
            }

            assertThat(Files.fileNamesIn(batchResizeOutputDir.getPath(), false)).isEmpty();
            assertThat(Files.fileNamesIn(batchFilterOutputDir.getPath(), false)).isEmpty();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void afterTestActions() {
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

    // decide whether some test(s) should be skipped in quick mode
    private boolean skip() {
        return skip(0.1); // only execute 10% of the time
    }

    private boolean skip(double threshold) {
        if (quick) {
            return random.nextDouble() > threshold;
        } else {
            return false;
        }
    }

    public Keyboard keyboard() {
        return keyboard;
    }

    public AppRunner app() {
        return app;
    }

    private void log(int indent, String msg) {
        for (int i = 0; i < indent; i++) {
            System.out.print("    ");
        }
        String fullMsg = "%s: %s (%s)".formatted(
            getCurrentTimeHM(), msg, maskMode);
        System.out.println(fullMsg);
        EDT.run(() -> PixelitorWindow.get().setTitle(fullMsg));
    }

    private void testRotateFlip(boolean entireComp) {
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
}
