/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.*;
import org.assertj.swing.launcher.ApplicationLauncher;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.FgBgColorSelector;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.io.Dirs;
import pixelitor.io.FileChoosers;
import pixelitor.io.IOTasks;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.ColorFillLayer;
import pixelitor.layers.LayerGUI;
import pixelitor.layers.MaskViewMode;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.SelectionModifyType;
import pixelitor.tools.BrushType;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.GradientColorType;
import pixelitor.tools.gradient.GradientType;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.TwoPointPaintType;
import pixelitor.utils.Language;
import pixelitor.utils.Utils;
import pixelitor.utils.test.PixelitorEventListener;

import javax.swing.*;
import java.awt.Color;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.awt.event.KeyEvent.*;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.guitest.AJSUtils.*;
import static pixelitor.tools.BrushType.*;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.shapes.TwoPointPaintType.NONE;
import static pixelitor.tools.shapes.TwoPointPaintType.RADIAL_GRADIENT;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * A utility class for running Pixelitor with assertj-swing based tests
 */
public class AppRunner {
    public static final int ROBOT_DELAY_DEFAULT = 50; // millis
    private static final int ROBOT_DELAY_SLOW = 300; // millis

    private static final DateTimeFormatter DATE_FORMAT_HM =
        DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT_HMS =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private static boolean newImageValidationTested = false;

    private final Robot robot;
    private final Mouse mouse;
    private final Keyboard keyboard;
    private final FrameFixture pw;
    private final LayersContainerFixture layersContainer;

    private static File startingOpenDir;
    private static File startingSaveDir;

    public AppRunner(File inputDir, String... fileNames) {
        robot = BasicRobot.robotWithNewAwtHierarchy();

        String[] paths = Stream.of(fileNames)
            .map(fileName -> new File(inputDir, fileName).getPath())
            .toArray(String[]::new);

        ApplicationLauncher
            .application("pixelitor.Pixelitor")
            .withArgs(paths)
            .start();

        new PixelitorEventListener().register();
        saveNormalSettings();

        pw = WindowFinder.findFrame(PixelitorWindow.class)
            .withTimeout(30, SECONDS)
            .using(robot);
        mouse = new Mouse(pw, robot);
        keyboard = new Keyboard(pw, robot, this);
        layersContainer = new LayersContainerFixture(robot);

        if (Language.getCurrent() != Language.ENGLISH) {
            throw new IllegalStateException("language is " + Language.getCurrent());
        }

        boolean nativeChoosers = EDT.call(FileChoosers::useNativeDialogs);
        if (nativeChoosers) {
            EDT.run(() -> FileChoosers.setUseNativeDialogs(false));
        }

        if (fileNames.length == 0) {
            return;
        }

        waitForImageLoading();
        mouse.recalcCanvasBounds();
    }

    private static void waitForImageLoading() {
        // wait even after the frame is shown to
        // make sure that the image is also loaded
        var comp = EDT.getComp();
        while (comp == null) {
            Utils.sleep(1, SECONDS);
            comp = EDT.getComp();
        }
    }

    public Robot getRobot() {
        return robot;
    }

    public Mouse getMouse() {
        return mouse;
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public FrameFixture getPW() {
        return pw;
    }

    public void runTests(Runnable tests) {
        assert calledOutsideEDT() : "on EDT";

        try {
            tests.run();
        } catch (Throwable t) {
            keyboard.releaseModifierKeys();
            throw t;
        }
        keyboard.releaseModifierKeys();
    }

    void setupDelayBetweenEvents() {
        // for example -Drobot.delay.millis=500 could be added to
        // the command line to slow it down
        String s = System.getProperty("robot.delay.millis");
        if (s == null) {
            delayBetweenEvents(ROBOT_DELAY_DEFAULT);
        } else {
            int delay = Integer.parseInt(s);
            delayBetweenEvents(delay);
        }
    }

    private void delayBetweenEvents(int millis) {
        robot.settings().delayBetweenEvents(millis);
        robot.settings().eventPostingDelay(2 * millis);
    }

    void runSlowly() {
        delayBetweenEvents(ROBOT_DELAY_SLOW);
    }

    void exit() {
        countDownBeforeExit();
        restoreNormalSettings();
        String exitMenuName = JVM.isMac ? "Quit" : "Exit";
        runMenuCommand(exitMenuName);
        findJOptionPane().yesButton().click();
    }

    private static void countDownBeforeExit() {
        final int secondsToWait = 5;
        int remainingSeconds = secondsToWait;
        do {
            System.out.print(remainingSeconds + "...");
            GUIUtils.showTaskbarProgress((int) (100 * (secondsToWait - remainingSeconds) / (double) secondsToWait));
            Utils.sleep(1, SECONDS);
        } while (--remainingSeconds > 0);
    }

    public void testBrushSettings(BrushType brushType, Tool tool) {
        var dialog = findDialogByTitleStartingWith("Settings for the");

        if (brushType == CALLIGRAPHY) {
            slideRandomly(dialog.slider("angle"));
        } else if (brushType == SHAPE) {
            chooseRandomly(dialog.comboBox("shape"));
            slideRandomly(dialog.slider("spacing"));
            slideRandomly(dialog.slider("angleJitter"));
            checkRandomly(dialog.checkBox("angleAware"));
        } else if (brushType == SPRAY) {
            chooseRandomly(dialog.comboBox("shape"));
            slideRandomly(dialog.slider("avgRadius"));
            slideRandomly(dialog.slider("radiusVar"));
            slideRandomly(dialog.slider("flow"));
            checkRandomly(dialog.checkBox("rndOpacity"));
            slideRandomly(dialog.slider("flow"));
            if (tool != ERASER) {
                slideRandomly(dialog.slider("colorRand"));
            }
        } else if (brushType == CONNECT) {
            chooseRandomly(dialog.comboBox("length"));
            slideRandomly(dialog.slider("density"));
            slideRandomly(dialog.slider("width"));
            checkRandomly(dialog.checkBox("resetForEach"));
            pushRandomly(0.1, dialog.button("resetHistNow"));
        } else if (brushType == OUTLINE_CIRCLE || brushType == OUTLINE_SQUARE) {
            checkRandomly(dialog.checkBox("dependsOnSpeed"));
        } else if (brushType == ONE_PIXEL) {
            checkRandomly(dialog.checkBox("aa"));
        } else {
            throw new IllegalStateException("brushType is " + brushType);
        }

        dialog.button("ok").click();
    }

    void setIndexedMode() {
        runMenuCommand("Indexed");
    }

    void clickTool(Tool tool) {
        pw.toggleButton(tool.getName() + " Button").click();
        //EDT.run(() -> Tools.changeTo(tool));

        Utils.sleep(200, MILLISECONDS);
        EDT.assertActiveToolIs(tool);
    }

    public void setDefaultColors() {
        pw.button(FgBgColorSelector.DEFAULTS_BUTTON_NAME).click();
    }

    public void swapColors() {
        pw.button(FgBgColorSelector.SWAP_BUTTON_NAME).click();
    }

    public void randomizeColors() {
        pw.button(FgBgColorSelector.RANDOMIZE_BUTTON_NAME).click();
    }

    public void createNewImage(int width, int height, String title) {
        runMenuCommand("New Image...");

        var dialog = findDialogByTitle("New Image");
        if (title != null) {
            dialog.textBox("nameTF").deleteText().enterText(title);
        }
        dialog.textBox("widthTF").deleteText().enterText(String.valueOf(width));

        if (!newImageValidationTested && Math.random() < 0.1) {
            dialog.textBox("heightTF").deleteText().enterText("e");

            // try to accept the dialog
            dialog.button("ok").click();
            expectAndCloseErrorDialog();

            newImageValidationTested = true;
        }

        dialog.textBox("heightTF").deleteText().enterText(String.valueOf(height));

        // try again
        dialog.button("ok").click();
        Utils.sleep(1, SECONDS);

        // this time the dialog should close
        dialog.requireNotVisible();

        String activeCompName = EDT.active(Composition::getName);
        if (title != null) {
            assertThat(activeCompName).isEqualTo(title);
        } else {
            assertThat(activeCompName).startsWith("Untitled");
        }
        assert !EDT.active(Composition::isDirty);

        mouse.recalcCanvasBounds();
        checkNumLayersIs(1);
    }

    void runMenuCommand(String text) {
        assert calledOutsideEDT() : threadInfo();

        findMenuItemByText(text)
            .requireEnabled()
            .click();
        Utils.sleep(200, MILLISECONDS);
    }

    public void selectLayerBellow() {
        runMenuCommand("Lower Layer Selection");
        keyboard.undoRedo("Lower Layer Selection");
    }

    public void selectLayerAbove() {
        runMenuCommand("Raise Layer Selection");
        keyboard.undoRedo("Raise Layer Selection");
    }

    void saveWithOverwrite(File baseTestingDir, String fileName) {
        var saveDialog = findSaveFileChooser();
        saveDialog.selectFile(new File(baseTestingDir, fileName));
        saveDialog.approve();
        var optionPane = findJOptionPane(); // overwrite question
        optionPane.yesButton().click();
    }

    void openFileWithDialog(String command, File dir, String fileName) {
        runMenuCommand(command);
        var openDialog = findOpenFileChooser();
        File file = new File(dir, fileName);
        assertThat(file)
            .exists()
            .isFile();
        openDialog.selectFile(file);
        openDialog.approve();

        // wait a bit to make sure that the async open completed
        IOTasks.waitForIdle();
        mouse.recalcCanvasBounds();

        if (EDT.active(Composition::isDirty)) {
            String compName = EDT.active(Composition::getName);
            throw new AssertionError(
                format("New comp '%s', loaded from %s is dirty",
                    compName, fileName));
        }
    }

    void waitForProgressMonitorEnd() {
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

        // even if the dialog is not visible, the
        // async saving of the last file might be still running
        boolean stillWriting = EDT.call(IOTasks::isBusyWriting);
        while (stillWriting) {
            System.out.println("waiting 1s for the IO thread...");
            Utils.sleep(1, SECONDS);
            stillWriting = EDT.call(IOTasks::isBusyWriting);
        }
    }

    void closeCurrentView() {
        runMenuCommand("Close");
    }

    void closeAll() {
        runMenuCommand("Close All");

        // close all warnings
        boolean warnings = true;
        while (warnings) {
            try {
                findJOptionPane().buttonWithText("Don't Save").click();
            } catch (Exception e) { // no more JOptionPane found
                warnings = false;
            }
        }

        EDT.assertNumOpenImagesIs(0);
    }

    void resize(int targetWidth) {
        resize(targetWidth, -1);
    }

    void resize(int targetWidth, int targetHeight) {
        runMenuCommand("Resize...");
        var dialog = findDialogByTitle("Resize");

        dialog.textBox("widthTF")
            .deleteText()
            .enterText(String.valueOf(targetWidth));

        if (targetHeight == -1) {
            // no target height was given, rely on "constrain proportions"
        } else {
            // disable constrain proportions
            dialog.checkBox().uncheck();

            dialog.textBox("heightTF")
                .deleteText()
                .enterText(String.valueOf(targetHeight));
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();

        Utils.sleep(5, SECONDS);
    }

    public void enlargeCanvas(int north, int west, int east, int south) {
        runMenuCommand("Enlarge Canvas...");
        var dialog = findDialogByTitle("Enlarge Canvas");

        dialog.slider("north").slideTo(north);
        dialog.slider("west").slideTo(west);
        dialog.slider("east").slideTo(east);
        dialog.slider("south").slideTo(south);

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    public void runModifySelection(int amount, SelectionModifyType type, int numClicks) {
        runMenuCommand("Modify Selection...");
        var dialog = findDialogByTitle("Modify Selection");

        dialog.slider("amount").slideTo(amount);
        dialog.comboBox("type").selectItem(type.toString());

        for (int i = 0; i < numClicks; i++) {
            AJSUtils.findButtonByText(dialog, "Change!").click();
        }

        AJSUtils.findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    static void clickPopupMenu(JPopupMenuFixture popupMenu, String text) {
        clickPopupMenu(popupMenu, text, true);
    }

    static void clickPopupMenu(JPopupMenuFixture popupMenu, String text, boolean onlyIfVisible) {
        AJSUtils.findPopupMenuItemByText(popupMenu, text, onlyIfVisible)
            .requireEnabled()
            .click();
    }

    void expectAndCloseErrorDialog() {
        var errorDialog = findDialogByTitle("Error");
        AJSUtils.findButtonByText(errorDialog, "OK").click();
        errorDialog.requireNotVisible();
    }

    static String getCurrentTimeHM() {
        return DATE_FORMAT_HM.format(LocalTime.now());
    }

    static String getCurrentTimeHMS() {
        return DATE_FORMAT_HMS.format(LocalTime.now());
    }

    JMenuItemFixture findMenuItemByText(String guiName) {
        return new JMenuItemFixture(robot, robot.finder().find(new GenericTypeMatcher<>(JMenuItem.class) {
            @Override
            protected boolean isMatching(JMenuItem menuItem) {
                return guiName.equals(menuItem.getText());
            }

            @Override
            public String toString() {
                return "Matcher for menu item, text = " + guiName;
            }
        }));
    }

    DialogFixture findFilterDialog() {
        return WindowFinder.findDialog("filterDialog").using(robot);
    }

    DialogFixture findAnyDialog() {
        return WindowFinder.findDialog(JDialog.class).using(robot);
    }

    DialogFixture findDialogByTitle(String title) {
        return new DialogFixture(robot, robot.finder().find(new GenericTypeMatcher<>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                // the visible condition is necessary because otherwise it finds
                // dialogs that were not disposed, but hidden
                return dialog.getTitle().equals(title) && dialog.isVisible();
            }

            @Override
            public String toString() {
                return "Matcher for JDialogs with title = " + title;
            }
        }));
    }

    DialogFixture findDialogByTitleStartingWith(String start) {
        return new DialogFixture(robot, robot.finder().find(new GenericTypeMatcher<>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                // the visible condition is necessary because otherwise it finds
                // dialogs that were not disposed, but hidden
                return dialog.getTitle().startsWith(start) && dialog.isVisible();
            }

            @Override
            public String toString() {
                return "Matcher for JDialogs with title starting with " + start;
            }
        }));
    }

    JOptionPaneFixture findJOptionPane() {
        return JOptionPaneFinder.findOptionPane()
            .withTimeout(10, SECONDS)
            .using(robot);
    }

    JFileChooserFixture findOpenFileChooser() {
        return JFileChooserFinder.findFileChooser("open").using(robot);
    }

    JFileChooserFixture findSaveFileChooser() {
        return JFileChooserFinder.findFileChooser("save").using(robot);
    }

    public JButtonFixture findButton(String name) {
        return pw.button(name);
    }

    public JButtonFixture findButtonByText(String text) {
        return AJSUtils.findButtonByText(pw, text);
    }

//    public void changeSmartFilterBlendingMode(String smartFilterName, BlendingMode newMode) {
//        JPopupMenuFixture popup = findLayerIconByLayerName(smartFilterName).showPopupMenu();
//        clickPopupMenu(popup, "Blending Options...");
//        DialogFixture dialog = findDialogByTitle("Blending Options for " + smartFilterName);
//        dialog.comboBox("bm").selectItem(newMode.toString());
//        dialog.button("ok").click();
//    }

    public void setMaskViewModeViaRightClick(String layerName, MaskViewMode maskViewMode) {
        clickMaskPopup(layerName, maskViewMode.toString());
    }

    public void deleteMaskViaRightClick(String layerName, boolean undo) {
        clickMaskPopup(layerName, "Delete");

        if (undo) {
            keyboard.undoRedoUndo("Delete Layer Mask");
        } else {
            keyboard.undoRedo("Delete Layer Mask");
        }
    }

    public void disableMaskViaRightClick(String layerName) {
        clickMaskPopup(layerName, "Disable");

        keyboard.undo("Disable Layer Mask");
        keyboard.redo("Disable Layer Mask");
    }

    public void enableMaskViaRightClick(String layerName) {
        clickMaskPopup(layerName, "Enable");

        keyboard.undo("Enable Layer Mask");
        keyboard.redo("Enable Layer Mask");
    }

    public void clickLayerPopup(String layerName, String menuName) {
        // this shouldn't be necessary, mask edit mode should be set by default
        var popup = findLayerIconByLayerName(layerName).showPopupMenu();
        clickPopupMenu(popup, menuName, false);
    }

    public void clickMaskPopup(String layerName, String menuName) {
        // this shouldn't be necessary, mask edit mode should be set by default
        var popup = findMaskIconByLayerName(layerName).showPopupMenu();
        clickPopupMenu(popup, menuName, false);
    }

    public JLabelFixture findLayerIconByLayerName(String layerName) {
        return findIconByLayerName(layerName, "layerIcon");
    }

    public JLabelFixture findMaskIconByLayerName(String layerName) {
        return findIconByLayerName(layerName, "maskIcon");
    }

    private JLabelFixture findIconByLayerName(String layerName, String iconType) {
        return pw.label(new GenericTypeMatcher<>(JLabel.class) {
            @Override
            protected boolean isMatching(JLabel label) {
                return (label.getParent() instanceof LayerGUI layerGUI)
                    && label.getName().equals(iconType)
                    && layerGUI.getLayer().getName().equals(layerName);
            }
        });
    }

    public void selectActiveLayer(String layerName) {
        findLayerIconByLayerName(layerName).click();
    }

    public void runFilterWithDialog(String name, Randomize randomize, Reseed reseed,
                                    ShowOriginal showOriginal, boolean testPresets,
                                    Consumer<DialogFixture> customizer) {
        runMenuCommand(name + "...");
        var dialog = findFilterDialog();
        if (customizer != null) {
            customizer.accept(dialog);
        }

        if (randomize == Randomize.YES) {
            dialog.button("randomize").click();
            dialog.button("resetAll").click();
            dialog.button("randomize").click();

            if (testPresets) {
                // load first the saved preset, to force it loading it from the disk
                String testPresetName = "test preset";
                boolean alreadyExists = hasPreset(dialog, testPresetName);
                if (alreadyExists) {
                    findMenuItemByText(testPresetName).click();
                    dialog.button("randomize").click();
                }

                savePreset(dialog, testPresetName, alreadyExists);
            }
        }

        if (showOriginal == ShowOriginal.YES) {
            dialog.checkBox("show original").click();
            dialog.checkBox("show original").click();
        }

        if (reseed == Reseed.YES) {
            dialog.button("reseed").click();
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    private static boolean hasPreset(DialogFixture dialog, String presetName) {
        JMenu presetsMenu = getPresetsMenu(dialog);
        if (presetsMenu != null) {
            int numItems = presetsMenu.getItemCount();
            for (int i = 0; i < numItems; i++) {
                JMenuItem item = presetsMenu.getItem(i);
                if (item != null && presetName.equals(item.getText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void savePreset(DialogFixture dialog, String presetName, boolean alreadyExists) {
        JMenu presetsMenu = getPresetsMenu(dialog);
        if (presetsMenu != null) {
            dialog.menuItem("savePreset").click();
            // expect a "preset name" OK-Cancel input dialog
            var pane = findJOptionPane();
            pane.textBox().enterText(presetName);
            pane.okButton().click();

            if (alreadyExists) {
                // expect a "preset already exists" Yes-No warning dialog
                pane = findJOptionPane();
                pane.yesButton().click();
            }
        }
    }

    private static JMenu getPresetsMenu(DialogFixture dialog) {
        JDialog realDialog = (JDialog) dialog.target();
        JMenuBar menuBar = realDialog.getJMenuBar();
        if (menuBar != null) {
            JMenu firstMenu = menuBar.getMenu(0);
            if (DialogMenuBar.PRESETS.equals(firstMenu.getText())) {
                return firstMenu;
            }
        }
        return null;
    }

    void checkNumLayersIs(int expected) {
        EDT.assertNumLayersIs(expected);

        // the above line checks the layers in the active holder
//        layersContainer.requireNumLayerButtons(expected);
    }

    public void checkLayerNamesAre(String... expectedNames) {
        assertThat(EDT.getComp()).layerNamesAre(expectedNames);
        layersContainer.requireLayerNames(expectedNames);
    }

    private static void saveNormalSettings() {
        startingOpenDir = Dirs.getLastOpen();
        startingSaveDir = Dirs.getLastSave();
    }

    private static void restoreNormalSettings() {
        Dirs.setLastOpen(startingOpenDir);
        Dirs.setLastSave(startingSaveDir);
    }

    void closeDoYouWantToSaveChangesDialog() {
        findJOptionPane().buttonWithText("Don't Save").click();
    }

    public void addLayerMask() {
        pw.button("addLayerMask")
            .requireEnabled()
            .click()
            .requireDisabled();
        assert EDT.activeLayerHasMask();
    }

    private void undoRedoNewLayer(int numLayersBefore, String editName) {
        checkNumLayersIs(numLayersBefore + 1);

        keyboard.undo(editName);
        checkNumLayersIs(numLayersBefore);

        keyboard.redo(editName);
        checkNumLayersIs(numLayersBefore + 1);
    }

    public void mergeDown() {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();

        runMenuCommand("Merge Down");
        checkNumLayersIs(numLayersBefore - 1);

        keyboard.undo("Merge Down");
        checkNumLayersIs(numLayersBefore);

        keyboard.redo("Merge Down");
        checkNumLayersIs(numLayersBefore - 1);
    }

    public void addEmptyImageLayer(boolean bellow) {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();

        if (bellow) {
            keyboard.pressCtrl();
        }
        pw.button("addLayer").click();
        if (bellow) {
            keyboard.releaseCtrl();
        }

        undoRedoNewLayer(numLayersBefore, "New Empty Layer");
    }

    public void addTextLayer(String text, Consumer<DialogFixture> customizer, String expectedText) {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();
        pw.button("addTextLayer").click();

        var dialog = findDialogByTitle("Create Text Layer");
        dialog.textBox("textTF")
            .requireText(expectedText)
            .deleteText()
            .enterText(text);

        if (customizer != null) {
            customizer.accept(dialog);
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();

        undoRedoNewLayer(numLayersBefore, "Add Text Layer");
    }

    public void editTextLayer(Consumer<DialogFixture> customizer) {
        boolean useKeyboard = Math.random() > 0.5;
        if (useKeyboard) {
            // press Ctrl-T
            pw.pressKey(VK_CONTROL).pressKey(VK_T);
        } else {
            runMenuCommand("Edit Text Layer...");
        }

        var dialog = findDialogByTitle("Edit Text Layer");

        if (useKeyboard) {
            // needs to be released on the dialog, otherwise ActionFailedException
            dialog.releaseKey(VK_T).releaseKey(VK_CONTROL);
        }

        GlobalEvents.assertDialogNestingIs(1);

        customizer.accept(dialog);

        dialog.button("ok").click();
        dialog.requireNotVisible();

        keyboard.undoRedo("Edit Text Layer");
    }

    public void addColorFillLayer(Color c) {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();

        runMenuCommand("New Color Fill Layer...");
        var colorSelector = findDialogByTitle("Add Color Fill Layer");
        AJSUtils.findButtonByText(colorSelector, "OK").click();

        EDT.run(() -> ((ColorFillLayer) Views.getActiveLayer()).changeColor(c, true));

        keyboard.undo("Color Fill Layer Change");
        undoRedoNewLayer(numLayersBefore, "Add Color Fill Layer");
        keyboard.redo("Color Fill Layer Change");
    }

    public void addGradientFillLayer(GradientType gradientType) {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();

        keyboard.ctrlAltPress(VK_G);
        undoRedoNewLayer(numLayersBefore, "Add Gradient Fill Layer");

        EDT.assertActiveToolIs(Tools.GRADIENT);
        CanvasDrag dragLocation = new CanvasDrag(100, 100, 100);
        drawGradient(gradientType, GradientColorType.FG_TO_BG, dragLocation,
            new Color(68, 152, 115),
            new Color(185, 56, 177));

        keyboard.undoRedo("Gradient Fill Layer Change");
    }

    public void drawGradient(GradientType gradientType, GradientColorType colorType, CanvasDrag dragLocation, Color fgColor, Color bgColor) {
        clickTool(Tools.GRADIENT);

        pw.comboBox("typeCB").selectItem(gradientType.toString());
        pw.comboBox("colorTypeCB").selectItem(colorType.toString());
        pw.checkBox("revertCB").uncheck();

        EDT.setFgBgColors(fgColor, bgColor);
        mouse.drag(dragLocation);

        keyboard.pressEsc(); // hide the gradient handles
    }

    public void drawGradient(GradientType gradientType) {
        clickTool(Tools.GRADIENT);

        // draw a radial gradient
        pw.comboBox("typeCB").selectItem(gradientType.toString());
        pw.checkBox("revertCB").check();

        if (EDT.getZoomLevelOfActive() != ZoomLevel.Z100) {
            // otherwise location on screen can lead to crazy results
            runMenuCommand("Actual Pixels");
        }

        mouse.dragFromCanvasCenterToTheRight();
        keyboard.pressEsc(); // hide the gradient handles
    }

    public void addShapesLayer(ShapeType shapeType, CanvasDrag shapeLocation) {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();

        keyboard.ctrlAltPress(VK_S);
        undoRedoNewLayer(numLayersBefore, "Add Shape Layer");

        EDT.assertActiveToolIs(Tools.SHAPES);

        EDT.setFgBgColors(new Color(248, 199, 25), new Color(39, 81, 39));
        drawShape(shapeType, RADIAL_GRADIENT, NONE, shapeLocation, false);
    }

    public void drawShape(ShapeType type,
                          TwoPointPaintType fillPaint,
                          TwoPointPaintType strokePaint,
                          CanvasDrag shapeLocation, boolean rasterize) {
        clickTool(Tools.SHAPES);
        pw.comboBox("shapeTypeCB").selectItem(type.toString());
        pw.comboBox("fillPaintCB").selectItem(fillPaint.toString());
        pw.comboBox("strokePaintCB").selectItem(strokePaint.toString());
        mouse.drag(shapeLocation);
        keyboard.undoRedo("Create Shape");

        if (rasterize) {
            keyboard.pressEsc();
            keyboard.undoRedo("Rasterize Shape");
        }
    }

    public void addAdjustmentLayer(String filterName, Consumer<DialogFixture> customizer) {
        pw.button("addAdjLayer").click();
        JPopupMenuFixture popup = new JPopupMenuFixture(robot, robot.findActivePopupMenu());
        clickPopupMenu(popup, "New " + filterName, true);
        DialogFixture dialog = findDialogByTitle(filterName);
        customizer.accept(dialog);
        dialog.button("ok").click();
    }

    public void changeLayerBlendingMode(BlendingMode blendingMode) {
        String existingBMString = pw.comboBox("layerBM").selectedItem();
        if (blendingMode.toString().equals(existingBMString)) {
            return;
        }
        pw.comboBox("layerBM")
            .selectItem(blendingMode.toString());

        keyboard.undo("Blending Mode Change");
        pw.comboBox("layerBM").requireSelection(existingBMString);

        keyboard.redo("Blending Mode Change");
        pw.comboBox("layerBM").requireSelection(blendingMode.toString());
    }

    public void changeLayerOpacity(float newValue) {
        String newValueString = String.valueOf((int) (newValue * 100));
        JTextComponentFixture layerOpacity = findLayerOpacityTF();
        layerOpacity
            .requireText("100")
            .deleteText()
            .enterText(newValueString)
            .pressKey(VK_ENTER)
            .releaseKey(VK_ENTER);

        keyboard.undo("Layer Opacity Change");
        layerOpacity.requireText("100");

        keyboard.redo("Layer Opacity Change");
        layerOpacity.requireText(newValueString);
    }

    private JTextComponentFixture findLayerOpacityTF() {
        // the name of the inner textfield can't be set directly,
        // because Nimbus uses it => use the name of the parent combo box
        return pw.textBox(new GenericTypeMatcher<>(JTextField.class) {
            @Override
            protected boolean isMatching(JTextField c) {
                return "layerOpacity".equals(c.getParent().getName());
            }
        });
    }

    public enum Randomize {YES, NO}

    public enum Reseed {YES, NO}

    public enum ShowOriginal {YES, NO}
}
