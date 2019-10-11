/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition;
import pixelitor.gui.PixelitorWindow;
import pixelitor.tools.Tool;
import pixelitor.utils.Utils;
import pixelitor.utils.test.PixelitorEventListener;

import javax.swing.*;
import java.awt.EventQueue;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A utility class for running Pixelitor with assertj-swing based tests
 */
public class AppRunner {
    public static final int ROBOT_DELAY_DEFAULT = 50; // millis
    public static final int ROBOT_DELAY_SLOW = 300; // millis
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("HH:mm"));

    private final Robot robot;
    private final Mouse mouse;
    private final Keyboard keyboard;
    private final FrameFixture pw;

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

        pw = WindowFinder.findFrame(PixelitorWindow.class)
                .withTimeout(30, SECONDS)
                .using(robot);
        mouse = new Mouse(pw, robot);
        keyboard = new Keyboard(pw, robot, this);

        // wait even after the frame is shown to
        // make sure that the image is also loaded
        Composition comp = EDT.getComp();
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
        String exitMenuName = JVM.isMac ? "Quit" : "Exit";
        runMenuCommand(exitMenuName);
        findJOptionPane().yesButton().click();
    }

    void clickTool(Tool tool) {
        pw.toggleButton(tool.getName() + " Tool Button").click();
        Utils.sleep(200, MILLISECONDS);
        EDT.assertActiveToolIs(tool);
    }

    void runMenuCommand(String text) {
        findMenuItemByText(text).click();
    }

    void saveWithOverwrite(File baseTestingDir, String fileName) {
        JFileChooserFixture saveDialog = findSaveFileChooser();
        saveDialog.selectFile(new File(baseTestingDir, fileName));
        saveDialog.approve();
        // say OK to the overwrite question
        JOptionPaneFixture optionPane = findJOptionPane();
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
    }

    void closeAll() {
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

        EDT.assertNumOpenImagesIs(0);
    }

    void resize(int targetWidth) {
        runMenuCommand("Resize...");
        DialogFixture dialog = findDialogByTitle("Resize");

        dialog.textBox("widthTF")
                .deleteText()
                .enterText(String.valueOf(targetWidth));

        // no need to also set the height, because
        // constrain proportions is checked by default

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    void resize(int targetWidth, int targetHeight) {
        runMenuCommand("Resize...");
        DialogFixture dialog = findDialogByTitle("Resize");

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
    }

    static void clickPopupMenu(JPopupMenuFixture popupMenu, String text) {
        findPopupMenuFixtureByText(popupMenu, text)
                .requireEnabled()
                .click();
    }

    void expectAndCloseErrorDialog() {
        DialogFixture errorDialog = findDialogByTitle("Error");
        findButtonByText(errorDialog, "OK").click();
        errorDialog.requireNotVisible();
    }

    static String getCurrentTime() {
        return DATE_FORMAT.get().format(new Date());
    }

    JMenuItemFixture findMenuItemByText(String guiName) {
        return new JMenuItemFixture(robot, robot.finder().find(new GenericTypeMatcher<JMenuItem>(JMenuItem.class) {
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
        return new DialogFixture(robot, robot.finder().find(new GenericTypeMatcher<JDialog>(JDialog.class) {
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
        return new DialogFixture(robot, robot.finder().find(new GenericTypeMatcher<JDialog>(JDialog.class) {
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
        JButtonMatcher matcher = JButtonMatcher.withText(text).andShowing();
        return container.button(matcher);
    }

    public JButtonFixture findButton(String name) {
        return pw.button(name);
    }

    public JButtonFixture findButtonByText(String text) {
        return findButtonByText(pw, text);
    }

    static JMenuItemFixture findPopupMenuFixtureByText(JPopupMenuFixture popupMenu, String text) {
        JMenuItemFixture menuItemFixture = popupMenu.menuItem(
                new GenericTypeMatcher<JMenuItem>(JMenuItem.class) {
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
        JButtonFixture buttonFixture = container.button(
                new GenericTypeMatcher<JButton>(JButton.class) {
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

        return buttonFixture;
    }

    static JButtonFixture findButtonByToolTip(ComponentContainerFixture container, String toolTip) {
        JButtonFixture buttonFixture = container.button(
                new GenericTypeMatcher<JButton>(JButton.class) {
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
}
