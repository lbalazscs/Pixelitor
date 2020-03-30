/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.ComponentContainerFixture;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.fixture.JMenuItemFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JPopupMenuFixture;
import org.assertj.swing.launcher.ApplicationLauncher;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.gui.PixelitorWindow;
import pixelitor.io.Dirs;
import pixelitor.io.IOThread;
import pixelitor.selection.SelectionModifyType;
import pixelitor.tools.Tool;
import pixelitor.utils.Utils;
import pixelitor.utils.test.PixelitorEventListener;

import javax.swing.*;
import java.awt.EventQueue;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A utility class for running Pixelitor with assertj-swing based tests
 */
public class AppRunner {
    public static final int ROBOT_DELAY_DEFAULT = 50; // millis
    public static final int ROBOT_DELAY_SLOW = 300; // millis
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_HM = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("HH:mm"));
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_HMS = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    private final Robot robot;
    private final Mouse mouse;
    private final Keyboard keyboard;
    private final FrameFixture pw;

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

        if (fileNames.length == 0) {
            return;
        }

        // wait even after the frame is shown to
        // make sure that the image is also loaded
        var comp = EDT.getComp();
        while (comp == null) {
            Utils.sleep(1, SECONDS);
            comp = EDT.getComp();
        }
        mouse.recalcCanvasBounds();
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
        assert !EventQueue.isDispatchThread();

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

    void delayBetweenEvents(int millis) {
        robot.settings().delayBetweenEvents(millis);
        robot.settings().eventPostingDelay(2 * millis);
    }

    void exit() {
        restoreNormalSettings();
        String exitMenuName = JVM.isMac ? "Quit" : "Exit";
        runMenuCommand(exitMenuName);
        findJOptionPane().yesButton().click();
    }

    void clickTool(Tool tool) {
        pw.toggleButton(tool.getName() + " Tool Button").click();
        //EDT.run(() -> Tools.changeTo(tool));

        Utils.sleep(200, MILLISECONDS);
        EDT.assertActiveToolIs(tool);
    }

    void runMenuCommand(String text) {
//        JMenuItemFixture menuItem = EDT.call(() -> findMenuItemByText(text));
//        menuItem.click();

        findMenuItemByText(text).click(); //
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
        boolean stillWriting = EDT.call(IOThread::isBusyWriting);
        while (stillWriting) {
            System.out.println("waiting 1s for the IO thread...");
            Utils.sleep(1, SECONDS);
            stillWriting = EDT.call(IOThread::isBusyWriting);
        }
    }

    void closeAll() {
        runMenuCommand("Close All");

        // close all warnings
        boolean warnings = true;
        while (warnings) {
            try {
                var optionPane = findJOptionPane();
                // click "Don't Save"
                optionPane.button(new GenericTypeMatcher<>(JButton.class) {
                    @Override
                    protected boolean isMatching(JButton button) {
                        return button.getText().equals("Don't Save");
                    }
                }).click();
            } catch (Exception e) { // no more JOptionPane found
                warnings = false;
            }
        }

        EDT.assertNumOpenImagesIs(0);
    }

    void resize(int targetWidth) {
        runMenuCommand("Resize...");
        var dialog = findDialogByTitle("Resize");

        dialog.textBox("widthTF")
                .deleteText()
                .enterText(String.valueOf(targetWidth));

        // no need to also set the height, because
        // constrain proportions is checked by default

        dialog.button("ok").click();
        dialog.requireNotVisible();

        Utils.sleep(5, SECONDS);
    }

    void resize(int targetWidth, int targetHeight) {
        runMenuCommand("Resize...");
        var dialog = findDialogByTitle("Resize");

        dialog.textBox("widthTF")
                .deleteText()
                .enterText(String.valueOf(targetWidth));

        // disable constrain proportions
        dialog.checkBox().uncheck();

        dialog.textBox("heightTF")
                .deleteText()
                .enterText(String.valueOf(targetHeight));

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
            findButtonByText(dialog, "Change!").click();
        }

        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    static void clickPopupMenu(JPopupMenuFixture popupMenu, String text) {
        findPopupMenuFixtureByText(popupMenu, text)
                .requireEnabled()
                .click();
    }

    void expectAndCloseErrorDialog() {
        var errorDialog = findDialogByTitle("Error");
        findButtonByText(errorDialog, "OK").click();
        errorDialog.requireNotVisible();
    }

    static String getCurrentTimeHM() {
        return DATE_FORMAT_HM.get().format(new Date());
    }

    static String getCurrentTimeHMS() {
        return DATE_FORMAT_HMS.get().format(new Date());
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
                return "Matcher for JDialogs with title = " + start;
            }
        }));
    }

    JOptionPaneFixture findJOptionPane() {
        return JOptionPaneFinder.findOptionPane().withTimeout(10, SECONDS).using(robot);
    }

    JFileChooserFixture findSaveFileChooser() {
        return JFileChooserFinder.findFileChooser("save").using(robot);
    }

    static JButtonFixture findButtonByText(ComponentContainerFixture container, String text) {
        var matcher = JButtonMatcher.withText(text).andShowing();
        return container.button(matcher);
    }

    public JButtonFixture findButton(String name) {
        return pw.button(name);
    }

    public JButtonFixture findButtonByText(String text) {
        return findButtonByText(pw, text);
    }

    static JMenuItemFixture findPopupMenuFixtureByText(JPopupMenuFixture popupMenu, String text) {
        var menuItemFixture = popupMenu.menuItem(
                new GenericTypeMatcher<>(JMenuItem.class) {
                    @Override
                    protected boolean isMatching(JMenuItem menuItem) {
                        if (!menuItem.isShowing()) {
                            return false; // not interested in menuItems that are not currently displayed
                        }
                        String menuItemText = menuItem.getText();
                        if (menuItemText == null) {
                            menuItemText = "";
                        }
                        return menuItemText.equals(text);
                    }

                    @Override
                    public String toString() {
                        return "[Popup menu item Matcher, text = " + text + "]";
                    }
                });

        return menuItemFixture;
    }

    static JButtonFixture findButtonByActionName(ComponentContainerFixture container, String actionName) {
        return container.button(
                new GenericTypeMatcher<>(JButton.class) {
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
    }

    static JButtonFixture findButtonByToolTip(ComponentContainerFixture container, String toolTip) {
        var buttonFixture = container.button(
                new GenericTypeMatcher<>(JButton.class) {
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
                        return "[Button Tooltip Matcher, tooltip = " + toolTip + "]";
                    }
                });

        return buttonFixture;
    }

    // waits until the IO thread not busy
    public static void waitForIO() {
        // make sure that the task started executing
        Utils.sleep(500, MILLISECONDS);

        // waiting until an empty task finishes works
        // because the IO thread pool has a single thread
        var executor = (ExecutorService) IOThread.getExecutor();
        var future = executor.submit(() -> {});
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void runFilterWithDialog(String name, Randomize randomize, Reseed reseed, ShowOriginal checkShowOriginal, String... extraButtonsToClick) {
        runMenuCommand(name + "...");
        var dialog = findFilterDialog();

        for (String buttonText : extraButtonsToClick) {
            findButtonByText(dialog, buttonText)
                    .requireEnabled()
                    .click();
        }

        if (randomize == Randomize.YES) {
            dialog.button("randomize").click();
            dialog.button("resetAll").click();
            dialog.button("randomize").click();
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
