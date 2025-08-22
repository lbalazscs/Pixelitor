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
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JSliderFixture;
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
import pixelitor.guitest.AppRunner.ExpectConfirmation;
import pixelitor.guitest.AppRunner.Randomize;
import pixelitor.guitest.AppRunner.Reseed;
import pixelitor.guitest.AppRunner.ShowOriginal;
import pixelitor.history.History;
import pixelitor.io.*;
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
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.TwoPointPaintType;
import pixelitor.tools.transform.TransformBox;
import pixelitor.utils.Geometry;
import pixelitor.utils.Rnd;
import pixelitor.utils.Texts;
import pixelitor.utils.Utils;

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
import static pixelitor.guitest.AppRunner.clickPopupMenu;
import static pixelitor.guitest.AppRunner.getCurrentTimeHM;
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
 * Assertj-Swing requires using the following VM option:
 * --add-opens java.base/java.util=ALL-UNNAMED
 */
public class MainGuiTest {
    private static boolean quick = false;

    private static File baseDir;
    private static File cleanerScript;

    private static File inputDir;
    private static File batchResizeOutputDir;
    private static File batchFilterOutputDir;

    private final AppRunner app;
    private final Robot robot;
    private final FrameFixture pw;
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

        app = new AppRunner(inputDir, "a.jpg");

        robot = app.getRobot();
        pw = app.getPW();
        keyboard = app.getKeyboard();
        mouse = app.getMouse();

        if (EDT.call(FileChoosers::useNativeDialogs)) {
            // we can't test if mative file choosers are enabled
            System.out.println("MainGuiTest: native dialogs, exiting");
            System.exit(0);
        }

        boolean testOneMethodSlowly = false;
        if (testOneMethodSlowly) {
            app.runSlowly();

            testMagick();
        } else {
            runMenuCommand("Reset Workspace");

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
        pw.toggleButton("Rectangle Selection Tool Button").click();
        pw.comboBox("combinatorCB").selectItem("Replace");
    }

    private void clickAndResetShapesTool() {
        pw.toggleButton("Shapes Tool Button").click();
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
            testZoomTool();
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

        runMenuCommand("Reload");
        maskMode.apply(this);

        checkConsistency();
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

        testLayerMasks();
        testTextLayers();
        testMaskFromColorRange();

        if (Features.enableExperimental) {
            testAdjLayers();
        }

        checkConsistency();
    }

    /**
     * Tests adding a new empty image layer.
     */
    private void testAddLayer() {
        log(1, "add layer");

        app.checkNumLayersIs(1);
        var layer1Button = findLayerButton("layer 1");
        layer1Button.requireSelected();

        var addEmptyLayerButton = pw.button("addLayer");

        // add layer
        addEmptyLayerButton.click();

        app.checkNumLayersIs(2);
        var layer2Button = findLayerButton("layer 2");
        layer2Button.requireSelected();

        keyboard.undo("New Empty Layer");
        app.checkNumLayersIs(1);
        layer1Button.requireSelected();

        keyboard.redo("New Empty Layer");
        app.checkNumLayersIs(2);
        layer2Button.requireSelected();
        maskMode.apply(this);

        app.drawGradient(GradientType.SPIRAL_CW);
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

        app.checkNumLayersIs(2);
        layer2Button.requireSelected();

        // delete layer 2
        pw.button("deleteLayer")
            .requireEnabled()
            .click();
        app.checkNumLayersIs(1);
        layer1Button.requireSelected();

        // undo delete
        keyboard.undo("Delete layer 2");

        app.checkNumLayersIs(2);
        layer2Button = findLayerButton("layer 2");
        layer2Button.requireSelected();

        // redo delete
        keyboard.redo("Delete layer 2");
        app.checkNumLayersIs(1);
        layer1Button.requireSelected();

        maskMode.apply(this);
    }

    private void testDuplicateLayer() {
        log(1, "duplicate layer");

        app.checkNumLayersIs(1);
        pw.button("duplicateLayer").click();

        findLayerButton("layer 1 copy").requireSelected();
        app.checkNumLayersIs(2);
        app.checkLayerNamesAre("layer 1", "layer 1 copy");

        keyboard.undo("Duplicate Layer");
        app.checkNumLayersIs(1);
        findLayerButton("layer 1").requireSelected();

        keyboard.redo("Duplicate Layer");
        app.checkNumLayersIs(2);
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

        runMenuCommand("Lower Layer");
        keyboard.undoRedo("Lower Layer");

        runMenuCommand("Raise Layer");
        keyboard.undoRedo("Raise Layer");

        runMenuCommand("Layer to Bottom");
        keyboard.undoRedo("Layer to Bottom");

        runMenuCommand("Layer to Top");
        keyboard.undoRedo("Layer to Top");
    }

    private void testActiveLayerChangeFromMenu() {
        log(1, "active layer change from menu");

        runMenuCommand("Lower Layer Selection");
        keyboard.undoRedo("Lower Layer Selection");

        runMenuCommand("Raise Layer Selection");
        keyboard.undoRedo("Raise Layer Selection");
    }

    private void testLayerToCanvasSize() {
        log(1, "layer to canvas size");

        // add a translation to make it a big layer,
        // otherwise "layer to canvas size" has no effect
        addTranslation();

        runMenuCommand("Layer to Canvas Size");
        keyboard.undoRedo("Layer to Canvas Size");
    }

    private void testLayerMenusChangingNumLayers() {
        log(1, "layer menus changing the number of layers");

        runMenuCommand("New from Visible");
        keyboard.undoRedo("New Layer from Visible");
        maskMode.apply(this);

        app.mergeDown();

        runMenuCommand("Duplicate Layer");
        keyboard.undoRedo("Duplicate Layer");
        maskMode.apply(this);

        runMenuCommand("New Layer");
        keyboard.undoRedo("New Empty Layer");
        maskMode.apply(this);

        runMenuCommand("Delete Layer");
        keyboard.undoRedo("Delete layer 3");
        maskMode.apply(this);

        runMenuCommand("Flatten Image");
        assertFalse(History.canUndo());
        maskMode.apply(this);
    }

    private void testLayerMasks() {
        log(1, "layer masks");

        boolean allowExistingMask = maskMode != NO_MASK;
        addLayerMask(allowExistingMask);

        testLayerMaskIconPopupMenus();

        deleteLayerMask();

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

        runMenuCommand("Mask from Color Range...");

        var dialog = findDialogByTitle("Mask from Color Range");

        mouse.moveTo(dialog, 100, 100);
        mouse.click();

        dialog.slider("toleranceSlider").slideTo(20);
        dialog.slider("softnessSlider").slideTo(20);
        dialog.checkBox("invertMaskCheckBox").check();
        dialog.comboBox("distMetricCombo").selectItem("RGB");

        dialog.button("ok").click();
        dialog.requireNotVisible();

        if (maskMode == NO_MASK) {
            // delete the created layer mask
            runMenuCommand("Delete");
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

        runMenuCommand("Rasterize Text Layer");
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

        var advDialog = findDialogByTitle("Advanced Text Settings");
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

    void testHelpMenu() {
        if (helpMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(0, "help menu");

        testTipOfTheDay();
        testInternalState();
        testCheckForUpdate();
        testAbout();

        checkConsistency();
        helpMenuTested = true;
    }

    private void testTipOfTheDay() {
        var laf = EDT.call(UIManager::getLookAndFeel);

        runMenuCommand("Tip of the Day");
        var dialog = findDialogByTitle("Tip of the Day");
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
        runMenuCommand("Internal State...");
        var dialog = findDialogByTitle("Internal State");
        findButtonByText(dialog, "Copy as JSON").click();
        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

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

        checkConsistency();
        colorsTested = true;
    }

    private void testColorPaletteMenu(String menuName, String dialogTitle) {
        runMenuCommand(menuName);
        testColorPaletteDialog(dialogTitle);
    }

    private void testColorPaletteDialog(String dialogTitle) {
        var dialog = findDialogByTitle(dialogTitle);
        if (dialogTitle.contains("Foreground")) {
            dialog.resizeTo(new Dimension(500, 500));
        } else {
            dialog.resizeTo(new Dimension(700, 500));
        }
        dialog.close();
        dialog.requireNotVisible();
    }

    private void testCheckForUpdate() {
        runMenuCommand("Check for Updates...");
        try {
            // the title is either "Pixelitor Is Up to Date"
            // or "New Version Available"
            app.findJOptionPane(null).buttonWithText("Close").click();
        } catch (ComponentLookupException e) {
            // if a close button was not found, then it must be the up to date dialog
            app.findJOptionPane("Pixelitor Is Up to Date").okButton().click();
        }
    }

    private void testAbout() {
        runMenuCommand("About Pixelitor");
        var dialog = findDialogByTitle("About Pixelitor");

        var tabbedPane = dialog.tabbedPane();
        tabbedPane.requireTabTitles("About", "Credits", "System Info");
        tabbedPane.selectTab("Credits");
        tabbedPane.selectTab("System Info");
        tabbedPane.selectTab("About");

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    void testEditMenu() {
        log(0, "edit menu");

        keyboard.invert();
        runMenuCommand("Repeat Invert");
        runMenuCommand("Undo Invert");
        runMenuCommand("Redo Invert");
        testFade();

        // select for crop
        clickAndResetRectSelectTool();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(400, 400);
        EDT.assertThereIsSelection();

        testCropSelection(() -> runMenuCommand("Crop Selection"),
            false, 200.0, 200.0);

        EDT.assertThereIsSelection();
        keyboard.deselect();
        EDT.assertThereIsNoSelection();

        testCopyPaste();

        testPreferences();

        checkConsistency();
    }

    private void testFade() {
        // test with own method so that a meaningful opacity can be set
        runMenuCommand("Fade Invert...");
        var dialog = app.findFilterDialog();

        dialog.slider().slideTo(75);

        dialog.checkBox("show original").click();
        dialog.checkBox("show original").click();

        dialog.button("ok").click();

        keyboard.undoRedoUndo("Fade");
    }

    private void testPreferences() {
        if (preferencesTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(1, "preferences dialog");

        runMenuCommand("Preferences...");
        var dialog = findDialogByTitle("Preferences");
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

    void testImageMenu() {
        log(0, "image menu");

        // image from the previous tests
        EDT.assertNumOpenImagesIs(1);
        app.checkNumLayersIs(1);

        // add more layer types
        app.addGradientFillLayer(GradientType.ANGLE);
        app.addColorFillLayer(Color.BLUE);
        app.addShapesLayer(ShapeType.BAT, new CanvasDrag(20, 380, 100));

        testDuplicateImage();

        // crop is tested with the crop tool

        runWithSelectionAndTranslation(() -> {
            testResize();
            testEnlargeCanvas();
            testRotateFlip();
        });

        // delete the 3 extra layers
        pw.button("deleteLayer")
            .requireEnabled()
            .click()
            .click()
            .click();
        app.checkNumLayersIs(1);

        maskMode.apply(this);
        maskMode.check();
        checkConsistency();
    }

    private void testDuplicateImage() {
        int numLayers = EDT.getNumLayersInActiveHolder();
        log(1, "image duplication, num layers = " + numLayers);

        EDT.assertNumOpenImagesIs(1);

        runMenuCommand("Duplicate");
        EDT.assertNumOpenImagesIs(2);
        EDT.assertNumLayersIs(numLayers);

        closeOneOfTwoViews();
        EDT.assertNumOpenImagesIs(1);
    }

    private void testResize() {
        log(2, "resize");
        app.resize(622);

        keyboard.undoRedoUndo("Resize");
    }

    private void testEnlargeCanvas() {
        log(2, "enlarge canvas");
        app.enlargeCanvas(100, 100, 100, 100);
        keyboard.undoRedoUndo("Enlarge Canvas");
    }

    private void testRotateFlip() {
        log(2, "rotate and flip");
        runMenuCommand("Rotate 90° CW");
        keyboard.undoRedoUndo("Rotate 90° CW");

        runMenuCommand("Rotate 180°");
        keyboard.undoRedoUndo("Rotate 180°");

        runMenuCommand("Rotate 90° CCW");
        keyboard.undoRedoUndo("Rotate 90° CCW");

        runMenuCommand("Flip Horizontal");
        keyboard.undoRedoUndo("Flip Horizontal");

        runMenuCommand("Flip Vertical");
        keyboard.undoRedoUndo("Flip Vertical");
    }

    private void testCopyPaste() {
        log(1, "copy-paste");

        EDT.assertNumOpenImagesIs(1);
        app.checkNumLayersIs(1);

        runMenuCommand("Copy Layer/Mask");
        runMenuCommand("Paste as New Layer");

        app.checkNumLayersIs(2);

        runMenuCommand("Copy Composite");
        runMenuCommand("Paste as New Image");
        EDT.assertNumOpenImagesIs(2);

        // close the pasted image
        app.closeCurrentView(ExpectConfirmation.NO);
        EDT.assertNumOpenImagesIs(1);

        // delete the pasted layer
        app.checkNumLayersIs(2);
        assert DeleteActiveLayerAction.INSTANCE.isEnabled();
        runMenuCommand("Delete Layer");
        app.checkNumLayersIs(1);

        maskMode.apply(this);
    }

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
    }

    private void testNewImage() {
        log(1, "new image");

        app.createNewImage(611, 411, null);

        app.closeCurrentView(ExpectConfirmation.NO);
    }

    private void testFileOpen() {
        log(1, "file open");

        runMenuCommand("Open...");
        var openDialog = app.findOpenFileChooser();
        openDialog.cancel();

        openFileWithDialog(inputDir, "b.jpg");

        checkConsistency();
    }

    private void testSave(String extension) {
        log(1, "save, ext = " + extension);

        // create a new image to be saved
        app.createNewImage(400, 400, null);
        maskMode.apply(this);

        // the new image is unsaved => has no file
        assertThat(EDT.active(Composition::getFile)).isNull();

        // new unsaved image, will be saved with a file chooser
        runMenuCommand("Save");
        var saveDialog = app.findSaveFileChooser();

        String fileName = "saved." + extension;
        File file = new File(baseDir, fileName);

        System.out.println("MainGuiTest::testSave: found file chooser, file = " + file);

        boolean fileExistsAlready = file.exists();

        String compName = EDT.active(Composition::getName);

        saveDialog.setCurrentDirectory(baseDir);
        saveDialog.fileNameTextBox()
            .requireText(compName)
            .deleteText()
            .enterText(fileName);
        saveDialog.approve();

        if (fileExistsAlready) {
            // say OK to the overwrite question
            app.findJOptionPane("Confirmation").yesButton().click();
        }
        Utils.sleep(500, MILLISECONDS);
        assertThat(file).exists().isFile();

        System.out.println("MainGuiTest::testSave: run Save, expect no file chooser");

        // now that the file is saved, save again:
        // no file chooser should appear
        runMenuCommand("Save");
        Utils.sleep(500, MILLISECONDS);

        // test "Save As"
        runMenuCommand("Save As...");

        // there is always a dialog for "Save As"
        app.saveWithOverwrite(baseDir, fileName);

        app.closeCurrentView(ExpectConfirmation.NO);

        openFileWithDialog(baseDir, fileName);
        maskMode.apply(this);

        // can be dirty if a masked mask mode is set
        app.closeCurrentView(ExpectConfirmation.UNKNOWN);

        maskMode.apply(this);
        checkConsistency();
    }

    private void testExportOptimizedJPEG() {
        log(1, "testing export optimized jpeg");

        runMenuCommand("Export Optimized JPEG...");

        // wait for the preview to be calculated
        Utils.sleep(2, SECONDS);

        findDialogByTitle("Export Optimized JPEG").button("ok").click();
        app.saveWithOverwrite(baseDir, "saved.jpg");

        checkConsistency();
    }

    private void testMagick() {
        log(1, "testing ImageMagick export-import");

        // test importing
        app.openFileWithDialog("Import...", baseDir, "webp_image.webp");

        // test exporting
        app.runMenuCommand("Export...");
        String exportFileName = "saved_image.webp";
        app.saveWithOverwrite(baseDir, exportFileName);
        app.findJOptionPane("WebP Export Options for " + exportFileName)
            .buttonWithText("Export").click();

        app.closeCurrentView(ExpectConfirmation.NO);
    }

    private void testExportLayerAnimation() {
        log(1, "testing exporting layer animation");

        // precondition: the active image has only 1 layer
        app.checkNumLayersIs(1);

        runMenuCommand("Export Layer Animation...");
        // error dialog, because there is only one layer
        app.findJOptionPane("Not Enough Layers")
            .okButton().click();

        addNewLayer();
        // this time it should work
        runMenuCommand("Export Layer Animation...");
        findDialogByTitle("Export Animated GIF").button("ok").click();

        app.saveWithOverwrite(baseDir, "layeranim.gif");

        checkConsistency();
    }

    private void testExportTweeningAnimation() {
        log(1, "testing export tweening animation");

        EDT.assertNumOpenImagesIsAtLeast(1);

        runMenuCommand("Export Tweening Animation...");
        var dialog = findDialogByTitle("Export Tweening Animation");
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

        checkConsistency();
    }

    private void closeOneOfTwoViews() {
        log(1, "testing close one of two views");

        int numOpenImages = EDT.call(Views::getNumViews);
        if (numOpenImages == 1) {
            app.createNewImage(400, 400, null);
        }

        EDT.assertNumOpenImagesIs(2);

        app.closeCurrentView(ExpectConfirmation.UNKNOWN);

        EDT.assertNumOpenImagesIs(1);

        maskMode.apply(this);
        checkConsistency();
    }

    private void testCloseAll() {
        log(1, "testing close all");

        EDT.assertNumOpenImagesIsAtLeast(1);

        app.closeAll();
        EDT.assertNumOpenImagesIs(0);

        checkConsistency();
    }

    private void testShowMetadata() {
        log(1, "testing show metadata");

        runMenuCommand("Show Metadata...");
        var dialog = findDialogByTitle("Metadata for "
            + EDT.active(Composition::getName));

        dialog.button("expandAllButton").click();
        dialog.button("collapseAllButton").click();

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    private void testBatchResize() {
        log(1, "testing batch resize");
        maskMode.apply(this);

        EDT.run(() -> {
            Dirs.setLastOpen(inputDir);
            Dirs.setLastSave(batchResizeOutputDir);
            FileFormat.setLastSaved(FileFormat.JPG);
        });

        runMenuCommand("Batch Resize...");
        var dialog = findDialogByTitle("Batch Resize");

        dialog.textBox("widthTF").setText("200");
        dialog.textBox("heightTF").setText("200");
        dialog.button("ok").click();
        dialog.requireNotVisible();

        Utils.sleep(5, SECONDS);
        checkConsistency();

        for (File inputFile : FileUtils.listSupportedInputFiles(inputDir)) {
            String fileName = inputFile.getName();

            File outFile = new File(batchResizeOutputDir, fileName);
            assertThat(outFile).exists().isFile();
        }
    }

    private void testBatchFilter() {
        log(1, "testing batch filter");

        Dirs.setLastOpen(inputDir);
        Dirs.setLastSave(batchFilterOutputDir);

        EDT.assertNumOpenImagesIsAtLeast(1);
        maskMode.apply(this);

        runMenuCommand("Batch Filter...");
        var dialog = findDialogByTitle("Batch Filter");
        dialog.textBox("searchTF").enterText("wav");
        dialog.pressKey(VK_DOWN).releaseKey(VK_DOWN)
            .pressKey(VK_DOWN).releaseKey(VK_DOWN);
        dialog.button("ok").click(); // next

        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // start processing
        dialog.requireNotVisible();

        app.waitForProgressMonitorEnd();

        checkConsistency();

        for (File inputFile : FileUtils.listSupportedInputFiles(inputDir)) {
            String fileName = inputFile.getName();

            File outFile = new File(batchFilterOutputDir, fileName);
            assertThat(outFile).exists().isFile();
        }
    }

    private void testExportLayerToPNG() {
        log(1, "testing export layer to png");

        Dirs.setLastSave(baseDir);
        addNewLayer();
        runMenuCommand("Export Layers to PNG...");
        findDialogByTitle("Select Output Folder").button("ok").click();
        Utils.sleep(2, SECONDS);

        checkConsistency();
    }

    void testAutoPaint() {
        log(0, "testing AutoPaint");

        runWithSelectionAndTranslation(this::testAutoPaintTask);

        checkConsistency();
    }

    private void testAutoPaintTask() {
        for (Tool tool : AutoPaint.SUPPORTED_TOOLS) {
            if (skipThis()) {
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
        runMenuCommand("Auto Paint...");
        var dialog = findDialogByTitle("Auto Paint");

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

        checkConsistency();
    }

    private void testScreenCapture(boolean hidePixelitor) {
        runMenuCommand("Screen Capture...");
        var dialog = findDialogByTitle("Screen Capture");
        var cb = dialog.checkBox();
        if (hidePixelitor) {
            cb.check();
        } else {
            cb.uncheck();
        }
        dialog.button("ok").click();
        dialog.requireNotVisible();

        maskMode.apply(this);

        checkConsistency();
    }

    private void testReload() {
        log(1, "testing reload");

        runMenuCommand("Reload");

        // reloading is asynchronous, wait a bit
        IOTasks.waitForIdle();

        keyboard.undoRedo("Reload");
        maskMode.apply(this);

        checkConsistency();
    }

    void testViewMenu() {
        if (viewMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        log(0, "view menu");

        EDT.assertNumOpenImagesIs(1);
        app.checkNumLayersIs(1);

        testZoomCommands();

        testHistory();

        runMenuCommand("Reset Workspace");
        assert EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert EDT.call(LayersContainer::areLayersShown);
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());

        runMenuCommand("Hide Status Bar");
        assert !EDT.call(StatusBar::isShown);

        runMenuCommand("Show Status Bar");
        assert EDT.call(StatusBar::isShown);

        runMenuCommand("Show Histograms");
        assert EDT.call(HistogramsPanel::isShown);

        runMenuCommand("Hide Histograms");
        assert !EDT.call(HistogramsPanel::isShown);

        keyboard.press(VK_F6);
        assert EDT.call(HistogramsPanel::isShown);

        keyboard.press(VK_F6);
        assert !EDT.call(HistogramsPanel::isShown);

        runMenuCommand("Hide Layers");
        assert !EDT.call(LayersContainer::areLayersShown);

        runMenuCommand("Show Layers");
        assert EDT.call(LayersContainer::areLayersShown);

        keyboard.press(VK_F7);
        assert !EDT.call(LayersContainer::areLayersShown);

        keyboard.press(VK_F7);
        assert EDT.call(LayersContainer::areLayersShown);

        runMenuCommand("Hide Tools");
        assert !EDT.call(() -> PixelitorWindow.get().areToolsShown());

        runMenuCommand("Show Tools");
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());

        runMenuCommand("Hide All");
        assert !EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert !EDT.call(LayersContainer::areLayersShown);
        assert !EDT.call(() -> PixelitorWindow.get().areToolsShown());

        runMenuCommand("Restore Workspace");
        assert EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert EDT.call(LayersContainer::areLayersShown);
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());

        testGuides();

        if (ImageArea.isActiveMode(FRAMES)) {
            runMenuCommand("Cascade");
            runMenuCommand("Tile");
        }

        checkConsistency();
        viewMenuTested = true;
    }

    private void testZoomCommands() {
        ZoomLevel startingZoom = EDT.getZoomLevelOfActive();

        runMenuCommand("Zoom In");
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn());

        runMenuCommand("Zoom Out");
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn().zoomOut());

        runMenuCommand("Fit Space");
        runMenuCommand("Fit Width");
        runMenuCommand("Fit Height");

        runMenuCommand("Actual Pixels");
        EDT.assertZoomOfActiveIs(ZoomLevel.ACTUAL_SIZE);
    }

    private void testGuides() {
        runMenuCommand("Add Horizontal Guide...");
        var dialog = findDialogByTitle("Add Horizontal Guide");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getGuides().getHorizontals()).containsExactly(0.5);
        assertThat(EDT.getGuides().getVerticals()).isEmpty();

        runMenuCommand("Add Vertical Guide...");
        dialog = findDialogByTitle("Add Vertical Guide");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getGuides().getHorizontals()).containsExactly(0.5);
        assertThat(EDT.getGuides().getVerticals()).containsExactly(0.5);

        runMenuCommand("Add Grid Guides...");
        dialog = findDialogByTitle("Add Grid Guides");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getGuides().getHorizontals()).containsExactly(0.25, 0.5, 0.75);
        assertThat(EDT.getGuides().getVerticals()).containsExactly(0.25, 0.5, 0.75);

        runMenuCommand("Clear Guides");
        assertThat(EDT.getGuides()).isNull();
    }

    private void testHistory() {
        // before testing make sure that we have something
        // in the history even if this is running alone
        pw.toggleButton("Brush Tool Button").click();
        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();
        pw.toggleButton("Eraser Tool Button").click();
        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();

        // now start testing the history
        runMenuCommand("Show History...");
        var dialog = findDialogByTitle("History");

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

    void testFilters() {
        log(0, "filters");
//        app.setIndexedMode();

        EDT.assertNumOpenImagesIs(1);
        app.checkNumLayersIs(1);

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

        checkConsistency();
    }

    private void testColorFilters(boolean squashedImage) {
        testColorBalance(squashedImage);
        testFilterWithDialog(HueSat.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(Colorize.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(Levels.NAME, Randomize.NO, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(BrightnessContrast.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(Solarize.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(Sepia.NAME, Randomize.NO, Reseed.NO, ShowOriginal.YES);
        testInvert(squashedImage);
        testFilterWithDialog("Channel Invert", Randomize.NO, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(ChannelMixer.NAME, Randomize.YES,
            Reseed.NO, ShowOriginal.YES, "Swap Red-Green", "Swap Red-Blue", "Swap Green-Blue",
            "R -> G -> B -> R", "R -> B -> G -> R",
            "Average BW", "Luminosity BW", "Sepia");
        testFilterWithDialog("Equalize", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Extract Channel", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testNoDialogFilter("Luminosity");
        testNoDialogFilter("Value = max(R,G,B)");
        testNoDialogFilter(ExtractChannelFilter.DESATURATE_NAME);
        testNoDialogFilter(GUIText.HUE);
        testNoDialogFilter("Hue (with colors)");
        testNoDialogFilter(ExtractChannelFilter.SATURATION_NAME);
        testFilterWithDialog("Quantize", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(Posterize.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(Threshold.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Color Threshold", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Tritone", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Gradient Map", Randomize.NO, Reseed.NO, ShowOriginal.YES);
        testNoDialogFilter(GUIText.FG_COLOR);
        testNoDialogFilter(GUIText.BG_COLOR);
        testNoDialogFilter("Transparent");
        testFilterWithDialog("Color Wheel", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Four Color Gradient", Randomize.YES, Reseed.NO, ShowOriginal.NO);
    }

    private void testBlurSharpenFilters() {
        testFilterWithDialog("Box Blur", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Focus", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Gaussian Blur", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Lens Blur", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Motion Blur", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Smart Blur", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Spin and Zoom Blur", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Unsharp Mask", Randomize.YES, Reseed.NO, ShowOriginal.YES);
    }

    private void testDistortFilters() {
        testFilterWithDialog("Swirl, Pinch, Bulge", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Circle to Square", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Perspective", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Lens Over Image", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Magnify", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Turbulent Distortion", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Underwater", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Water Ripple", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Waves", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Angular Waves", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Radial Waves", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Glass Tiles", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Polar Glass Tiles", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Frosted Glass", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog(LittlePlanet.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(JHPolarCoordinates.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Wrap Around Arc", Randomize.YES, Reseed.NO, ShowOriginal.YES);
    }

    private void testDisplaceFilters() {
        testFilterWithDialog("Displacement Map", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Drunk Vision", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Grid Kaleidoscope", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(JHKaleidoscope.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog(Mirror.NAME, Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Offset", Randomize.NO, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Slice", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Tile Seamless", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Video Feedback", Randomize.YES, Reseed.NO, ShowOriginal.YES);
    }

    private void testLightFilters() {
        testFilterWithDialog("Bump Map", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Flashlight", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Glint", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Glow", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Rays", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Sparkle", Randomize.YES, Reseed.YES, ShowOriginal.YES);
    }

    private void testNoiseFilters() {
        testFilterWithDialog("Kuwahara", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testNoDialogFilter("Reduce Single Pixel Noise");
        testNoDialogFilter("3x3 Median Filter");
        testFilterWithDialog("Add Noise", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Pixelate", Randomize.YES, Reseed.NO, ShowOriginal.YES);
    }

    private void testRenderFilters() {
        testFilterWithDialog("Clouds", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Plasma", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Value Noise", Randomize.YES, Reseed.YES, ShowOriginal.NO);

        testFilterWithDialog("Abstract Lights", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Brushed Metal", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Caustics", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Cells", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Flow Field", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Marble", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Voronoi Diagram", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Wood", Randomize.YES, Reseed.YES, ShowOriginal.NO);

        // Curves
        testFilterWithDialog("Circle Weave", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Flower of Life", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("L-Systems", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Grid", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Lissajous Curve", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Spider Web", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Spiral", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Spirograph", Randomize.YES, Reseed.NO, ShowOriginal.NO);

        // Fractals
        testFilterWithDialog("Chaos Game", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Fractal Tree", Randomize.YES, Reseed.YES, ShowOriginal.NO);
        testFilterWithDialog("Julia Set", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Mandelbrot Set", Randomize.YES, Reseed.NO, ShowOriginal.NO);

        // Geometry
        testFilterWithDialog("Border Mask", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Concentric Shapes", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Checker Pattern", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Cubes Pattern", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Penrose Tiling", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Rose", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Starburst", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Stripes", Randomize.YES, Reseed.NO, ShowOriginal.NO);
        testFilterWithDialog("Truchet Tiles", Randomize.YES, Reseed.NO, ShowOriginal.NO);
    }

    private void testArtisticFilters() {
        testFilterWithDialog("Comic Book", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Crystallize", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Pointillize", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Stamp", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Oil Painting", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Spheres", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Smear", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Emboss", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Orton Effect", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Photo Collage", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Weave", Randomize.YES, Reseed.NO, ShowOriginal.YES);

        testFilterWithDialog("Dots Halftone", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Striped Halftone", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Concentric Halftone", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Color Halftone", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Ordered Dithering", Randomize.YES, Reseed.NO, ShowOriginal.YES);
    }

    private void testFindEdgesFilters() {
        testFilterWithDialog("Canny", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Convolution Edge Detection", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Difference of Gaussians", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testNoDialogFilter("Laplacian");
    }

    private void testGMICFilters() {
        app.resize(200);

        // Artistic
        testFilterWithDialog("Bokeh", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Box Fitting", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Brushify", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Cubism", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Huffman Glitches", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Random 3D Objects", Randomize.YES, Reseed.YES, ShowOriginal.YES);
        testFilterWithDialog("Rodilius", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Voronoi", Randomize.YES, Reseed.YES, ShowOriginal.YES);

        // Blur/Sharpen
        testFilterWithDialog("Anisothropic Smoothing", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Bilateral Smoothing", Randomize.YES, Reseed.NO, ShowOriginal.YES);

        testFilterWithDialog("G'MIC Command", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Light Glow", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Local Normalization", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Stroke", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Vibrance", Randomize.YES, Reseed.NO, ShowOriginal.YES);

        // the image was reduced in size at the start of the GMIC filter tests
        app.closeCurrentView(ExpectConfirmation.YES);
        app.openFileWithDialog("Open...", inputDir, "a.jpg");
        maskMode.apply(this);
    }

    private void testOtherFilters() {
        testFilterWithDialog("Drop Shadow", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Morphology", Randomize.YES, Reseed.NO, ShowOriginal.YES);
//        testRandomFilter();
        testText();
        testFilterWithDialog("Transform Layer", Randomize.YES, Reseed.NO, ShowOriginal.YES);

        testConvolution();

        testFilterWithDialog("Channel to Transparency", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testNoDialogFilter("Invert Transparency");
    }

    private void testConvolution() {
        testFilterWithDialog("Custom 3x3 Convolution", Randomize.NO,
            Reseed.NO, ShowOriginal.NO, "Corner Blur", "\"Gaussian\" Blur", "Mean Blur", "Sharpen",
            "Edge Detection", "Edge Detection 2", "Horizontal Edge Detection",
            "Vertical Edge Detection", "Emboss", "Emboss 2", "Color Emboss",
            "Reset", "Randomize");
        testFilterWithDialog("Custom 5x5 Convolution", Randomize.NO,
            Reseed.NO, ShowOriginal.NO, "Diamond Blur", "Motion Blur",
            "Find Horizontal Edges", "Find Vertical Edges",
            "Find / Edges", "Find \\ Edges", "Sharpen",
            "Reset", "Randomize");
    }

    private void testTransitionsFilters() {
        testFilterWithDialog("2D Transitions", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Blinds Transition", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Checkerboard Transition", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Goo Transition", Randomize.YES, Reseed.NO, ShowOriginal.YES);
        testFilterWithDialog("Shapes Grid Transition", Randomize.YES, Reseed.NO, ShowOriginal.YES);
    }

    private void testColorBalance(boolean squashedImage) {
        runWithSelectionAndTranslation(squashedImage, () ->
            testFilterWithDialog(ColorBalance.NAME,
                Randomize.YES, Reseed.NO, ShowOriginal.YES));
    }

    private void testInvert(boolean squashedImage) {
        runWithSelectionAndTranslation(squashedImage, () ->
            testNoDialogFilter(Invert.NAME));
    }

    private void testText() {
        if (skipThis()) {
            return;
        }
        log(1, "filter Text");

        runMenuCommand("Text...");
        var dialog = app.findFilterDialog();

        testTextDialog(dialog, textFilterTested ?
            "my text" : TextSettings.DEFAULT_TEXT);

        findButtonByText(dialog, "OK").click();
        afterFilterRunActions("Text");

        textFilterTested = true;
    }

    private void testRandomFilter() {
        log(1, "random filter");

        runMenuCommand("Random Filter...");
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
        if (skipThis()) {
            return;
        }
        log(1, "filter " + name);

        runMenuCommand(name);

        afterFilterRunActions(name);
    }

    private void testFilterWithDialog(String name,
                                      Randomize randomize,
                                      Reseed reseed,
                                      ShowOriginal showOriginal,
                                      String... extraButtonsToClick) {
        if (skipThis()) {
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

        app.runFilterWithDialog(name, randomize, reseed, showOriginal, testPresets, extraButtonClicker);

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

    private void stressTestFilterWithDialog(String name, Randomize randomize, Reseed reseed, boolean resizeToSmall) {
        if (resizeToSmall) {
            app.resize(200);
            runMenuCommand("Zoom In");
            runMenuCommand("Zoom In");
        }

        String nameWithoutDots = name.substring(0, name.length() - 3);
        log(1, "filter " + nameWithoutDots);

        runMenuCommand(name);
        var dialog = app.findFilterDialog();

        int max = 1000;
        for (int i = 0; i < max; i++) {
            System.out.println("MainGuiTest stress testing " + nameWithoutDots + ": " + (i + 1) + " of " + max);
            if (randomize == Randomize.YES) {
                findButtonByText(dialog, "Randomize Settings").click();
            }
            if (reseed == Reseed.YES) {
                findButtonByText(dialog, "Reseed").click();
                findButtonByText(dialog, "Reseed").click();
                findButtonByText(dialog, "Reseed").click();
            }
        }

        dialog.button("ok").click();
    }

    private void testHandTool() {
        log(1, "hand tool");

        app.clickTool(Tools.HAND);

        mouse.randomAltClick();

        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();

        testAutoZoomButtons();

        checkConsistency();
    }

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

        setupEffectsDialog();
        mouse.moveToCanvas(50, 50);
        mouse.dragToCanvas(150, 100);
        keyboard.undoRedo("Create Shape");

        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.FOREGROUND.toString());
        keyboard.undoRedo("Change Shape Stroke");
        setupStrokeSettingsDialog();
        keyboard.undoRedo("Change Shape Stroke Settings");

        mouse.moveToCanvas(200, 50);
        mouse.dragToCanvas(300, 100);
        pw.comboBox("shapeTypeCB").selectItem(ShapeType.CAT.toString());
        keyboard.undoRedo("Change Shape Type");

        // resize the transform box by the SE handle
        mouse.moveToCanvas(300, 100);
        mouse.dragToCanvas(500, 300);
        keyboard.undoRedo("Change Transform Box");

        ShapeType[] shapeTypes = ShapeType.values();
        for (ShapeType shapeType : shapeTypes) {
            if (shapeType == ShapeType.CAT || skipThis()) {
                continue;
            }
            pw.comboBox("shapeTypeCB").selectItem(shapeType.toString());
            keyboard.undoRedo("Change Shape Type");
        }

        for (TwoPointPaintType paintType : TwoPointPaintType.values()) {
            if (skipThis()) {
                continue;
            }
            pw.comboBox("fillPaintCB").selectItem(paintType.toString());
            pw.comboBox("strokePaintCB").selectItem(paintType.toString());
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
        keyboard.deselect();

        checkConsistency();
    }

    private void setupEffectsDialog() {
        findButtonByText(pw, "Effects...")
            .requireEnabled()
            .click();

        var dialog = findDialogByTitle("Effects");
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

        dialog.checkBox("enabledCB").check();

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    private void setupStrokeSettingsDialog() {
        findButtonByText(pw, "Stroke Settings...")
            .requireEnabled()
            .click();
        var dialog = findDialogByTitle("Stroke Settings");

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

    private void testColorPickerTool() {
        log(1, "color picker tool");

        app.clickTool(Tools.COLOR_PICKER);

        mouse.randomAltClick();

        mouse.moveToCanvas(300, 300);
        pw.click();
        mouse.dragToCanvas(400, 400);

        checkConsistency();
    }

    private void testPathTools() {
        testPenTool();
        testNodeTool();
        testTransformPathTool();

        checkConsistency();
    }

    private void testPenTool() {
        log(1, "pen tool");
        app.clickTool(Tools.PEN);

        pw.button("toSelectionButton").requireDisabled();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(200, 400);
        mouse.moveToCanvas(300, 400);
        mouse.dragToCanvas(300, 200);

        mouse.moveToCanvas(200, 200);
        mouse.click();

        assertThat(EDT.getActivePath())
            .isNotNull()
            .numSubPathsIs(1)
            .numAnchorsIs(2);

        keyboard.undo("Close Subpath");
        keyboard.undo("Add Anchor Point");
        keyboard.undo("Subpath Start");

        assertThat(EDT.getActivePath())
            .isNull();

        keyboard.redo("Subpath Start");
        keyboard.redo("Add Anchor Point");
        keyboard.redo("Close Subpath");

        // add a second subpath, this one will be open and
        // consists of straight segments
        mouse.clickCanvas(600, 200);
        mouse.clickCanvas(600, 300);
        mouse.clickCanvas(700, 300);
        mouse.clickCanvas(700, 200);
        mouse.ctrlClickCanvas(700, 150);

        assertThat(EDT.getActivePath())
            .isNotNull()
            .numSubPathsIs(2)
            .numAnchorsIs(6);
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

        keyboard.invert();

        pw.button("toPathButton")
            .requireEnabled()
            .click();
        EDT.assertActiveToolIs(Tools.NODE);
        assertThat(EDT.getActivePath()).isNotNull();

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

    private void testTransformPathTool() {
        log(1, "transform path tool");
        app.clickTool(Tools.TRANSFORM_PATH);

        Point nw = EDT.getTransformPathToolBoxPos(0, TransformBox::getNW);
        mouse.moveToScreen(nw.x, nw.y);
        mouse.dragToScreen(nw.x - 100, nw.y - 50);

        Point rot = EDT.getTransformPathToolBoxPos(1, TransformBox::getRot);
        mouse.moveToScreen(rot.x, rot.y);
        mouse.dragToScreen(rot.x + 100, rot.y + 100);

        pw.button("traceWithBrush")
            .requireEnabled()
            .click();
    }

    private void testPaintBucketTool() {
        log(1, "paint bucket tool");

        app.clickTool(Tools.PAINT_BUCKET);

        mouse.randomAltClick();

        mouse.moveToCanvas(300, 300);
        pw.click();

        keyboard.undoRedoUndo("Paint Bucket Tool");
        checkConsistency();
    }

    private void testGradientTool() {
        log(1, "gradient tool");

        app.clickTool(Tools.GRADIENT);

        if (maskMode.isMaskEditing()) {
            // reset the default colors, otherwise it might be all gray
            keyboard.fgBgDefaults();
        }

        mouse.randomAltClick();
        boolean gradientCreated = false;

        for (GradientType gradientType : GradientType.values()) {
            pw.comboBox("typeCB").selectItem(gradientType.toString());
            for (String cycleMethod : GradientTool.CYCLE_METHODS) {
                pw.comboBox("cycleMethodCB").selectItem(cycleMethod);
                GradientColorType[] gradientColorTypes = GradientColorType.values();
                for (GradientColorType colorType : gradientColorTypes) {
                    if (skipThis()) {
                        continue;
                    }
                    pw.comboBox("colorTypeCB").selectItem(colorType.toString());

                    GUITestUtils.checkRandomly(pw.checkBox("reverseCB"));

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
        }
        checkConsistency();
    }

    private void testEraserTool() {
        log(1, "eraser tool");

        app.clickTool(Tools.ERASER);

        testBrushStrokes(Tools.ERASER);

        checkConsistency();
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

        checkConsistency();
    }

    private void enableLazyMouse(boolean b) {
        pw.button("lazyMouseDialogButton")
            .requireEnabled()
            .click();
        var dialog = findDialogByTitle("Lazy Mouse Settings");
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

        boolean tested = false;
        for (BrushType brushType : BrushType.values()) {
            pw.comboBox("typeCB").selectItem(brushType.toString());
            app.testBrushSettings(tool, brushType);

            for (Symmetry symmetry : Symmetry.values()) {
                if (skipThis()) {
                    continue;
                }
                pw.comboBox("symmetrySelector").selectItem(symmetry.toString());
                keyboard.randomizeColors();
                mouse.moveRandomlyWithinCanvas();
                mouse.dragRandomlyWithinCanvas();
                tested = true;
            }
        }
        if (tested) {
            keyboard.undoRedo(tool == Tools.BRUSH ? "Brush Tool" : "Eraser Tool");
        }
    }

    private void testSmudgeTool() {
        log(1, "smudge tool");

        app.clickTool(Tools.SMUDGE);

        mouse.randomAltClick();

        for (int i = 0; i < 3; i++) {
            mouse.randomClick();
            mouse.shiftMoveClickRandom();
            mouse.moveRandomlyWithinCanvas();
            mouse.dragRandomlyWithinCanvas();
        }

        keyboard.undoRedo("Smudge Tool");

        checkConsistency();
    }

    private void testCloneTool() {
        log(1, "clone tool");

        app.clickTool(Tools.CLONE);

        testClone(false, false, 100);
        testClone(false, true, 200);
        testClone(true, false, 300);
        testClone(true, true, 400);

        checkConsistency();
    }

    private void testClone(boolean aligned, boolean sampleAllLayers, int startX) {
        selectCheckBox("alignedCB", aligned);

        selectCheckBox("sampleAllLayersCB", sampleAllLayers);

        // set the source point
        mouse.moveToCanvas(300, 300);
        mouse.altClick();

        // do some cloning
        mouse.moveToCanvas(startX, 300);
        for (int i = 1; i <= 5; i++) {
            int x = startX + i * 10;
            mouse.dragToCanvas(x, 300);
            mouse.dragToCanvas(x, 400);
        }
        keyboard.undoRedo("Clone Stamp Tool");
    }

    private void testSelectionToolsAndMenus() {
        log(1, "selection tools and the selection menus");

        // make sure we are at 100%
        keyboard.actualPixels();

        app.clickTool(Tools.RECTANGLE_SELECTION);
        EDT.assertSelectionCombinatorIs(Tools.RECTANGLE_SELECTION, REPLACE);

        mouse.randomAltClick();
        // the Alt should change the interaction only temporarily,
        // while the mouse is down
        EDT.assertSelectionCombinatorIs(Tools.RECTANGLE_SELECTION, REPLACE);

        // TODO test poly selection
        testWithSimpleSelection();
        testWithTwoEclipseSelections();
    }

    private void testWithSimpleSelection() {
        EDT.assertThereIsNoSelection();

        mouse.moveToCanvas(200, 100);
        mouse.dragToCanvas(400, 300);
        EDT.assertThereIsSelection();

        keyboard.nudge();
        EDT.assertThereIsSelection();

        keyboard.undo("Nudge Selection");
        EDT.assertThereIsSelection();

        keyboard.redo("Nudge Selection");
        EDT.assertThereIsSelection();

        keyboard.undo("Nudge Selection");
        EDT.assertThereIsSelection();

        keyboard.deselect();
        EDT.assertThereIsNoSelection();

        keyboard.undo("Deselect");
        EDT.assertThereIsSelection();
    }

    private void testWithTwoEclipseSelections() {
        app.clickTool(Tools.ELLIPSE_SELECTION);
        EDT.assertActiveToolIs(Tools.ELLIPSE_SELECTION);

        // replace current selection with the first ellipse
        int e1X = 200;
        int e1Y = 100;
        int e1Width = 200;
        int e1Height = 200;
        mouse.moveToCanvas(e1X, e1Y);
        mouse.dragToCanvas(e1X + e1Width, e1Y + e1Height);
        EDT.assertThereIsSelection();

        // add second ellipse
        pw.comboBox("combinatorCB").selectItem("Add");
        EDT.assertSelectionCombinatorIs(Tools.ELLIPSE_SELECTION, ADD);

        int e2X = 400;
        int e2Y = 100;
        int e2Width = 100;
        int e2Height = 100;
        mouse.moveToCanvas(e2X, e2Y);
        mouse.dragToCanvas(e2X + e2Width, e2Y + e2Height);

        // test crop selection by clicking on the button
        testCropSelection(() -> findButtonByText(pw, "Crop Selection").requireEnabled().click(),
            true, 300.0, 200.0);

        if (!quick) {
            // test crop selection by using the menu
            testCropSelection(() -> runMenuCommand("Crop Selection"),
                true, 300.0, 200.0);
        }

        testSelectionModifyMenu();
        EDT.assertThereIsSelection();

        runMenuCommand("Invert Selection");
        EDT.assertThereIsSelection();

        runMenuCommand("Deselect");
        EDT.assertThereIsNoSelection();
    }

    private void testCropSelection(Runnable triggerTask,
                                   boolean assumeNonRectangular,
                                   double expectedSelWidth,
                                   double expectedSelHeight) {
        EDT.assertThereIsSelection();

        Selection selection = EDT.getActiveSelection();
        boolean rectangular = selection.getShape() instanceof Rectangle2D;
        assert rectangular == !assumeNonRectangular;

        Rectangle2D selectionBounds = selection.getShapeBounds2D();
        double selWidth = selectionBounds.getWidth();
        double selHeight = selectionBounds.getHeight();

        // the values can be off by one due to rounding errors
        assertThat(selWidth).isCloseTo(expectedSelWidth, within(2.0));
        assertThat(selHeight).isCloseTo(expectedSelHeight, within(2.0));

        Canvas canvas = EDT.active(Composition::getCanvas);
        int origCanvasWidth = canvas.getWidth();
        int origCanvasHeight = canvas.getHeight();

        triggerTask.run();

        if (rectangular) {
            undoRedoUndoSimpleSelectionCrop(origCanvasWidth, origCanvasHeight, selWidth, selHeight);
            return;
        }

        // not rectangular: test choosing "Only Crop"
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Only Crop").click();
        undoRedoUndoSimpleSelectionCrop(origCanvasWidth, origCanvasHeight, selWidth, selHeight);

        if (skipThis(0.5)) {
            return;
        }

        // not rectangular: test choosing "Only Hide"
        triggerTask.run();
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Only Hide").click();

        EDT.assertThereIsNoSelection();
        EDT.assertCanvasSizeIs(origCanvasWidth, origCanvasHeight);
        assert EDT.activeLayerHasMask();

        keyboard.undoRedoUndo("Add Hiding Mask");

        if (skipThis(0.5)) {
            return;
        }

        // not rectangular: test choosing "Crop and Hide"
        triggerTask.run();
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Crop and Hide").click();
        checkAfterSelectionCrop(selWidth, selHeight);
        assert EDT.activeLayerHasMask();

        keyboard.undoRedoUndo("Crop and Hide");

        if (skipThis(0.5)) {
            return;
        }

        // not rectangular: test choosing "Cancel"
        triggerTask.run();
        app.findJOptionPane("Non-Rectangular Selection Crop")
            .buttonWithText("Cancel").click();

        EDT.assertThereIsSelection();
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
        EDT.assertThereIsNoSelection();
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

        keyboard.undoRedoUndo("Modify Selection");
    }

    private void testCropTool() {
        log(1, "crop tool");

        app.clickTool(Tools.CROP);

        List<Boolean> checkBoxStates = List.of(Boolean.TRUE, Boolean.FALSE);
        for (Boolean allowGrowing : checkBoxStates) {
            for (Boolean deleteCropped : checkBoxStates) {
                selectCheckBox("allowGrowingCB", allowGrowing);
                selectCheckBox("deleteCroppedCB", deleteCropped);

                cropFromCropTool();
            }
        }

        checkConsistency();
    }

    private void cropFromCropTool() {
        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(400, 400);
        mouse.dragToCanvas(450, 450);
        mouse.moveToCanvas(200, 200); // move to the top left corner
        mouse.dragToCanvas(150, 150);

        keyboard.nudge();
        // currently there is no undo after resizing or nudging the crop rectangle

        mouse.randomAltClick(); // must be at the end, otherwise it tries to start a rectangle

        findButtonByText(pw, "Crop")
            .requireEnabled()
            .click();
        keyboard.undoRedoUndo("Crop");
    }

    private void testMoveTool() {
        log(1, "move tool");

        addSelection(); // so that the moving of the selection can be tested
        app.clickTool(Tools.MOVE);

        MoveMode[] moveModes = MoveMode.values();
        for (MoveMode mode : moveModes) {
            pw.comboBox("modeSelector").selectItem(mode.toString());

            testMoveToolImpl(mode, false);
            testMoveToolImpl(mode, true);
            app.checkNumLayersIs(1);

            keyboard.nudge();
            keyboard.undoRedoUndo(mode.getEditName());
            app.checkNumLayersIs(1);
        }

        // check that all move-related edits have been undone
        EDT.assertEditToBeUndoneNameIs("Create Selection");

        mouse.click();
        mouse.ctrlClick();
        app.checkNumLayersIs(1);

        mouse.altClick(); // this duplicates the layer
        app.checkNumLayersIs(2);
        pw.button("deleteLayer")
            .requireEnabled()
            .click();
        app.checkNumLayersIs(1);
        maskMode.apply(this);

        checkConsistency();
    }

    private void testMoveToolImpl(MoveMode mode, boolean altDrag) {
        mouse.moveToCanvas(400, 400);

        if (altDrag) {
            mouse.altDragToCanvas(300, 300);
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

        if (altDrag && mode != MoveMode.MOVE_SELECTION_ONLY) {
            // The alt-dragged movement creates two history edits:
            // a duplicate and a layer move. Now also undo the duplication.
            keyboard.undo("Duplicate Layer");
        }

        // check that all move-related edits have been undone
        EDT.assertEditToBeUndoneNameIs("Create Selection");
    }

    private void testZoomTool() {
        log(1, "zoom tool");

        app.clickTool(Tools.ZOOM);

        ZoomLevel startingZoom = EDT.getZoomLevelOfActive();

        mouse.moveToActiveCanvasCenter();

        mouse.click();
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn());
        mouse.click();
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn().zoomIn());
        mouse.altClick();
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn().zoomIn().zoomOut());
        mouse.altClick();
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn().zoomIn().zoomOut().zoomOut());

        testMouseWheelZooming();
        testControlPlusMinusZooming();
        testZoomControlAndNavigatorZooming();
        testNavigatorRightClickPopupMenu();
        testAutoZoomButtons();

        checkConsistency();
    }

    private void testControlPlusMinusZooming() {
        ZoomLevel startingZoom = EDT.getZoomLevelOfActive();

        Keyboard.pressCtrlPlus(pw, 2);
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn().zoomIn());

        Keyboard.pressCtrlMinus(pw, 2);
        EDT.assertZoomOfActiveIs(startingZoom.zoomIn().zoomIn().zoomOut().zoomOut());
    }

    private void testZoomControlAndNavigatorZooming() {
        var slider = findZoomControlSlider();

        slider.slideToMinimum();
        EDT.assertZoomOfActiveIs(zoomLevels[0]);

        findButtonByText(pw, "100%").click();
        EDT.assertZoomOfActiveIs(ZoomLevel.ACTUAL_SIZE);

        slider.slideToMaximum();
        EDT.assertZoomOfActiveIs(zoomLevels[zoomLevels.length - 1]);

        findButtonByText(pw, "Fit").click();

        runMenuCommand("Show Navigator...");
        var navigator = findDialogByTitle("Navigator");
        navigator.resizeTo(new Dimension(500, 400));

        ZoomLevel startingZoom = EDT.getZoomLevelOfActive();

        Keyboard.pressCtrlPlus(navigator, 4);
        ZoomLevel expectedZoomIn = startingZoom.zoomIn().zoomIn().zoomIn().zoomIn();
        EDT.assertZoomOfActiveIs(expectedZoomIn);

        Keyboard.pressCtrlMinus(navigator, 2);
        ZoomLevel expectedZoomOut = expectedZoomIn.zoomOut().zoomOut();
        EDT.assertZoomOfActiveIs(expectedZoomOut);
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
        runMenuCommand("Show Navigator...");
        var navigator = findDialogByTitle("Navigator");
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
        ZoomLevel startingZoom = EDT.getZoomLevelOfActive();
        View view = EDT.getActiveView();

        robot.rotateMouseWheel(view, 2);
        if (JVM.isLinux) {
            EDT.assertZoomOfActiveIs(startingZoom.zoomOut().zoomOut());
        } else {
            EDT.assertZoomOfActiveIs(startingZoom.zoomOut());
        }

        robot.rotateMouseWheel(view, -2);

        if (JVM.isLinux) {
            EDT.assertZoomOfActiveIs(startingZoom.zoomOut().zoomOut().zoomIn().zoomIn());
        } else {
            EDT.assertZoomOfActiveIs(startingZoom.zoomOut().zoomIn());
        }

        pw.releaseKey(VK_CONTROL);
    }

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
    }

    private void testColorSelectorDialog(String title) {
        var colorSelector = findDialogByTitle(title);
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

    private static void cleanOutputs() {
        try {
            String cleanerScriptPath = cleanerScript.getCanonicalPath();
            System.out.println("MainGuiTest::cleanOutputs: running " + cleanerScript);
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

    private void addSelection() {
        pw.toggleButton("Rectangle Selection Tool Button").click();
        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(600, 500);
    }

    private void addTranslation() {
        pw.toggleButton("Move Tool Button").click();
        mouse.moveToCanvas(400, 400);
        mouse.click();
        mouse.dragToCanvas(200, 300);
    }

    private void runWithSelectionAndTranslation(boolean squashedImage, Runnable task) {
        if (squashedImage) {
            task.run();
        } else {
            runWithSelectionAndTranslation(task);
        }
    }

    private void runWithSelectionAndTranslation(Runnable task) {
        log(1, "simple test");
        keyboard.deselect();
        task.run();

        if (skipThis(0.5)) {
            return;
        }

        log(1, "test with selection");
        addSelection();
        task.run();
        keyboard.deselect();

        if (skipThis(0.5)) {
            return;
        }

        log(1, "test with translation");
        addTranslation();
        task.run();

        if (skipThis(0.5)) {
            return;
        }

        log(1, "test with selection+translation");
        addSelection();
        task.run();
        keyboard.undo("Create Selection");
        keyboard.undo("Move Layer");
    }

    public void addLayerMask(boolean allowExistingMask) {
        if (EDT.activeLayerHasMask()) {
            pw.button("addLayerMask").requireDisabled();
            if (!allowExistingMask) {
                throw new IllegalStateException("already has mask");
            }
        } else {
            app.addLayerMask();
            app.drawGradient(GradientType.RADIAL);
        }
    }

    void deleteLayerMask() {
        runMenuCommand("Delete");
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

    private boolean skipThis() {
        // in quick mode only execute 10% of the repetitive tests
        return skipThis(0.1);
    }

    private boolean skipThis(double threshold) {
        if (quick) {
            return random.nextDouble() > threshold;
        } else {
            return false;
        }
    }

    public Keyboard keyboard() {
        return keyboard;
    }

    private void addNewLayer() {
        int numLayers = EDT.active(Composition::getNumLayers);
        runMenuCommand("Duplicate Layer");
        app.checkNumLayersIs(numLayers + 1);
        keyboard.invert();
        maskMode.apply(this);
    }

    private void selectCheckBox(String name, boolean newSelected) {
        if (newSelected) {
            pw.checkBox(name).check();
        } else {
            pw.checkBox(name).uncheck();
        }
    }

    private void runMenuCommand(String text) {
        app.runMenuCommand(text);
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

    private DialogFixture findDialogByTitle(String title) {
        return app.findDialogByTitle(title);
    }
}
