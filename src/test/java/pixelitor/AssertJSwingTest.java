/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor;

import com.bric.util.JVM;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.ComponentContainerFixture;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JCheckBoxFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.fixture.JMenuItemFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.launcher.ApplicationLauncher;
import org.fest.util.Files;
import pixelitor.automate.AutoPaint;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.io.FileChoosers;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.LayerButton;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.Selection;
import pixelitor.testutils.WithSelection;
import pixelitor.testutils.WithTranslation;
import pixelitor.tools.BrushType;
import pixelitor.tools.GradientColorType;
import pixelitor.tools.GradientTool;
import pixelitor.tools.GradientType;
import pixelitor.tools.ShapeType;
import pixelitor.tools.ShapesAction;
import pixelitor.tools.Symmetry;
import pixelitor.tools.Tool;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import static java.awt.event.KeyEvent.VK_0;
import static java.awt.event.KeyEvent.VK_ADD;
import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_I;
import static java.awt.event.KeyEvent.VK_MINUS;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_Z;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static pixelitor.utils.test.Assertions.canvasSizeIs;
import static pixelitor.utils.test.Assertions.hasSelection;
import static pixelitor.utils.test.Assertions.noSelection;
import static pixelitor.utils.test.Assertions.numLayersIs;


public class AssertJSwingTest {
    private static final File BASE_TESTING_DIR = new File("C:\\pix_tests");
    private static final File INPUT_DIR = new File(BASE_TESTING_DIR, "input");
    private static final File BATCH_RESIZE_OUTPUT_DIR = new File(BASE_TESTING_DIR, "batch_resize_output");
    private static final File BATCH_FILTER_OUTPUT_DIR = new File(BASE_TESTING_DIR, "batch_filter_output");

    private Robot robot;
    private static final int ROBOT_DELAY_MILLIS = 100;

    private FrameFixture window;
    private final Random random = new Random();

    enum Randomize {YES, NO}

    enum ShowOriginal {YES, NO}

    public static void main(String[] args) {
        initialize();
        AssertJSwingTest test = new AssertJSwingTest();
        test.setUp();
        test.testApp();

        System.out.println("AssertJSwingTest::main: finished, exiting in 5 seconds");
        Utils.sleep(5, SECONDS);
        test.exit();
    }

    private static void initialize() {
        cleanOutputs();
        checkTestingDirs();
        Utils.checkThatAssertionsAreEnabled();
    }

    public void setUp() {
        robot = BasicRobot.robotWithNewAwtHierarchy();
        robot.settings().delayBetweenEvents(ROBOT_DELAY_MILLIS);

        ApplicationLauncher
                .application("pixelitor.Pixelitor")
                .withArgs((new File(INPUT_DIR, "a.jpg")).getPath())
                .start();

        window = WindowFinder.findFrame("frame0")
                .withTimeout(15, SECONDS)
                .using(robot);
        PixelitorWindow.getInstance().setLocation(0, 0);
    }

    private void testApp() {
        testDevelopMenu();
        testTools();
        testMenus();
        testLayers();
    }

    private void testDevelopMenu() {
    }

    private void testTools() {
        // make sure we have a big internal frame for the tool tests
        keyboardActualPixels();

        testMoveTool();
        testCropTool();
        testSelectionToolAndMenus();
        testBrushTool();
        testCloneTool();
        testEraserTool();
        testSmudgeTool();
        testGradientTool();
        testPaintBucketTool();
        testColorPickerTool();
        testShapesTool();
        testHandTool();
        testZoomTool();
    }

    private void testMenus() {
        testFileMenu();
        testEditMenu();
        testFilters();
        testZoomCommands();
        testViewCommands();
        testHelpMenu();
    }

    private void testLayers() {
        LayerButtonFixture layer1Button = findLayerButton("layer 1");
        layer1Button.requireSelected();

        JButtonFixture addEmptyLayerButton = findButtonByActionName(window, "Add New Layer");
        JButtonFixture deleteLayerButton = findButtonByActionName(window, "Delete Layer");
        JButtonFixture duplicateLayerButton = findButtonByActionName(window, "Duplicate Layer");

        addEmptyLayerButton.click();
        LayerButtonFixture layer2Button = findLayerButton("layer 2");
        layer2Button.requireSelected();

        deleteLayerButton.click();
        duplicateLayerButton.click();
        LayerButtonFixture layer1CopyButton = findLayerButton("layer 1 copy");
        layer1CopyButton.requireSelected();

        layer1CopyButton.setOpenEye(false);

        findMenuItemByText("Lower Layer").click();
        findMenuItemByText("Raise Layer").click();
        findMenuItemByText("Layer to Bottom").click();
        findMenuItemByText("Layer to Top").click();
        findMenuItemByText("Lower Layer Selection").click();
        findMenuItemByText("Raise Layer Selection").click();

        // doesn't do much
        findMenuItemByText("Layer to Canvas Size").click();

        findMenuItemByText("New Layer from Composite").click();
        findMenuItemByText("Duplicate Layer").click();
        findMenuItemByText("Merge Down").click();
        findMenuItemByText("Duplicate Layer").click();

        findMenuItemByText("Add New Layer").click();
        findMenuItemByText("Delete Layer").click();

        findMenuItemByText("Flatten Image").click();
    }

    private LayerButtonFixture findLayerButton(String layerName) {
        return new LayerButtonFixture(robot, robot.finder().find(new GenericTypeMatcher<LayerButton>(LayerButton.class) {
            @Override
            protected boolean isMatching(LayerButton layerButton) {
                return layerButton.getLayerName().equals(layerName);
            }
        }));
    }

    private void testHelpMenu() {
        testTipOfTheDay();
        testCheckForUpdate();
        testAbout();
    }

    private void testTipOfTheDay() {
        findMenuItemByText("Tip of the Day").click();
        DialogFixture dialog = findDialogByTitle("Tip of the Day");
        findButtonByText(dialog, "Next >").click();
        findButtonByText(dialog, "Next >").click();
        findButtonByText(dialog, "< Back").click();
        findButtonByText(dialog, "Close").click();
    }

    private void testCheckForUpdate() {
        findMenuItemByText("Check for Update...").click();
        try {
            findJOptionPane().cancelButton().click();
        } catch (org.assertj.swing.exception.ComponentLookupException e) {
            // can happen if the current version is the same as the latest
            findJOptionPane().okButton().click();
        }
    }

    private void testAbout() {
        findMenuItemByText("About").click();
        DialogFixture aboutDialog = findDialogByTitle("About Pixelitor");

        JTabbedPaneFixture tabbedPane = aboutDialog.tabbedPane();
        tabbedPane.requireTabTitles("About", "Credits", "System Info");
        tabbedPane.selectTab("Credits");
        tabbedPane.selectTab("System Info");
        tabbedPane.selectTab("About");

        aboutDialog.button("ok").click();
    }

    private void exit() {
        String exitMenuName = JVM.isMac ? "Quit" : "Exit";
        runMenuCommand(exitMenuName);
        findJOptionPane().yesButton().click();
    }

    private void testEditMenu() {
        keyboardInvert();
        runMenuCommand("Repeat Invert");
        runMenuCommand("Undo Invert");
        runMenuCommand("Redo Invert");
        testFilterWithDialog("Fade Invert", Randomize.NO, ShowOriginal.YES);

        // select for crop
        window.toggleButton("Selection Tool Button").click();
        moveTo(200, 200);
        dragTo(400, 400);
        runMenuCommand("Crop");
        keyboardUndoRedoUndo();
        keyboardDeselect();

        testCopyPaste();

        testMultiLayerEdits();

        testPreferences();
    }

    private void testPreferences() {
        runMenuCommand("Preferences...");
        DialogFixture d = findDialogByTitle("Preferences");
        d.button("ok").click();
    }

    private void testMultiLayerEdits() {
        // crop is also a multilayer edit, but it is tested with the crop tool

        WithTranslation[] translationSettings =
                {WithTranslation.NO, WithTranslation.YES};
        WithSelection[] selectionSettings =
                {WithSelection.NO, WithSelection.YES};

        for (WithTranslation translation : translationSettings) {
            for (WithSelection selection : selectionSettings) {
                testResize(selection, translation);
                testEnlargeCanvas(selection, translation);
                testRotateFlip(selection, translation);
            }
        }
    }

    private void testResize(WithSelection withSelection, WithTranslation withTranslation) {
        if (withSelection == WithSelection.YES) {
            addSelection();
        } else {
            keyboardDeselect();
        }

        if (withTranslation == WithTranslation.YES) {
            addTranslation();
        }

        runMenuCommand("Resize...");
        DialogFixture resizeDialog = findDialogByTitle("Resize");

        JTextComponentFixture widthTF = resizeDialog.textBox("widthTF");
        widthTF.deleteText().enterText("622");

        // no need to also set the height, because
        // constrain proportions is checked by default

        resizeDialog.button("ok").click();

        keyboardUndoRedoUndo();

        if (withTranslation == WithTranslation.YES) {
            keyboardUndo(); // undo translation
        }

        keyboardDeselect();
    }

    private void testEnlargeCanvas(WithSelection withSelection, WithTranslation withTranslation) {
        if (withSelection == WithSelection.YES) {
            addSelection();
        } else {
            keyboardDeselect();
        }

        if (withTranslation == WithTranslation.YES) {
            addTranslation();
        }

        runMenuCommand("Enlarge Canvas...");
        DialogFixture enlargeDialog = findDialogByTitle("Enlarge Canvas");

        enlargeDialog.slider("north").slideTo(100);
        enlargeDialog.slider("west").slideTo(100);
        enlargeDialog.slider("east").slideTo(100);
        enlargeDialog.slider("south").slideTo(100);

        enlargeDialog.button("ok").click();

        keyboardUndoRedoUndo();

        if (withTranslation == WithTranslation.YES) {
            keyboardUndo(); // undo translation
        }
        keyboardDeselect();
    }

    private void testRotateFlip(WithSelection withSelection, WithTranslation withTranslation) {
        if (withSelection == WithSelection.YES) {
            addSelection();
        } else {
            keyboardDeselect();
        }

        if (withTranslation == WithTranslation.YES) {
            addTranslation();
        }

        runMenuCommand("Rotate 90° CW");
        keyboardUndoRedoUndo();

        runMenuCommand("Rotate 180°");
        keyboardUndoRedoUndo();

        runMenuCommand("Rotate 90° CCW");
        keyboardUndoRedoUndo();

        runMenuCommand("Flip Horizontal");
        keyboardUndoRedoUndo();

        runMenuCommand("Flip Vertical");
        keyboardUndoRedoUndo();

        if (withTranslation == WithTranslation.YES) {
            keyboardUndo(); // undo translation
        }
        keyboardDeselect();
    }

    private void testCopyPaste() {
        runMenuCommand("Copy Layer");
        runMenuCommand("Paste as New Layer");
        runMenuCommand("Copy Composite");
        runMenuCommand("Paste as New Image");
    }

    private void testFileMenu() {
        testNewImage();
        testSaveUnnamed();
        testClose();
        testFileOpen();
        testClose();
        testExportOptimizedJPEG();
        testExportOpenRaster();
        testExportLayerAnimation();
        testExportTweeningAnimation();
        testBatchResize();
        testBatchFilter();
        testExportLayerToPNG();
        testAutoPaint();
        testScreenCapture();
        testCloseAll();
    }

    private void testNewImage() {
        findMenuItemByText("New Image...").click();
        DialogFixture newImageDialog = findDialogByTitle("New Image");
        newImageDialog.textBox("widthTF").deleteText().enterText("611");
        newImageDialog.textBox("heightTF").deleteText().enterText("411");
        newImageDialog.button("ok").click();
    }

    private void testFileOpen() {
        findMenuItemByText("Open...").click();
        JFileChooserFixture openDialog = JFileChooserFinder.findFileChooser("open").using(robot);
        openDialog.cancel();

        findMenuItemByText("Open...").click();
        openDialog = JFileChooserFinder.findFileChooser("open").using(robot);
        openDialog.selectFile(new File(INPUT_DIR, "b.jpg"));
        openDialog.approve();
    }

    private void testSaveUnnamed() {
        // new unsaved image, will be saved as save as
        runMenuCommand("Save");
        JFileChooserFixture saveDialog = findSaveFileChooser();
        // due to an assertj bug, the file must exist - TODO investigate, report
        saveDialog.selectFile(new File(BASE_TESTING_DIR, "saved.png"));
        saveDialog.approve();
        // say OK to the overwrite question
        findJOptionPane().yesButton().click();

        // TODO test save as menuitem and simple save (without file chooser)
    }

    private void testExportOptimizedJPEG() {
        runMenuCommand("Export Optimized JPEG...");
        findDialogByTitle("Save Optimized JPEG").button("ok").click();
        saveWithOverwrite("saved.png");
    }

    private void testExportOpenRaster() {
        // precondition: the active image has only 1 layer
        assert numLayersIs(1);

        runMenuCommand("Export OpenRaster...");
        findJOptionPane().noButton().click(); // don't save

        runMenuCommand("Export OpenRaster...");
        findJOptionPane().yesButton().click(); // save anyway
        findDialogByTitle("Export OpenRaster").button("ok").click(); // save with default settings
        saveWithOverwrite("saved.ora");

        // TODO test multi-layer save
    }

    private void testExportLayerAnimation() {
        // precondition: the active image has only 1 layer
        assert numLayersIs(1);

        runMenuCommand("Export Layer Animation...");
        findJOptionPane().okButton().click();
        addNewLayer();

        // this time it should work
        runMenuCommand("Export Layer Animation...");
        findDialogByTitle("Export Animated GIF").button("ok").click();

        saveWithOverwrite("layeranim.gif");
    }

    private void testExportTweeningAnimation() {
        assertThat(ImageComponents.getActiveComp().isPresent()).isTrue();
        runMenuCommand("Export Tweening Animation...");
        DialogFixture dialog = findDialogByTitle("Export Tweening Animation");
        dialog.comboBox().selectItem("Angular Waves");
        dialog.button("ok").click(); // next
        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // next
        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // next
        dialog.button("ok").click(); // render

        // say OK to the folder not empty question
        JOptionPaneFixture optionPane = findJOptionPane();
        optionPane.yesButton().click();

        waitForProgressMonitorEnd();
    }

    private void testClose() {
        assertThat(ImageComponents.getNrOfOpenImages()).isEqualTo(2);

        runMenuCommand("Close");

        assertThat(ImageComponents.getNrOfOpenImages()).isEqualTo(1);
    }

    private void testCloseAll() {
        assertThat(ImageComponents.getNrOfOpenImages() > 1);

        // save for the next test
        runMenuCommand("Copy Composite");

        runMenuCommand("Close All");

        // close all warnings
        boolean warnings = true;
        while (warnings) {
            try {
                JOptionPaneFixture pane = findJOptionPane();
                // click "Don't Save"
                pane.button(new GenericTypeMatcher<JButton>(JButton.class) {
                    @Override
                    protected boolean isMatching(JButton button) {
                        return button.getText().equals("Don't Save");
                    }
                }).click();
            } catch (Exception e) { // no more JOptionPane found
                warnings = false;
            }
        }

        assertThat(ImageComponents.getNrOfOpenImages()).isEqualTo(0);

        // restore for the next test
        runMenuCommand("Paste as New Image");
    }

    private void testBatchResize() {
        FileChoosers.setLastOpenDir(INPUT_DIR);
        FileChoosers.setLastSaveDir(BATCH_RESIZE_OUTPUT_DIR);
        runMenuCommand("Batch Resize...");
        DialogFixture dialog = findDialogByTitle("Batch Resize");

        dialog.textBox("widthTF").setText("200");
        dialog.textBox("heightTF").setText("200");
        dialog.button("ok").click();
    }

    private void testBatchFilter() {
        FileChoosers.setLastOpenDir(INPUT_DIR);
        FileChoosers.setLastSaveDir(BATCH_FILTER_OUTPUT_DIR);

        assertThat(ImageComponents.getActiveComp().isPresent()).isTrue();
        runMenuCommand("Batch Filter...");
        DialogFixture dialog = findDialogByTitle("Batch Filter");
        dialog.comboBox("filtersCB").selectItem("Angular Waves");
        dialog.button("ok").click(); // next
        Utils.sleep(3, SECONDS);
        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // start processing

        waitForProgressMonitorEnd();
    }

    private void testExportLayerToPNG() {
        FileChoosers.setLastSaveDir(BASE_TESTING_DIR);
        addNewLayer();
        runMenuCommand("Export Layers to PNG...");
        findDialogByTitle("Select Output Folder").button("ok").click();
        Utils.sleep(2, SECONDS);
    }

    private void testAutoPaint() {
        for (Tool tool : AutoPaint.ALLOWED_TOOLS) {
            testAutoPaintWithTool(tool);
        }
    }

    private void testAutoPaintWithTool(Tool tool) {
        runMenuCommand("Auto Paint...");
        DialogFixture dialog = findDialogByTitle("Auto Paint");

        JComboBoxFixture toolSelector = dialog.comboBox("toolSelector");
        toolSelector.selectItem(tool.toString());

        JTextComponentFixture numStrokesTF = dialog.textBox("numStrokesTF");
        String testNumStrokes = "111";
        if (!numStrokesTF.text().equals(testNumStrokes)) {
            numStrokesTF.deleteText();
            numStrokesTF.enterText(testNumStrokes);
        }

        dialog.button("ok").click();
    }

    private void testScreenCapture() {
        ImageDisplay activeIC = ImageComponents.getActiveIC();
        testScreenCapture(true);
        testScreenCapture(false);
        try {
            SwingUtilities.invokeAndWait(() -> ImageComponents.setActiveIC(activeIC, true));
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void testScreenCapture(boolean hidePixelitor) {
        runMenuCommand("Screen Capture...");
        DialogFixture dialog = findDialogByTitle("Screen Capture");
        JCheckBoxFixture cb = dialog.checkBox();
        if (hidePixelitor) {
            cb.check();
        } else {
            cb.uncheck();
        }
        dialog.button("ok").click();
    }

    private void testViewCommands() {
        runMenuCommand("Set Default Workspace");
        runMenuCommand("Hide Status Bar");
        runMenuCommand("Show Status Bar");
        runMenuCommand("Show Histograms");
        runMenuCommand("Hide Histograms");
        runMenuCommand("Hide Layers");
        runMenuCommand("Show Layers");

        runMenuCommand("Hide Tools");
        runMenuCommand("Show Tools");

        runMenuCommand("Hide All");
        runMenuCommand("Show Hidden");

        runMenuCommand("Cascade");
        runMenuCommand("Tile");
    }

    private void testZoomCommands() {
        runMenuCommand("Zoom In");
        runMenuCommand("Zoom Out");
        runMenuCommand("Actual Pixels");
        runMenuCommand("Fit Screen");

        ZoomLevel[] values = ZoomLevel.values();
        for (ZoomLevel zoomLevel : values) {
            runMenuCommand(zoomLevel.toString());
        }
    }

    private void testFilters() {
        testFilterWithDialog("Color Balance...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Hue/Saturation...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Colorize...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Levels...", Randomize.NO, ShowOriginal.YES);
        testFilterWithDialog("Brightness/Contrast...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Solarize...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Sepia...", Randomize.NO, ShowOriginal.YES);
        testNoDialogFilter("Invert");
        testFilterWithDialog("Channel Invert...", Randomize.NO, ShowOriginal.YES);
        testFilterWithDialog("Channel Mixer...", Randomize.YES,
                ShowOriginal.YES, "Swap Red-Green", "Swap Red-Blue", "Swap Green-Blue",
                "R -> G -> B -> R", "R -> B -> G -> R",
                "Average BW", "Luminosity BW", "Sepia",
                "Normalize", "Randomize and Normalize");
        testFilterWithDialog("Extract Channel...", Randomize.YES, ShowOriginal.YES);
        testNoDialogFilter("Luminosity");
        testNoDialogFilter("Value = max(R,G,B)");
        testNoDialogFilter("Desaturate");
        testNoDialogFilter("Hue");
        testNoDialogFilter("Hue (with colors)");
        testNoDialogFilter("Saturation");
        testFilterWithDialog("Quantize...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Posterize...", Randomize.NO, ShowOriginal.YES);
        testFilterWithDialog("Threshold...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Tritone...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Gradient Map...", Randomize.NO, ShowOriginal.YES);
        testFilterWithDialog("Color Halftone...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Dither...", Randomize.YES, ShowOriginal.YES);
        testNoDialogFilter("Foreground Color");
        testNoDialogFilter("Background Color");
        testNoDialogFilter("Transparent");
        testFilterWithDialog("Color Wheel...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Four Color Gradient...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Starburst...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Gaussian Blur...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Smart Blur...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Box Blur...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Fast Blur...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Lens Blur...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Motion Blur...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Spin and Zoom Blur...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Unsharp Mask...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Swirl, Pinch, Bulge...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Circle to Square...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Perspective...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Lens Over Image...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Magnify...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Turbulent Distortion...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Underwater...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Water Ripple...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Waves...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Angular Waves...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Radial Waves...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Glass Tiles...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Polar Glass Tiles...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Frosted Glass...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Little Planet...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Polar Coordinates...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Wrap Around Arc...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Kaleidoscope...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Video Feedback...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Offset...", Randomize.NO, ShowOriginal.YES);
        testFilterWithDialog("Slice...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Mirror...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Glow...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Sparkle...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Rays...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Glint...", Randomize.YES, ShowOriginal.YES);
        testNoDialogFilter("Reduce Single Pixel Noise");
        testNoDialogFilter("3x3 Median Filter");
        testFilterWithDialog("Add Noise...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Pixelate...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Clouds...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Value Noise...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Caustics...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Plasma...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Wood...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Cells...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Brushed Metal...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Voronoi Diagram...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Fractal Tree...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Mystic Rose...", Randomize.YES, ShowOriginal.NO);
        testFilterWithDialog("Crystallize...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Pointillize...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Stamp...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Oil Painting...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Random Spheres...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Smear...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Emboss...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Orton Effect...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Photo Collage...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Weave...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Convolution Edge Detection...", Randomize.YES, ShowOriginal.YES);
        testNoDialogFilter("Laplacian");
        testFilterWithDialog("Difference of Gaussians...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Canny Edge Detector...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("Drop Shadow...", Randomize.YES, ShowOriginal.YES);
        testFilterWithDialog("2D Transitions...", Randomize.YES, ShowOriginal.YES);

        testFilterWithDialog("Custom 3x3 Convolution...", Randomize.NO,
                ShowOriginal.NO, "\"Corner\" Blur", "\"Gaussian\" Blur", "Mean Filter", "Sharpen",
                "Edge Detection", "Edge Detection 2", "Horizontal Edge Detection",
                "Vertical Edge Detection", "Emboss", "Emboss 2", "Color Emboss",
                "Do Nothing", "Randomize");
        testFilterWithDialog("Custom 5x5 Convolution...", Randomize.NO,
                ShowOriginal.NO, "Diamond Blur", "Motion Blur", "Find Horizontal Edges", "Find Vertical Edges",
                "Find Diagonal Edges", "Find Diagonal Edges 2", "Sharpen",
                "Do Nothing", "Randomize");

        testRandomFilter();
        testFilterWithDialog("Transform Layer...", Randomize.YES, ShowOriginal.YES);

        testFilterWithDialog("Channel to Transparency...", Randomize.YES, ShowOriginal.YES);


        testText();
    }

    private void testText() {
        findMenuItemByText("Text...").click();
        DialogFixture dialog = WindowFinder.findDialog("filterDialog").using(robot);

        dialog.textBox("textTF").requireEditable().enterText("testing...");
        dialog.slider("fontSize").slideTo(250);

        dialog.checkBox("boldCB").check().uncheck();
        dialog.checkBox("italicCB").check();
//        dialog.checkBox("underlineCB").check().uncheck();
//        dialog.checkBox("strikeThroughCB").check().uncheck();
// TODO test the advanced settings dialog

        findButtonByText(dialog, "OK").click();
        keyboardUndoRedoUndo();
    }

    private void testRandomFilter() {
        findMenuItemByText("Random Filter...").click();
        DialogFixture dialog = WindowFinder.findDialog("filterDialog").using(robot);
        JButtonFixture nextRandomButton = findButtonByText(dialog, "Next Random Filter");
        JButtonFixture backButton = findButtonByText(dialog, "Back");
        JButtonFixture forwardButton = findButtonByText(dialog, "Forward");

        assertThat(nextRandomButton.isEnabled()).isTrue();
        assertThat(backButton.isEnabled()).isFalse();
        assertThat(forwardButton.isEnabled()).isFalse();

        nextRandomButton.click();
        assertThat(backButton.isEnabled()).isTrue();
        assertThat(forwardButton.isEnabled()).isFalse();

        nextRandomButton.click();
        backButton.click();
        assertThat(forwardButton.isEnabled()).isTrue();

        backButton.click();
        forwardButton.click();
        nextRandomButton.click();

        findButtonByText(dialog, "OK").click();
        keyboardUndoRedoUndo();
    }

    private void runMenuCommand(String text) {
        findMenuItemByText(text).click();
    }

    private void testNoDialogFilter(String name) {
        runMenuCommand(name);

        keyboardUndoRedoUndo();
    }

    private void testFilterWithDialog(String name, Randomize randomize, ShowOriginal showOriginal, String... extraButtonsToClick) {
        findMenuItemByText(name).click();
        DialogFixture dialog = WindowFinder.findDialog("filterDialog").using(robot);

        for (String buttonText : extraButtonsToClick) {
            findButtonByText(dialog, buttonText).click();
        }

        if (randomize == Randomize.YES) {
            findButtonByText(dialog, "Randomize Settings").click();
            findButtonByText(dialog, "Reset All").click();
            findButtonByText(dialog, "Randomize Settings").click();
        }

        if (showOriginal == ShowOriginal.YES) {
            dialog.checkBox("show original").click();
            dialog.checkBox("show original").click();
        }

        dialog.button("ok").click();

        keyboardUndoRedoUndo();
    }

    private void testHandTool() {
        window.toggleButton("Hand Tool Button").click();
        randomAltClick();

        moveRandom();
        dragRandom();
    }

    private void testShapesTool() {
        window.toggleButton("Shapes Tool Button").click();
        randomAltClick();

        setupEffectsDialog();
        boolean stokeSettingsSetup = false;

        for (ShapeType shapeType : ShapeType.values()) {
            window.comboBox("shapeTypeCB").selectItem(shapeType.toString());
            for (ShapesAction shapesAction : ShapesAction.values()) {
                window.comboBox("actionCB").selectItem(shapesAction.toString());
                window.pressAndReleaseKeys(KeyEvent.VK_R);

                if (shapesAction == ShapesAction.STROKE) { // stroke settings will be enabled here
                    if (!stokeSettingsSetup) {
                        setupStrokeSettingsDialog();
                        stokeSettingsSetup = true;
                    }
                }

                moveRandom();
                dragRandom();

                if (shapesAction == ShapesAction.SELECTION || shapesAction == ShapesAction.SELECTION_FROM_STROKE) {
                    keyboardDeselect();
                }
            }
        }

        keyboardUndoRedoUndo();
    }

    private void setupEffectsDialog() {
        findButtonByText(window, "Effects...").click();

        DialogFixture dialog = findDialogByTitle("Effects");
        JTabbedPaneFixture tabbedPane = dialog.tabbedPane();
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
    }

    private void setupStrokeSettingsDialog() {
        findButtonByText(window, "Stroke Settings...").click();
        Utils.sleep(1, SECONDS);
        DialogFixture dialog = findDialogByTitle("Stroke Settings");

        dialog.slider().slideTo(20);

        dialog.button("ok").click();
    }

    private void testColorPickerTool() {
        window.toggleButton("Color Picker Tool Button").click();
        randomAltClick();

        moveTo(300, 300);
        window.click();
        dragTo(400, 400);
    }

    private void testPaintBucketTool() {
        window.toggleButton("Paint Bucket Tool Button").click();
        randomAltClick();

        moveTo(300, 300);
        window.click();

        keyboardUndoRedoUndo();
    }

    private void testGradientTool() {
        window.toggleButton("Gradient Tool Button").click();
        randomAltClick();

        for (GradientType gradientType : GradientType.values()) {
            window.comboBox("gradientTypeSelector").selectItem(gradientType.toString());
            for (String cycleMethod : GradientTool.CYCLE_METHODS) {
                window.comboBox("gradientCycleMethodSelector").selectItem(cycleMethod);
                GradientColorType[] gradientColorTypes = GradientColorType.values();
                for (GradientColorType colorType : gradientColorTypes) {
                    window.comboBox("gradientColorTypeSelector").selectItem(colorType.toString());
                    window.checkBox("gradientInvert").uncheck();
                    moveTo(200, 200);
                    dragTo(400, 400);
                    window.checkBox("gradientInvert").check();
                    moveTo(200, 200);
                    dragTo(400, 400);
                }
            }
        }
        keyboardUndoRedo();
    }

    private void testEraserTool() {
        window.toggleButton("Eraser Tool Button").click();
        testBrushStrokes();
    }

    private void testBrushTool() {
        window.toggleButton("Brush Tool Button").click();
        testBrushStrokes();
    }

    private void testBrushStrokes() {
        randomAltClick();

        for (BrushType brushType : BrushType.values()) {
            window.comboBox("brushTypeSelector").selectItem(brushType.toString());
            for (Symmetry symmetry : Symmetry.values()) {
                window.comboBox("symmetrySelector").selectItem(symmetry.toString());
                window.pressAndReleaseKeys(KeyEvent.VK_R);
                moveRandom();
                dragRandom();
            }
        }
        keyboardUndoRedo();
    }

    private void testSmudgeTool() {
        window.toggleButton("Smudge Tool Button").click();
        randomAltClick();

        for (int i = 0; i < 3; i++) {
            randomClick();
            shiftMoveClickRandom();
            moveRandom();
            dragRandom();
        }
    }

    private void testCloneTool() {
        window.toggleButton("Clone Stamp Tool Button").click();

        testClone(false, false, 100);
        testClone(false, true, 200);
        testClone(true, false, 300);
        testClone(true, true, 400);
    }

    private void testClone(boolean aligned, boolean sampleAllLayers, int startX) {
        if (aligned) {
            window.checkBox("alignedCB").check();
        } else {
            window.checkBox("alignedCB").uncheck();
        }

        if (sampleAllLayers) {
            window.checkBox("sampleAllLayersCB").check();
        } else {
            window.checkBox("sampleAllLayersCB").uncheck();
        }

        moveTo(300, 300);

        altClick();

        moveTo(startX, 300);
        for (int i = 1; i <= 5; i++) {
            int x = startX + i * 10;
            dragTo(x, 300);
            dragTo(x, 400);
        }
        keyboardUndoRedo();
    }

    private void testSelectionToolAndMenus() {
        // make sure we are at 100%
        keyboardActualPixels();

        window.toggleButton("Selection Tool Button").click();
        randomAltClick();

        testWithSimpleSelection();
        testWithTwoEclipseSelections();
    }

    private void testWithSimpleSelection() {
        moveTo(200, 200);
        dragTo(400, 400);
        keyboardNudge();
        keyboardUndoRedoUndo();

        //window.button("brushTraceButton").click();
        findButtonByText(window, "Stroke with Current Brush").click();

        keyboardDeselect();
        keyboardUndo(); // keyboardUndo deselection
        keyboardUndo(); // keyboardUndo tracing
    }

    private void testWithTwoEclipseSelections() {
        window.comboBox("selectionTypeCombo").selectItem("Ellipse");

        // replace current selection with the first ellipse
        int e1X = 200;
        int e1Y = 200;
        int e1Width = 200;
        int e1Height = 200;
        moveTo(e1X, e1Y);
        dragTo(e1X + e1Width, e1Y + e1Height);

        // add second ellipse
        window.comboBox("selectionInteractionCombo").selectItem("Add");
        int e2X = 400;
        int e2Y = 200;
        int e2Width = 100;
        int e2Height = 100;
        moveTo(e2X, e2Y);
        dragTo(e2X + e2Width, e2Y + e2Height);

        Composition comp = ImageComponents.getActiveCompOrNull();
        Canvas canvas = comp.getCanvas();
        int origCanvasWidth = canvas.getWidth();
        int origCanvasHeight = canvas.getHeight();
        assert canvasSizeIs(origCanvasWidth, origCanvasHeight);

        assert hasSelection();
        Selection selection = comp.getSelectionOrNull();
        Rectangle selectionBounds = selection.getShapeBounds();
        int selectionWidth = selectionBounds.width;
        int selectionHeight = selectionBounds.height;

        // the eclipses in screen coordinates
        Ellipse2D e1 = new Ellipse2D.Double(e1X, e1Y, e1Width, e1Height);
        Ellipse2D e2 = new Ellipse2D.Double(e2X, e2Y, e2Width, e2Height);
        Area area = new Area(e1);
        area.add(new Area(e2));
        Rectangle areaBounds = area.getBounds();
        // the x and y are relative tyo the screen, but width and height are absolute
        int expectedSelectionWidth = areaBounds.width;
        int expectedSelectionHeight = areaBounds.height;
        assertEquals("selection width", expectedSelectionWidth, selectionWidth);
        assertEquals("selection height", expectedSelectionHeight, selectionHeight);

        //window.button("eraserTraceButton").click();
        findButtonByText(window, "Stroke with Current Eraser").click();

        // crop using the "Crop" button in the selection tool
        assert hasSelection();
        findButtonByText(window, "Crop").click();
        assert canvasSizeIs(selectionWidth, selectionHeight);
        assert noSelection();
        keyboardUndoRedoUndo();
        assert hasSelection();
        assert canvasSizeIs(origCanvasWidth, origCanvasHeight);

        // crop from the menu
        runMenuCommand("Crop");
        assert noSelection();
        assert canvasSizeIs(selectionWidth, selectionHeight);
        keyboardUndoRedoUndo();
        assert hasSelection();
        assert canvasSizeIs(origCanvasWidth, origCanvasHeight);

        testSelectionModifyMenu();
        assert hasSelection();

        runMenuCommand("Invert Selection");
        runMenuCommand("Stroke with Current Brush");
        runMenuCommand("Stroke with Current Eraser");
        assert hasSelection();
        runMenuCommand("Deselect");
        assert noSelection();
    }

    private void testSelectionModifyMenu() {
        runMenuCommand("Modify...");
        DialogFixture dialog = findDialogByTitle("Modify Selection");

        findButtonByText(dialog, "Change!").click();
        findButtonByText(dialog, "Change!").click();
        findButtonByText(dialog, "Close").click();

        keyboardUndoRedoUndo();
    }

    private void testCropTool() {
        window.toggleButton("Crop Tool Button").click();
        moveTo(200, 200);
        dragTo(400, 400);
        dragTo(450, 450);
        moveTo(200, 200);
        dragTo(150, 150);
        Utils.sleep(1, SECONDS);

        keyboardNudge();
        keyboardUndoRedoUndo();

        randomAltClick(); // must be at the end, otherwise it tries to start a rectangle

        findButtonByText(window, "Crop").click();

        keyboardUndoRedoUndo();
    }

    private void testMoveTool() {
        window.toggleButton("Move Tool Button").click();
        testMoveToolImpl(false);
        testMoveToolImpl(true);

        keyboardNudge();
        keyboardUndoRedoUndo();
    }

    private void testMoveToolImpl(boolean altDrag) {
        moveTo(400, 400);
        click();
        if (altDrag) {
            altDragTo(300, 300);
        } else {
            ImageLayer layer = ImageComponents.getActiveIC().getComp().getActiveMaskOrImageLayer();
            int txx = layer.getTranslationX();
            int txy = layer.getTranslationY();
            assert txx == 0;
            assert txy == 0;

            dragTo(200, 300);

            txx = layer.getTranslationX();
            txy = layer.getTranslationY();

            // This will be true only if we are at 100% zoom!
            assert txx == -200 : "txx = " + txx;
            assert txy == -100 : "txy = " + txx;
        }
        keyboardUndoRedoUndo();
    }

    private void testZoomTool() {
        window.toggleButton("Zoom Tool Button").click();
        moveTo(300, 300);

        click();
        click();
        altClick();
        altClick();

        testMouseWheelZooming();
        testControlPlusMinusZooming();
    }

    private void testControlPlusMinusZooming() {
        pressCtrlNumpadPlus();
        pressCtrlNumpadPlus();
        pressCtrlMinus();
        pressCtrlMinus();
    }

    private void pressCtrlNumpadPlus() {
        window.pressKey(VK_CONTROL).pressKey(VK_ADD)
                .releaseKey(VK_ADD).releaseKey(VK_CONTROL);
    }

    private void pressCtrlMinus() {
        window.pressKey(VK_CONTROL).pressKey(VK_MINUS)
                .releaseKey(VK_MINUS).releaseKey(VK_CONTROL);
    }


    private void testMouseWheelZooming() {
        window.pressKey(VK_CONTROL);
        ImageComponent c = (ImageComponent) ImageComponents.getActiveIC();
        robot.rotateMouseWheel(c, 2);
        robot.rotateMouseWheel(c, -2);
        window.releaseKey(VK_CONTROL);
    }

    private void keyboardUndo() {
        // press Ctrl-Z
        window.pressKey(VK_CONTROL).pressKey(VK_Z)
                .releaseKey(VK_Z).releaseKey(VK_CONTROL);
    }

    private void keyboardRedo() {
        // press Ctrl-Shift-Z
        window.pressKey(VK_CONTROL).pressKey(VK_SHIFT).pressKey(VK_Z)
                .releaseKey(VK_Z).releaseKey(VK_SHIFT).releaseKey(VK_CONTROL);
    }

    private void keyboardUndoRedo() {
        keyboardUndo();
        keyboardRedo();
    }

    private void keyboardUndoRedoUndo() {
        keyboardUndo();
        keyboardRedo();
        keyboardUndo();
    }

    private void keyboardInvert() {
        // press Ctrl-I
        window.pressKey(VK_CONTROL).pressKey(VK_I).releaseKey(VK_I).releaseKey(VK_CONTROL);
    }

    private void keyboardDeselect() {
        // press Ctrl-D
        window.pressKey(VK_CONTROL).pressKey(VK_D).releaseKey(VK_D).releaseKey(VK_CONTROL);
    }

    private void keyboardActualPixels() {
        // press Ctrl-0
        window.pressKey(VK_CONTROL).pressKey(VK_0).releaseKey(VK_0).releaseKey(VK_CONTROL);
    }

    private void keyboardNudge() {
        // TODO for some reason the shift is not detected
        window.pressKey(VK_SHIFT).pressKey(VK_RIGHT).releaseKey(VK_RIGHT).releaseKey(VK_SHIFT);
    }

    private void moveTo(int x, int y) {
        robot.moveMouse(x, y);
    }

    private void moveRandom() {
        int x = 200 + random.nextInt(400);
        int y = 200 + random.nextInt(400);
        moveTo(x, y);
    }

    private void shiftMoveClickRandom() {
        window.pressKey(VK_SHIFT);
        int x = 200 + random.nextInt(400);
        int y = 200 + random.nextInt(400);
        moveTo(x, y);
        click();
        window.releaseKey(VK_SHIFT);
    }

    private void dragRandom() {
        int x = 200 + random.nextInt(400);
        int y = 200 + random.nextInt(400);
        dragTo(x, y);
    }

    private void dragTo(int x, int y) {
        robot.pressMouse(MouseButton.LEFT_BUTTON);
        robot.moveMouse(x, y);
        robot.releaseMouse(MouseButton.LEFT_BUTTON);
    }

    private void altDragTo(int x, int y) {
        window.pressKey(VK_ALT);
        dragTo(x, y);
        window.releaseKey(VK_ALT);
    }

    private JMenuItemFixture findMenuItemByText(String guiName) {
        return new JMenuItemFixture(robot, robot.finder().find(new GenericTypeMatcher<JMenuItem>(JMenuItem.class) {
            @Override
            protected boolean isMatching(JMenuItem menuItem) {
                return guiName.equals(menuItem.getText());
            }
        }));
    }

    private DialogFixture findDialogByTitle(String title) {
        return new DialogFixture(robot, robot.finder().find(new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                return dialog.getTitle().equals(title);
            }
        }));
    }

    private static JButtonFixture findButtonByText(ComponentContainerFixture container, String text) {
        JButtonFixture button = container.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                if (!button.isShowing()) {
                    return false; // not interested in buttons that are not currently displayed
                }
                String buttonText = button.getText();
                if (buttonText == null) {
                    buttonText = "";
                }
                return buttonText.equals(text);
            }

            @Override
            public String toString() {
                return "[Button Text Matcher, text = " + text + "]";
            }
        });

        return button;
    }

    private static JButtonFixture findButtonByActionName(ComponentContainerFixture container, String actionName) {
        JButtonFixture button = container.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                if (!button.isShowing()) {
                    return false; // not interested in buttons that are not currently displayed
                }
                Action action = button.getAction();
                if (action == null) {
                    return false;
                }
                String buttonActionName = (String) action.getValue(Action.NAME);
                return actionName.equals(buttonActionName);
            }

            @Override
            public String toString() {
                return "[Button Action Name Matcher, action name = " + actionName + "]";
            }
        });

        return button;
    }


    private static JButtonFixture findButtonByToolTip(ComponentContainerFixture container, String toolTip) {
        JButtonFixture button = container.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                if (!button.isShowing()) {
                    return false; // not interested in buttons that are not currently displayed
                }
                String buttonToolTip = button.getToolTipText();
                if (buttonToolTip == null) {
                    buttonToolTip = "";
                }
                return buttonToolTip.equals(toolTip);
            }

            @Override
            public String toString() {
                return "[Button Text Matcher, text = " + toolTip + "]";
            }
        });

        return button;
    }


    private JOptionPaneFixture findJOptionPane() {
        return JOptionPaneFinder.findOptionPane().withTimeout(10, SECONDS).using(robot);
    }

    private JFileChooserFixture findSaveFileChooser() {
        return JFileChooserFinder.findFileChooser("save").using(robot);
    }

    private void saveWithOverwrite(String fileName) {
        JFileChooserFixture saveDialog = findSaveFileChooser();
        saveDialog.selectFile(new File(BASE_TESTING_DIR, fileName));
        saveDialog.approve();
        // say OK to the overwrite question
        JOptionPaneFixture optionPane = findJOptionPane();
        optionPane.yesButton().click();
    }

    private void waitForProgressMonitorEnd() {
        Utils.sleep(2, SECONDS); // wait until progress monitor comes up

        boolean dialogRunning = true;
        while (dialogRunning) {
            Utils.sleep(1, SECONDS);
            try {
                findDialogByTitle("Progress...");
            } catch (Exception e) {
                dialogRunning = false;
            }
        }
    }

    private void addNewLayer() {
        int nrLayers = ImageComponents.getActiveComp().get().getNrLayers();
        runMenuCommand("Duplicate Layer");
        assert numLayersIs(nrLayers + 1);
        keyboardInvert();
    }

    private static void checkTestingDirs() {
        assertThat(BASE_TESTING_DIR).exists().isDirectory();
        assertThat(INPUT_DIR).exists().isDirectory();
        assertThat(BATCH_RESIZE_OUTPUT_DIR).exists().isDirectory();
        assertThat(BATCH_FILTER_OUTPUT_DIR).exists().isDirectory();

        assertThat(Files.fileNamesIn(BATCH_RESIZE_OUTPUT_DIR.getPath(), false)).isEmpty();
        assertThat(Files.fileNamesIn(BATCH_FILTER_OUTPUT_DIR.getPath(), false)).isEmpty();
    }

    private void click() {
        robot.pressMouse(MouseButton.LEFT_BUTTON);
        robot.releaseMouse(MouseButton.LEFT_BUTTON);
    }

    private void randomClick() {
        moveRandom();
        click();
    }

    private void altClick() {
        robot.pressKey(VK_ALT);
        robot.pressMouse(MouseButton.LEFT_BUTTON);
        robot.releaseMouse(MouseButton.LEFT_BUTTON);
        robot.releaseKey(VK_ALT);
    }

    private static void cleanOutputs() {
        try {
            String cleanerScript = BASE_TESTING_DIR + "\\0000_clean_outputs.bat";
            Process process = Runtime.getRuntime().exec(cleanerScript);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void randomAltClick() {
        moveRandom();
        altClick();
    }

    private void addSelection() {
        window.toggleButton("Selection Tool Button").click();
        moveTo(200, 200);
        dragTo(400, 400);
    }

    private void addTranslation() {
        window.toggleButton("Move Tool Button").click();
        moveTo(400, 400);
        click();
        dragTo(200, 300);
    }
}
