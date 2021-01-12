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
import pixelitor.colors.FgBgColorSelector;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.gui.PixelitorWindow;
import pixelitor.io.Dirs;
import pixelitor.io.IOTasks;
import pixelitor.selection.SelectionModifyType;
import pixelitor.tools.BrushType;
import pixelitor.tools.Tool;
import pixelitor.utils.Language;
import pixelitor.utils.Utils;
import pixelitor.utils.test.PixelitorEventListener;

import javax.swing.*;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.guitest.AJSUtils.*;
import static pixelitor.tools.BrushType.*;
import static pixelitor.tools.Tools.ERASER;
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

        if (fileNames.length == 0) {
            return;
        }

        waitForImageLoading();
        mouse.recalcCanvasBounds();

        if (Language.getCurrent() != Language.ENGLISH) {
            throw new IllegalStateException("language is " + Language.getCurrent());
        }
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
        restoreNormalSettings();
        String exitMenuName = JVM.isMac ? "Quit" : "Exit";
        runMenuCommand(exitMenuName);
        findJOptionPane().yesButton().click();
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

    void clickTool(Tool tool) {
        pw.toggleButton(tool.getName() + " Tool Button").click();
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

    void runMenuCommand(String text) {
        assert calledOutsideEDT() : threadInfo();

        findMenuItemByText(text).click();
        Utils.sleep(200, MILLISECONDS);
    }

    void saveWithOverwrite(File baseTestingDir, String fileName) {
        var saveDialog = findSaveFileChooser();
        saveDialog.selectFile(new File(baseTestingDir, fileName));
        saveDialog.approve();
        // say OK to the overwrite question
        var optionPane = findJOptionPane();
        optionPane.yesButton().click();
    }

    void openFileWithDialog(File dir, String fileName) {
        JFileChooserFixture openDialog;
        runMenuCommand("Open...");
        openDialog = JFileChooserFinder.findFileChooser("open").using(robot);
        openDialog.selectFile(new File(dir, fileName));
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
        AJSUtils.findPopupMenuFixtureByText(popupMenu, text)
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

    JFileChooserFixture findSaveFileChooser() {
        return JFileChooserFinder.findFileChooser("save").using(robot);
    }

    public JButtonFixture findButton(String name) {
        return pw.button(name);
    }

    public JButtonFixture findButtonByText(String text) {
        return AJSUtils.findButtonByText(pw, text);
    }

    public void runFilterWithDialog(String name, Randomize randomize, Reseed reseed,
                                    ShowOriginal checkShowOriginal, boolean testPresets,
                                    String... extraButtonsToClick) {
        runMenuCommand(name + "...");
        var dialog = findFilterDialog();

        for (String buttonText : extraButtonsToClick) {
            AJSUtils.findButtonByText(dialog, buttonText)
                .requireEnabled()
                .click();
        }

        if (randomize == Randomize.YES) {
            boolean presetAdded = false;
            if (testPresets) {
                presetAdded = savePreset(dialog);
            }

            dialog.button("randomize").click();
            dialog.button("resetAll").click();
            dialog.button("randomize").click();

            if (presetAdded) {
                dialog.menuItem("test preset").click();
                // the filter should now be set to default again

                dialog.button("randomize").click();
            }
        }

        if (checkShowOriginal.isYes()) {
            dialog.checkBox("show original").click();
            dialog.checkBox("show original").click();
        }

        if (reseed == Reseed.YES) {
            dialog.button("reseed").click();
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    private boolean savePreset(DialogFixture filterDialog) {
        boolean presetAdded = false;
        JDialog realDialog = (JDialog) filterDialog.target();
        JMenuBar menuBar = realDialog.getJMenuBar();
        if (menuBar != null) {
            if (DialogMenuBar.PRESETS.equals(menuBar.getMenu(0).getText())) {
                filterDialog.menuItem("savePreset").click();
                var pane = findJOptionPane();
                pane.textBox().enterText("test preset");
                pane.okButton().click();
                presetAdded = true;
            }
        }
        return presetAdded;
    }

    void checkNumLayersIs(int expected) {
        EDT.assertNumLayersIs(expected);
        layersContainer.requireNumLayerButtons(expected);
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

    public enum Randomize {YES, NO}

    public enum Reseed {YES, NO}
}
