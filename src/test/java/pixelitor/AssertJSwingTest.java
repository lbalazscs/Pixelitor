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

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JCheckBoxFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.fixture.JMenuItemFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.fixture.JToggleButtonFixture;
import org.assertj.swing.launcher.ApplicationLauncher;
import org.fest.util.Files;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import pixelitor.io.FileChoosers;
import pixelitor.layers.LayerButton;
import pixelitor.tools.BrushType;
import pixelitor.tools.GradientColorType;
import pixelitor.tools.GradientTool;
import pixelitor.tools.GradientType;
import pixelitor.tools.Symmetry;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_I;
import static java.awt.event.KeyEvent.VK_Z;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class AssertJSwingTest {
    public static final File BASE_TESTING_DIR = new File("C:\\pix_tests");
    public static final File INPUT_DIR = new File(BASE_TESTING_DIR, "input");
    public static final File BATCH_RESIZE_OUTPUT_DIR = new File(BASE_TESTING_DIR, "batch_resize_output");
    public static final File BATCH_FILTER_OUTPUT_DIR = new File(BASE_TESTING_DIR, "batch_filter_output");

    private FrameFixture window;
    private Random random = new Random();
    private Robot robot;

    enum Randomize {YES, NO}

    protected void setUpRobot() {
        robot = BasicRobot.robotWithNewAwtHierarchy();
        robot.settings().delayBetweenEvents(500);
    }

    @Before
    public void setUp() {
        setUpRobot();
        onSetUp();
    }

    protected void onSetUp() {
        ApplicationLauncher
                .application("pixelitor.Pixelitor")
                .withArgs((new File(INPUT_DIR, "a.jpg")).getPath())
                .start();
        window = WindowFinder.findFrame("frame0")
                .withTimeout(15, SECONDS)
                .using(robot);
        PixelitorWindow.getInstance().setLocation(0, 0);
    }

    @Test
    public void testApp() {
        checkTestingDirs();

        testTools();
        testMenus();
        testLayers();

        sleep(5, SECONDS);
    }

    protected void testTools() {
        testZoomTool();
        testMoveTool();
        testCropTool();
        testSelectionTool();
        testCloneTool();
        testEraserTool();
        testBrushTool();
        testGradientTool();
        testPaintBucketTool();
        testColorPickerTool();
        testShapesTool();
        testHandTool();
    }

    private void testLayers() {
        // TODO add, remove, change visibility, all the Layers menus

        // TODO create a LayerButtonFixture class??
    }

    private JToggleButtonFixture findLayerButton(String layerName) {
        return new JToggleButtonFixture(robot, robot.finder().find(new GenericTypeMatcher<LayerButton>(LayerButton.class) {
            @Override
            protected boolean isMatching(LayerButton layerButton) {
                return layerButton.getLayerName().equals(layerName);
            }
        }));
    }

    protected void testMenus() {
        testFileMenu();
        testEditMenu();
        testFilters();
        testZoomCommands();
        testViewCommands();
        testHelpMenu();
    }

    protected void testHelpMenu() {
        testTipOfTheDay();
        testCheckForUpdate();
        testAbout();
    }

    private void testTipOfTheDay() {
        findMenuItemByText("Tip of the Day").click();
        DialogFixture dialog = findDialogByTitle("Tip of the Day");
        findButtonInDialogByText(dialog, "Next >").click();
        findButtonInDialogByText(dialog, "Next >").click();
        findButtonInDialogByText(dialog, "< Back").click();
        findButtonInDialogByText(dialog, "Close").click();
    }

    private void testCheckForUpdate() {
        findMenuItemByText("Check for Update...").click();
        findJOptionPane().cancelButton().click();
    }

    private void testAbout() {
        findMenuItemByText("About").click();
        DialogFixture aboutDialog = findDialogByTitle("About Pixelitor");
        aboutDialog.button("ok").click();
    }

    protected void testEditMenu() {
        keyboardInvert();
        runMenuCommand("Repeat Invert");
        runMenuCommand("Undo Invert");
        runMenuCommand("Redo Invert");
        testFilterWithDialog("Fade Invert", Randomize.NO);

        // select for crop
        window.toggleButton("Selection Tool Button").click();
        move(200, 200);
        drag(400, 400);
        runMenuCommand("Crop");
        keyboardUndo();
        keyboardDeselect();

        testCopyPaste();

        testResize();
        testRotateFlip();

        testFilterWithDialog("Transform Layer...", Randomize.YES);

        testPreferences();
    }

    private void testPreferences() {
        runMenuCommand("Preferences...");
        DialogFixture d = findDialogByTitle("Preferences");
        d.button("ok").click();
    }

    private void testRotateFlip() {
        runMenuCommand("Rotate 90° CW");
        runMenuCommand("Rotate 180°");
        runMenuCommand("Rotate 90° CCW");
        runMenuCommand("Flip Horizontal");
        runMenuCommand("Flip Vertical");
    }

    private void testResize() {
        runMenuCommand("Resize...");
        DialogFixture resizeDialog = findDialogByTitle("Resize");

        JTextComponentFixture widthTF = resizeDialog.textBox("widthTF");
        widthTF.deleteText().enterText("622");

        JTextComponentFixture heightTF = resizeDialog.textBox("heightTF");
        heightTF.deleteText().enterText("422");

        resizeDialog.button("ok").click();
    }

    private void testCopyPaste() {
        runMenuCommand("Copy Layer");
        runMenuCommand("Paste as New Layer");
        runMenuCommand("Copy Composite");
        runMenuCommand("Paste as New Image");
    }

    protected void testFileMenu() {
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
        testScreenCapture();
        testCloseAll();
    }

    protected void testNewImage() {
        findMenuItemByText("New Image...").click();
        DialogFixture newImageDialog = findDialogByTitle("New Image");
        newImageDialog.textBox("widthTF").deleteText().enterText("611");
        newImageDialog.textBox("heightTF").deleteText().enterText("411");
        newImageDialog.button("ok").click();
    }

    protected void testFileOpen() {
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
        checkNumLayers(1);

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
        checkNumLayers(1);

        runMenuCommand("Export Layer Animation...");
        findJOptionPane().okButton().click();
        addNewLayer();

        // this time it should work
        runMenuCommand("Export Layer Animation...");
        findDialogByTitle("Export Animated GIF").button("ok").click();

        saveWithOverwrite("layeranim.gif");
    }

    private void testExportTweeningAnimation() {
        assertTrue(ImageComponents.getActiveComp().isPresent());
        runMenuCommand("Export Tweening Animation...");
        DialogFixture dialog = findDialogByTitle("Export Tweening Animation");
        dialog.comboBox().selectItem("Angular Waves");
        dialog.button("ok").click(); // next
        dialog.button("Randomize Settings").click();
        dialog.button("ok").click(); // next
        dialog.button("Randomize Settings").click();
        dialog.button("ok").click(); // next
        dialog.button("ok").click(); // render

        // say OK to the folder not empty question
        JOptionPaneFixture optionPane = findJOptionPane();
        optionPane.yesButton().click();

        waitForProgressMonitorEnd();
    }

    private void testClose() {
        assertEquals(2, ImageComponents.getNrOfOpenImages());

        runMenuCommand("Close");

        assertEquals(1, ImageComponents.getNrOfOpenImages());
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

        assertEquals(0, ImageComponents.getNrOfOpenImages());

        // restore for the next test
        runMenuCommand("Paste as New Image");
    }

    private void testBatchResize() {
        // TODO before stating the test check that the output dir is empty
        FileChoosers.setLastOpenDir(INPUT_DIR);
        FileChoosers.setLastSaveDir(BATCH_RESIZE_OUTPUT_DIR);
        runMenuCommand("Batch Resize...");
        DialogFixture dialog = findDialogByTitle("Batch Resize");

        dialog.textBox("widthTF").setText("200");
        dialog.textBox("heightTF").setText("200");
        dialog.button("ok").click();
    }

    private void testBatchFilter() {
        // TODO before stating the test check that the output dir is empty
        FileChoosers.setLastOpenDir(INPUT_DIR);
        FileChoosers.setLastSaveDir(BATCH_FILTER_OUTPUT_DIR);

        assertTrue(ImageComponents.getActiveComp().isPresent());
        runMenuCommand("Batch Filter...");
        DialogFixture dialog = findDialogByTitle("Batch Filter");
        dialog.comboBox("filtersCB").selectItem("Angular Waves");
        dialog.button("ok").click(); // next
        sleep(3, SECONDS);
        dialog.button("Randomize Settings").click();
        dialog.button("ok").click(); // start processing

        waitForProgressMonitorEnd();
    }

    private void testExportLayerToPNG() {
        FileChoosers.setLastSaveDir(BASE_TESTING_DIR);
        addNewLayer();
        runMenuCommand("Export Layers to PNG...");
        findDialogByTitle("Select Output Folder").button("ok").click();
        sleep(2, SECONDS);
    }

    private void testScreenCapture() {
        ImageComponent activeIC = ImageComponents.getActiveImageComponent();
        testScreenCapture(true);
        testScreenCapture(false);
        ImageComponents.setActiveImageComponent(activeIC, true);
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

    protected void testViewCommands() {
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

    protected void testZoomCommands() {
        runMenuCommand("Zoom In");
        runMenuCommand("Zoom Out");
        runMenuCommand("Actual Pixels");
        runMenuCommand("Fit Screen");
        runMenuCommand("12.5 %");
        runMenuCommand("17.7 %");
        runMenuCommand("25 %");
        runMenuCommand("35.3 %");
        runMenuCommand("50 %");
        runMenuCommand("70.7 %");
        runMenuCommand("100 %");
        runMenuCommand("141.4 %");
        runMenuCommand("200 %");
        runMenuCommand("282.8 %");
        runMenuCommand("400 %");
        runMenuCommand("565.7 %");
        runMenuCommand("800 %");
        runMenuCommand("1131.4 %");
        runMenuCommand("1600 %");
        runMenuCommand("2262.7 %");
        runMenuCommand("3200 %");
        runMenuCommand("4525.5 %");
        runMenuCommand("6400 %");
    }

    protected void testFilters() {
        testFilterWithDialog("Color Balance...", Randomize.YES);
        testFilterWithDialog("Hue/Saturation...", Randomize.YES);
        testFilterWithDialog("Colorize...", Randomize.YES);
        testFilterWithDialog("Levels...", Randomize.NO);
        testFilterWithDialog("Brightness/Contrast...", Randomize.YES);
        testFilterWithDialog("Solarize...", Randomize.YES);
        testFilterWithDialog("Sepia...", Randomize.NO);
        testNoDialogFilter("Invert");
        testFilterWithDialog("Channel Invert...", Randomize.NO);
        testFilterWithDialog("Channel Mixer...", Randomize.NO); // TODO
        testFilterWithDialog("Extract Channel...", Randomize.YES);
        testNoDialogFilter("Luminosity");
        testNoDialogFilter("Value = max(R,G,B)");
        testNoDialogFilter("Desaturate");
        testNoDialogFilter("Hue");
        testNoDialogFilter("Hue (with colors)");
        testNoDialogFilter("Saturation");
        testFilterWithDialog("Quantize...", Randomize.YES);
        testFilterWithDialog("Posterize...", Randomize.NO);
        testFilterWithDialog("Threshold...", Randomize.YES);
        testFilterWithDialog("Tritone...", Randomize.YES);
        testFilterWithDialog("Gradient Map...", Randomize.NO);
        testFilterWithDialog("Color Halftone...", Randomize.YES);
        testFilterWithDialog("Dither...", Randomize.YES);
        testNoDialogFilter("Foreground Color");
        testNoDialogFilter("Background Color");
        testNoDialogFilter("Transparent");
        testFilterWithDialog("Color Wheel...", Randomize.YES);
        testFilterWithDialog("Four Color Gradient...", Randomize.NO); // TODO
        testFilterWithDialog("Starburst...", Randomize.YES);
        testFilterWithDialog("Gaussian Blur...", Randomize.YES);
        testFilterWithDialog("Smart Blur...", Randomize.YES);
        testFilterWithDialog("Box Blur...", Randomize.YES);
        testFilterWithDialog("Fast Blur...", Randomize.YES);
        testFilterWithDialog("Lens Blur...", Randomize.YES);
        testFilterWithDialog("Motion Blur...", Randomize.YES);
        testFilterWithDialog("Spin and Zoom Blur...", Randomize.YES);
        testFilterWithDialog("Unsharp Mask...", Randomize.YES);
        testFilterWithDialog("Swirl, Pinch, Bulge...", Randomize.YES);
        testFilterWithDialog("Circle to Square...", Randomize.YES);
        testFilterWithDialog("Perspective...", Randomize.NO); // TODO
        testFilterWithDialog("Lens Over Image...", Randomize.YES);
        testFilterWithDialog("Magnify...", Randomize.YES);
        testFilterWithDialog("Turbulent Distortion...", Randomize.YES);
        testFilterWithDialog("Underwater...", Randomize.YES);
        testFilterWithDialog("Water Ripple...", Randomize.YES);
        testFilterWithDialog("Waves...", Randomize.YES);
        testFilterWithDialog("Angular Waves...", Randomize.YES);
        testFilterWithDialog("Radial Waves...", Randomize.YES);
        testFilterWithDialog("Glass Tiles...", Randomize.YES);
        testFilterWithDialog("Polar Glass Tiles...", Randomize.YES);
        testFilterWithDialog("Frosted Glass...", Randomize.YES);
        testFilterWithDialog("Little Planet...", Randomize.YES);
        testFilterWithDialog("Polar Coordinates...", Randomize.YES);
        testFilterWithDialog("Wrap Around Arc...", Randomize.YES);
        testFilterWithDialog("Kaleidoscope...", Randomize.YES);
        testFilterWithDialog("Video Feedback...", Randomize.YES);
        testFilterWithDialog("Offset...", Randomize.NO); // TODO
        testFilterWithDialog("Slice...", Randomize.YES);
        testFilterWithDialog("Mirror...", Randomize.YES);
        testFilterWithDialog("Glow...", Randomize.YES);
        testFilterWithDialog("Sparkle...", Randomize.YES);
        testFilterWithDialog("Rays...", Randomize.YES);
        testFilterWithDialog("Glint...", Randomize.YES);
        testNoDialogFilter("Reduce Single Pixel Noise");
        testNoDialogFilter("3x3 Median Filter");
        testFilterWithDialog("Add Noise...", Randomize.YES);
        testFilterWithDialog("Pixelate...", Randomize.YES);
        testFilterWithDialog("Clouds...", Randomize.YES);
        testFilterWithDialog("Value Noise...", Randomize.YES);
        testFilterWithDialog("Caustics...", Randomize.YES);
        testFilterWithDialog("Plasma...", Randomize.YES);
        testFilterWithDialog("Wood...", Randomize.YES);
        testFilterWithDialog("Cells...", Randomize.YES);
        testFilterWithDialog("Brushed Metal...", Randomize.YES);
        testFilterWithDialog("Crystallize...", Randomize.YES);
        testFilterWithDialog("Pointillize...", Randomize.YES);
        testFilterWithDialog("Stamp...", Randomize.YES);
        testFilterWithDialog("Dry Brush...", Randomize.YES);
        testFilterWithDialog("Random Spheres...", Randomize.YES);
        testFilterWithDialog("Smear...", Randomize.YES);
        testFilterWithDialog("Emboss...", Randomize.YES);
        testFilterWithDialog("Orton Effect...", Randomize.YES);
        testFilterWithDialog("Photo Collage...", Randomize.YES);
        testFilterWithDialog("Convolution Edge Detection...", Randomize.YES);
        testNoDialogFilter("Laplacian");
        testFilterWithDialog("Difference of Gaussians...", Randomize.YES);
        testFilterWithDialog("Canny Edge Detector...", Randomize.YES);
        testFilterWithDialog("Drop Shadow...", Randomize.YES);
        testFilterWithDialog("2D Transitions...", Randomize.YES);

        // TODO    Custom 3x3 Convolution...
        // TODO    Custom 5x5 Convolution...
        // TODO    Random Filter...
        // TODO    Text...
    }

    private void runMenuCommand(String text) {
        findMenuItemByText(text).click();
    }

    private void testNoDialogFilter(String name) {
        runMenuCommand(name);
    }


    private void testFilterWithDialog(String name, Randomize randomize) {
        // window.menuItem(name).click();
        findMenuItemByText(name).click();
        DialogFixture filterDialog = WindowFinder.findDialog("filterDialog").using(robot);
        if (randomize == Randomize.YES) {
            filterDialog.button("Randomize Settings").click();
            filterDialog.button("Reset All").click();
            filterDialog.button("Randomize Settings").click();
        }
        filterDialog.button("ok").click();
    }

    protected void testHandTool() {
        window.toggleButton("Hand Tool Button").click();
    }

    protected void testShapesTool() {
        window.toggleButton("Shapes Tool Button").click();
    }

    protected void testColorPickerTool() {
        window.toggleButton("Color Picker Tool Button").click();
        move(300, 300);
        window.click();
        drag(400, 400);
    }

    protected void testPaintBucketTool() {
        window.toggleButton("Paint Bucket Tool Button").click();
        move(300, 300);
        window.click();
    }

    protected void testGradientTool() {
        window.toggleButton("Gradient Tool Button").click();
        for (GradientType gradientType : GradientType.values()) {
            window.comboBox("gradientTypeSelector").selectItem(gradientType.toString());
            for (String cycleMethod : GradientTool.CYCLE_METHODS) {
                window.comboBox("gradientCycleMethodSelector").selectItem(cycleMethod);
                GradientColorType[] gradientColorTypes = GradientColorType.values();
                for (GradientColorType colorType : gradientColorTypes) {
                    window.comboBox("gradientColorTypeSelector").selectItem(colorType.toString());
                    window.checkBox("gradientInvert").uncheck();
                    move(200, 200);
                    drag(400, 400);
                    window.checkBox("gradientInvert").check();
                    move(200, 200);
                    drag(400, 400);
                }
            }
        }
    }

    protected void testEraserTool() {
        window.toggleButton("Eraser Tool Button").click();
        testBrushStrokes();
    }

    protected void testBrushTool() {
        window.toggleButton("Brush Tool Button").click();
        testBrushStrokes();
    }

    protected void testCloneTool() {
        window.toggleButton("Clone Tool Button").click();
        move(300, 300);
        window.pressKey(VK_ALT).click().releaseKey(VK_ALT);
        move(400, 300);
        for (int i = 1; i <= 20; i++) {
            drag(400 + i * 5, 300);
            drag(400 + i * 5, 400);
        }
    }

    protected void testBrushStrokes() {
        for (BrushType brushType : BrushType.values()) {
            window.comboBox("brushTypeSelector").selectItem(brushType.toString());
            for (Symmetry symmetry : Symmetry.values()) {
                window.comboBox("symmetrySelector").selectItem(symmetry.toString());
                window.pressAndReleaseKeys(KeyEvent.VK_R);
                moveRandom();
                dragRandom();
            }
        }
    }

    protected void testSelectionTool() {
        window.toggleButton("Selection Tool Button").click();
        move(200, 200);
        drag(400, 400);
        window.button("brushTraceButton").click();
        keyboardDeselect();
        keyboardUndo(); // keyboardUndo deselection
        keyboardUndo(); // keyboardUndo tracing
        window.comboBox("selectionTypeCombo").selectItem("Ellipse");
        move(200, 200);
        drag(400, 400);
        window.comboBox("selectionInteractionCombo").selectItem("Add");
        move(400, 200);
        drag(500, 300);
        window.button("eraserTraceButton").click();
        keyboardDeselect();
        // TODO test crop from tool and also from menu
        // TODO test all items from selection menu
    }

    protected void testCropTool() {
        window.toggleButton("Crop Tool Button").click();
        move(200, 200);
        drag(400, 400);
        drag(450, 450);
        move(200, 200);
        drag(150, 150);
        sleep(1, SECONDS);
        window.button("cropButton").click();
        keyboardUndo();
    }

    protected void testMoveTool() {
        window.toggleButton("Move Tool Button").click();
        move(300, 300);
        drag(400, 400);
        keyboardUndo();
    }

    protected void testZoomTool() {
        window.toggleButton("Zoom Tool Button").click();
        move(300, 300);
        window.click();
        window.click();
        // TODO Alt-click to zoom out and all the zoom methods, including mouse wheel
    }

    private void keyboardUndo() {
        window.pressKey(VK_CONTROL).pressKey(VK_Z).releaseKey(VK_Z).releaseKey(VK_CONTROL);
    }

    private void keyboardInvert() {
        window.pressKey(VK_CONTROL).pressKey(VK_I).releaseKey(VK_I).releaseKey(VK_CONTROL);
    }

    private void keyboardDeselect() {
        window.pressKey(VK_CONTROL).pressKey(VK_D).releaseKey(VK_D).releaseKey(VK_CONTROL);
    }

    private void move(int x, int y) {
        robot.moveMouse(x, y);
    }

    private void moveRandom() {
        int x = 200 + random.nextInt(400);
        int y = 200 + random.nextInt(400);
        move(x, y);
    }

    private void dragRandom() {
        int x = 200 + random.nextInt(400);
        int y = 200 + random.nextInt(400);
        drag(x, y);
    }

    private void drag(int x, int y) {
        robot.pressMouse(MouseButton.LEFT_BUTTON);
        robot.moveMouse(x, y);
        robot.releaseMouse(MouseButton.LEFT_BUTTON);
    }

    private void sleep(int duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            throw new IllegalStateException("interrupted!");
        }
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

    private JButtonFixture findButtonInDialogByText(DialogFixture dialog, String text) {
        JButtonFixture button = dialog.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                return button.getText().equals(text);
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

    private void checkNumLayers(int num) {
        int nrLayers = ImageComponents.getActiveComp().get().getNrLayers();
        assertTrue(nrLayers == num);
    }

    private void waitForProgressMonitorEnd() {
        sleep(2, SECONDS); // wait until progress monitor comes up

        boolean dialogRunning = true;
        while (dialogRunning) {
            sleep(1, SECONDS);
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
        checkNumLayers(nrLayers + 1);
        keyboardInvert();
    }

    private void checkTestingDirs() {
        assertThat(BASE_TESTING_DIR).exists().isDirectory();
        assertThat(INPUT_DIR).exists().isDirectory();
        assertThat(BATCH_RESIZE_OUTPUT_DIR).exists().isDirectory();
        assertThat(BATCH_FILTER_OUTPUT_DIR).exists().isDirectory();

        assertThat(Files.fileNamesIn(BATCH_RESIZE_OUTPUT_DIR.getPath(), false)).isEmpty();
        assertThat(Files.fileNamesIn(BATCH_FILTER_OUTPUT_DIR.getPath(), false)).isEmpty();
    }
}
