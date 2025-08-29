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
import pixelitor.gui.ImageArea;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.history.History;
import pixelitor.history.HistoryChecker;
import pixelitor.io.Dirs;
import pixelitor.io.FileChoosers;
import pixelitor.io.IOTasks;
import pixelitor.layers.*;
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

import javax.swing.*;
import java.awt.Color;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_G;
import static java.awt.event.KeyEvent.VK_S;
import static java.awt.event.KeyEvent.VK_T;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.guitest.GUITestUtils.checkRandomly;
import static pixelitor.guitest.GUITestUtils.chooseRandomly;
import static pixelitor.guitest.GUITestUtils.clickRandomly;
import static pixelitor.guitest.GUITestUtils.slideRandomly;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.shapes.TwoPointPaintType.NONE;
import static pixelitor.tools.shapes.TwoPointPaintType.RADIAL_GRADIENT;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.Utils.toPercentage;

/**
 * A utility class for running Pixelitor with assertj-swing based tests
 */
public class AppRunner {
    public static final int DEFAULT_ROBOT_DELAY = 50; // millis
    private static final int SLOW_ROBOT_DELAY = 300; // millis
    private static final int APP_START_TIMEOUT = 30; // seconds

    private static final DateTimeFormatter TIME_FORMAT_HM =
        DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMAT_HMS =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private static boolean newImageValidationTested = false;

    private final Robot robot;
    private final Mouse mouse;
    private final Keyboard keyboard;
    private final FrameFixture pw;
    private final LayersContainerFixture layersContainer;

    private static File initialOpenDir;
    private static File initialSaveDir;

    private final HistoryChecker historyChecker;

    public AppRunner(HistoryChecker historyChecker, File inputDir, String... fileNames) {
        this.historyChecker = historyChecker;
        robot = BasicRobot.robotWithNewAwtHierarchy();

        History.setChecker(historyChecker);

        // Converts filenames to full paths
        String[] filePaths = Stream.of(fileNames)
            .map(fileName -> new File(inputDir, fileName).getPath())
            .toArray(String[]::new);

        ApplicationLauncher
            .application("pixelitor.Pixelitor")
            .withArgs(filePaths)
            .start();

        rememberInitialSettings();

        pw = WindowFinder.findFrame(PixelitorWindow.class)
            .withTimeout(APP_START_TIMEOUT, SECONDS)
            .using(robot);
        mouse = new Mouse(pw, robot);
        keyboard = new Keyboard(pw, robot, this, historyChecker);
        layersContainer = new LayersContainerFixture(robot);

        if (Language.getActive() != Language.ENGLISH) {
            throw new IllegalStateException("language is " + Language.getActive());
        }
        if (EDT.call(() -> ImageArea.isActiveMode(ImageArea.Mode.FRAMES))) {
            EDT.run(ImageArea::toggleUI);
        }

        // initialize the AWT native picker here, if it is used
        boolean nativeChoosers = EDT.call(FileChoosers::useNativeDialogs);
        if (nativeChoosers) {
            EDT.run(() -> FileChoosers.setUseNativeDialogs(false));
        }

        if (fileNames.length > 0) {
            waitForImageLoading(fileNames.length);
            mouse.updateCanvasBounds();
        }
    }

    private static void waitForImageLoading(int numImages) {
        // wait even after the frame is shown to
        // make sure that the image is also loaded
        int numOpenImages = EDT.call(Views::getNumViews);
        while (numOpenImages < numImages) {
            Utils.sleep(1, SECONDS);
            numOpenImages = EDT.call(Views::getNumViews);
        }
    }

    public void runTests(Runnable tests) {
        assert calledOutsideEDT() : "on EDT";

        try {
            tests.run();
        } finally {
            keyboard.releaseModifierKeys();
        }
    }

    void configureRobotDelay() {
        // for example -Drobot.delay.millis=500 could be added to
        // the command line to slow it down
        String customDelay = System.getProperty("robot.delay.millis");
        int delayMs = customDelay != null ?
            Integer.parseInt(customDelay) :
            DEFAULT_ROBOT_DELAY;
        setRobotDelay(delayMs);
    }

    private void setRobotDelay(int millis) {
        robot.settings().delayBetweenEvents(millis);
        robot.settings().eventPostingDelay(2 * millis);
    }

    void runSlowly() {
        setRobotDelay(SLOW_ROBOT_DELAY);
    }

    void exit() {
        countDownBeforeExit();
        restoreInitialSettings();
        
        String exitMenuName = JVM.isMac ? "Quit" : "Exit";
        runMenuCommand(exitMenuName);
        findJOptionPane("Unsaved Changes").yesButton().click();
    }

    private static void countDownBeforeExit() {
        final int secondsToWait = 5;
        int remainingSeconds = secondsToWait;
        do {
            System.out.print(remainingSeconds + "...");
            GUIUtils.updateTaskbarProgress((int) (100 * (secondsToWait - remainingSeconds) / (double) secondsToWait));
            Utils.sleep(1, SECONDS);
        } while (--remainingSeconds > 0);
    }

    public void testBrushSettings(Tool tool, BrushType brushType) {
        var settingsButton = findButtonByText("Settings...");
        if (brushType.hasSettings()) {
            settingsButton.requireEnabled().click();
            var dialog = findDialogByTitleStartingWith("Settings for the");
            testBrushSettingsDialog(dialog, tool, brushType);
        } else {
            settingsButton.requireDisabled();
        }
    }

    private static void testBrushSettingsDialog(DialogFixture dialog, Tool tool, BrushType brushType) {
        switch (brushType) {
            case HARD, SOFT, WOBBLE, REALISTIC, HAIR -> {
                assert !brushType.hasSettings();
            }
            case CALLIGRAPHY -> slideRandomly(dialog.slider("angle"));
            case SHAPE -> testShapeBrushSettings(dialog);
            case SPRAY -> testSprayBrushSettings(dialog, tool == ERASER);
            case CONNECT -> testConnectBrushSettings(dialog);
            case OUTLINE_CIRCLE, OUTLINE_SQUARE -> checkRandomly(dialog.checkBox("dependsOnSpeed"));
            case ONE_PIXEL -> checkRandomly(dialog.checkBox("aa"));
        }

        dialog.button("ok").click();
    }

    private static void testShapeBrushSettings(DialogFixture dialog) {
        chooseRandomly(dialog.comboBox("shape"));
        slideRandomly(dialog.slider("spacing"));
        slideRandomly(dialog.slider("angleJitter"));
        checkRandomly(dialog.checkBox("angled"));
    }

    private static void testSprayBrushSettings(DialogFixture dialog, boolean isEraser) {
        chooseRandomly(dialog.comboBox("shape"));
        slideRandomly(dialog.slider("avgRadius"));
        slideRandomly(dialog.slider("radiusVar"));
        slideRandomly(dialog.slider("flow"));
        checkRandomly(dialog.checkBox("rndOpacity"));
        slideRandomly(dialog.slider("flow"));
        if (!isEraser) {
            slideRandomly(dialog.slider("colorRand"));
        }
    }

    private static void testConnectBrushSettings(DialogFixture dialog) {
        chooseRandomly(dialog.comboBox("style"));
        slideRandomly(dialog.slider("density"));
        slideRandomly(dialog.slider("width"));
        checkRandomly(dialog.checkBox("resetForEach"));
        clickRandomly(0.1, dialog.button("resetHistNow"));
    }

    void setIndexedMode() {
        runMenuCommand("Indexed");
    }

    void clickTool(Tool tool) {
        pw.toggleButton(tool.getName() + " Button").click();

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

    public void createNewImage(int width, int height, String name) {
        runMenuCommand("New Image...");

        var dialog = findDialogByTitle("New Image");
        if (name != null) {
            dialog.textBox("nameTF").deleteText().enterText(name);
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
        if (name != null) {
            assertThat(activeCompName).isEqualTo(name);
        } else {
            assertThat(activeCompName).startsWith("Untitled");
        }
        assert !EDT.active(Composition::isDirty);

        mouse.updateCanvasBounds();
        checkNumLayersIs(1);
    }

    void duplicateLayer(boolean undoRedo) {
        runMenuCommand("Duplicate Layer");

        if (undoRedo) {
            keyboard.undoRedo("Duplicate Layer");
        }
    }

    void runMenuCommand(String text) {
        assert calledOutsideEDT() : callInfo();

        findMenuItemByText(text)
            .requireEnabled()
            .click();
        Utils.sleep(200, MILLISECONDS);
    }

    void runMenuCommandByName(String name) {
        assert calledOutsideEDT() : callInfo();

        pw.menuItem(name)
            .requireEnabled()
            .click();
        Utils.sleep(200, MILLISECONDS);
    }

    public void selectLayerBelow() {
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
        var optionPane = findJOptionPane("Confirmation"); // overwrite question
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

        // make sure that the opening task is started
        Utils.sleep(200, MILLISECONDS);

        // wait until the async open completes
        IOTasks.waitForIdle();
        mouse.updateCanvasBounds();

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
        boolean stillWriting = EDT.call(IOTasks::hasActiveWrites);
        while (stillWriting) {
            System.out.println("waiting 1s for the IO thread...");
            Utils.sleep(1, SECONDS);
            stillWriting = EDT.call(IOTasks::hasActiveWrites);
        }
    }

    void closeCurrentView(ExpectConfirmation expectConfirmation) {
        boolean unsaved = EDT.active(Composition::hasUnsavedChanges);

        if (unsaved && expectConfirmation == ExpectConfirmation.NO
            || !unsaved && expectConfirmation == ExpectConfirmation.YES) {
            throw new IllegalStateException("unsaved = " + unsaved + ", expectConfirmation = " + expectConfirmation);
        }

        boolean expectDialog = switch (expectConfirmation) {
            case YES -> true;
            case NO -> false;
            case UNKNOWN -> unsaved;
        };

        runMenuCommand("Close");

        if (expectDialog) {
            findJOptionPane("Unsaved Changes")
                .buttonWithText("Don't Save").click();
        }
    }

    void closeAll() {
        runMenuCommand("Close All");

        // close all warnings
        boolean warnings = true;
        while (warnings) {
            try {
                findJOptionPane("Unsaved Changes")
                    .buttonWithText("Don't Save").click();
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

        keyboard.undoRedo("Resize");
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

    public void runModifySelection(SelectionModifyType type, int amount) {
        runMenuCommand("Modify Selection...");
        var dialog = findDialogByTitle("Modify Selection");

        dialog.slider("amount").slideTo(amount);
        dialog.comboBox("type").selectItem(type.toString());

        dialog.button("ok").click();
        dialog.requireNotVisible();

        keyboard.undoRedo("Modify Selection");
    }

    static void clickPopupMenu(JPopupMenuFixture popupMenu, String text) {
        clickPopupMenu(popupMenu, text, true);
    }

    static void clickPopupMenu(JPopupMenuFixture popupMenu, String text, boolean onlyIfVisible) {
        GUITestUtils.findPopupMenuItemByText(popupMenu, text, onlyIfVisible)
            .requireEnabled()
            .click();
    }

    void expectAndCloseErrorDialog() {
        var errorDialog = findDialogByTitle("Error");
        GUITestUtils.findButtonByText(errorDialog, "OK").click();
        errorDialog.requireNotVisible();
    }

    static String getCurrentTimeHM() {
        return TIME_FORMAT_HM.format(LocalTime.now());
    }

    static String getCurrentTimeHMS() {
        return TIME_FORMAT_HMS.format(LocalTime.now());
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
        return WindowFinder.findDialog("filterDialog")
            .withTimeout(1, TimeUnit.MINUTES)
            .using(robot);
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

    JOptionPaneFixture findJOptionPane(String expectedTitle) {
        JOptionPaneFixture pane = JOptionPaneFinder.findOptionPane()
            .withTimeout(10, SECONDS)
            .using(robot);
        if (expectedTitle != null) {
            pane.requireTitle(expectedTitle);
        }
        return pane;
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
        return GUITestUtils.findButtonByText(pw, text);
    }

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

            var pane = findJOptionPane("Preset Name");
            pane.textBox().enterText(presetName);
            pane.okButton().click();

            if (alreadyExists) {
                pane = findJOptionPane("Preset Exists");
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

    private static void rememberInitialSettings() {
        initialOpenDir = Dirs.getLastOpen();
        initialSaveDir = Dirs.getLastSave();
    }

    private static void restoreInitialSettings() {
        Dirs.setLastOpen(initialOpenDir);
        Dirs.setLastSave(initialSaveDir);
    }

    public void addLayerMask() {
        pw.button("addLayerMask")
            .requireEnabled()
            .click()
            .requireDisabled();
        assert EDT.activeLayerHasMask();

        keyboard.undoRedo("Add Layer Mask");
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

    public void addEmptyImageLayer(boolean below) {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();

        if (below) {
            keyboard.pressCtrl();
        }
        pw.button("addLayer").click();
        if (below) {
            keyboard.releaseCtrl();
        }

        undoRedoNewLayer(numLayersBefore, "New Empty Layer");
    }

    public void addTextLayer(String text, Consumer<DialogFixture> customizer, String expectedText) {
        int numLayersBefore = EDT.getNumLayersInActiveHolder();
        pw.button("addTextLayer").click();

        var dialog = findDialogByTitle("Create Text Layer");
        dialog.textBox("textArea")
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
        GUITestUtils.findButtonByText(colorSelector, "OK").click();
        keyboard.undoRedo("Add Color Fill Layer");

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

    public void drawGradient(GradientType gradientType,
                             GradientColorType colorType,
                             CanvasDrag dragLocation,
                             Color fgColor, Color bgColor) {
        clickTool(Tools.GRADIENT);

        pw.comboBox("typeCB").selectItem(gradientType.toString());
        pw.comboBox("colorTypeCB").selectItem(colorType.toString());
        pw.checkBox("reverseCB").uncheck();

        EDT.setFgBgColors(fgColor, bgColor);
        mouse.drag(dragLocation);

        if (EDT.isActiveLayerType(GradientFillLayer.class)) {
            keyboard.undoRedo("Gradient Fill Layer Change");
            // gradient fill layers don't hide handles
        } else {
            keyboard.undoRedo("Create Gradient");

            keyboard.pressEsc(); // hide the gradient handles
            keyboard.undoRedo("Hide Gradient Handles");
        }
    }

    public void drawGradientFromCenter(GradientType gradientType) {
        clickTool(Tools.GRADIENT);

        pw.comboBox("typeCB").selectItem(gradientType.toString());
        pw.checkBox("reverseCB").check();

        if (EDT.getZoomLevelOfActive() != ZoomLevel.ACTUAL_SIZE) {
            // otherwise location on screen can lead to crazy results
            runMenuCommand("Actual Pixels");
        }

        mouse.dragFromCanvasCenterToTheRight();
        keyboard.undoRedo("Create Gradient");

        keyboard.pressEsc(); // hide the gradient handles
        keyboard.undoRedo("Hide Gradient Handles");
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
        if (historyChecker != null) {
            historyChecker.setMaxUntestedEdits(2);
        }

        pw.button("addAdjLayer").click();
        JPopupMenuFixture popup = new JPopupMenuFixture(robot, robot.findActivePopupMenu());
        clickPopupMenu(popup, "New " + filterName, true);
        DialogFixture dialog = findDialogByTitle(filterName);
        customizer.accept(dialog);
        dialog.button("ok").click();

        keyboard.undo(filterName + " Changed");
        keyboard.undoRedo("New Adjustment Layer");
        keyboard.redo(filterName + " Changed");

        if (historyChecker != null) {
            historyChecker.setMaxUntestedEdits(1);
        }
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
        String newValueString = String.valueOf(toPercentage(newValue));
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

    public void setMaxUntestedEdits(int newLimit) {
        if (historyChecker != null) {
            historyChecker.setMaxUntestedEdits(newLimit);
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

    // Whether a "randomize settings" button should be tested for a filter.
    public enum Randomize {YES, NO}

    // Whether a "reseed" button should be tested for a filter.
    public enum Reseed {YES, NO}

    // Whether a "show original" checkbox should be tested for a filter.
    public enum ShowOriginal {YES, NO}

    // Whether we expect a save modified image confirmation dialog
    public enum ExpectConfirmation {
        YES,
        NO,
        UNKNOWN
    }
}
