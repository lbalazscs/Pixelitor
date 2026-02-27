/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui.utils;

import com.bric.util.JVM;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.filters.gui.FilterSetting;
import pixelitor.filters.gui.Linkable;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.Resettable;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.PixelitorWindow;
import pixelitor.io.FileIO;
import pixelitor.utils.Icons;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import static java.awt.FlowLayout.CENTER;
import static java.awt.FlowLayout.RIGHT;
import static java.awt.Taskbar.Feature.PROGRESS_VALUE_WINDOW;
import static java.awt.event.ActionEvent.CTRL_MASK;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;
import static pixelitor.utils.Cursors.BUSY;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Keys.ESC;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * Static GUI-related utility methods.
 */
public final class GUIUtils {
    private GUIUtils() {
    }

    public static final boolean CAN_USE_FILE_MANAGER = Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);

    private static final int BUSY_CURSOR_DELAY_MS = 300;
    private static Component lastBusyCursorParent;

    private static final Map<String, Point> dialogLocationsByTitle = new HashMap<>();

    /**
     * Checks if any application window currently has focus.
     */
    public static boolean isAppFocused() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window.isActive()) {
                return true;
            }
        }
        return false;
    }

    public static void showCopyTextToClipboardDialog(JComponent content,
                                                     Supplier<String> textSupplier,
                                                     String title) {
        new DialogBuilder()
            .okText(GUIText.COPY_AS_JSON)
            .okAction(() -> Utils.copyStringToClipboard(textSupplier.get()))
            .keepOpenAfterOk()
            .cancelText(CLOSE_DIALOG)
            .title(title)
            .content(content)
            .show();
    }

    public static JPanel createVerticalPanel(ParamSet paramSet) {
        return createVerticalPanel(paramSet.getParams());
    }

    public static JPanel createVerticalPanel(Iterable<? extends FilterSetting> settings) {
        JPanel panel = new JPanel();
        arrangeVertically(panel, settings);
        return panel;
    }

    public static JPanel createHorizontalPanel(Iterable<? extends FilterSetting> settings) {
        JPanel panel = new JPanel(new FlowLayout(CENTER));
        for (FilterSetting setting : settings) {
            panel.add(new JLabel(setting.getName() + ":"));
            panel.add(setting.createGUI());
        }
        return panel;
    }

    public static void arrangeVertically(JPanel panel,
                                         Iterable<? extends FilterSetting> settings) {
        panel.setLayout(new GridBagLayout());
        new GridBagHelper(panel).arrangeVertically(settings);
    }

    public static void showDialog(JDialog dialog, JComponent parent) {
        setDialogLocation(dialog, parent);
        dialog.setVisible(true);
    }

    public static void setDialogLocation(JDialog dialog, JComponent parent) {
        Point lastLocation = dialogLocationsByTitle.get(dialog.getTitle());
        if (lastLocation != null) {
            dialog.setLocation(lastLocation);
        } else {
            dialog.setLocationRelativeTo(parent);
        }
    }

    public static void showDialog(JDialog d) {
        showDialog(d, SCREEN_CENTER);
    }

    public static void showDialog(JDialog dialog, Screens.Align align) {
        Point lastLocation = dialogLocationsByTitle.get(dialog.getTitle());
        if (lastLocation != null) {
            Screens.positionWindow(dialog, lastLocation);
        } else {
            Screens.positionWindow(dialog, align);
        }

        dialog.setVisible(true);
    }

    public static void closeDialog(JDialog dialog, boolean dispose) {
        if (dialog != null && dialog.isVisible()) {
            dialogLocationsByTitle.put(dialog.getTitle(), dialog.getLocationOnScreen());
            dialog.setVisible(false);
            // dispose should not be called if the dialog will be re-shown
            // because then AssertJ-Swing doesn't find it even if it's there
            if (dispose) {
                dialog.dispose();
            }
        }
    }

    public static void randomizeChildren(JPanel panel) {
        int count = panel.getComponentCount();

        for (int i = 0; i < count; i++) {
            Component child = panel.getComponent(i);
            switch (child) {
                case JComboBox<?> cb -> cb.setSelectedIndex(Rnd.nextInt(cb.getItemCount()));
                case JCheckBox cb -> cb.setSelected(Rnd.nextBoolean());
                case SliderSpinner spinner -> spinner.getModel().randomize();
                case BlendingModePanel bmp -> bmp.randomize();
                case null, default -> {
                }
            }
        }
    }

    public static void invokeAndWait(Runnable task) {
        assert calledOutsideEDT() : "on EDT";
        try {
            EventQueue.invokeAndWait(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Messages.showExceptionOnEDT(e);
        } catch (InvocationTargetException e) {
            Messages.showExceptionOnEDT(e.getCause());
        }
    }

    public static ProgressMonitor createPercentageProgressMonitor(String msg) {
        return new ProgressMonitor(PixelitorWindow.get(),
            msg, "", 0, 100);
    }

    public static void addOKCancelButtons(JPanel panel,
                                          JButton okButton, JButton cancelButton) {
        if (JVM.isMac) {
            panel.setLayout(new FlowLayout(RIGHT, 5, 5));
            panel.add(cancelButton);
            panel.add(okButton);
        } else {
            panel.setLayout(new FlowLayout(CENTER, 5, 5));
            panel.add(okButton);
            panel.add(cancelButton);
        }
    }

    /**
     * Prevents the given dialog from closing when the user
     * clicks the X button, and instead triggers the given action.
     */
    public static void setupCloseAction(JDialog d, Runnable action) {
        d.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                action.run();
            }
        });
    }

    /**
     * Registers the given action to be run when the
     * escape key is pressed in the given dialog.
     */
    public static void setupEscAction(JDialog d, Runnable action) {
        JComponent contentPane = (JComponent) d.getContentPane();
        contentPane.registerKeyboardAction(e -> action.run(),
            ESC, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public static void runWithBusyCursor(Runnable task) {
        runWithBusyCursor(task, PixelitorWindow.get());
    }

    public static void runWithBusyCursor(Runnable task, Component parent) {
        assert (parent.isShowing() && EventQueue.isDispatchThread())
            || AppMode.isUnitTesting();
        
        java.util.Timer timer = new Timer();
        var startBusyCursorTask = new TimerTask() {
            @Override
            public void run() {
                parent.setCursor(BUSY);
            }
        };

        int delay = BUSY_CURSOR_DELAY_MS;
        if (parent != lastBusyCursorParent) {
            // wait longer when a dialog is shown for the first time
            delay *= 2;
        }
        lastBusyCursorParent = parent;

        try {
            // if after BUSY_CURSOR_DELAY_MS the original task is
            // still running, set the cursor to the busy cursor
            timer.schedule(startBusyCursorTask, delay);
            task.run(); // on this thread!
        } finally {
            // reset the cursor when the original task has stopped running
            timer.cancel();
            parent.setCursor(DEFAULT);
        }
    }

    public static void synchronizeScrollPanes(JScrollPane from, JScrollPane to) {
        var sharedVerticalModel = from.getVerticalScrollBar().getModel();
        to.getVerticalScrollBar().setModel(sharedVerticalModel);

        var sharedHorizontalModel = from.getHorizontalScrollBar().getModel();
        to.getHorizontalScrollBar().setModel(sharedHorizontalModel);
    }

    /**
     * Adds button-like behavior to a component by running
     * an action when the left mouse button is released.
     */
    public static void addClickAction(JComponent component, Runnable action) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // on Linux/Mac the popup trigger check alone isn't enough
                // probably because the popups are started by mousePressed
                boolean shouldTriggerAction = component.isEnabled()
                    && !e.isPopupTrigger()
                    && SwingUtilities.isLeftMouseButton(e)
                    && component.contains(e.getPoint());

                if (shouldTriggerAction) {
                    action.run();
                }
            }
        });
    }

    public static Action createShowInFolderAction(File file) {
        return new AbstractAction("Show in Folder...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openFileInFolder(file);
                } catch (IOException ex) {
                    Messages.showException(ex);
                }
            }
        };
    }

    private static void openFileInFolder(File file) throws IOException {
        // this line should work in a platform-independent way, but
        // https://bugs.openjdk.java.net/browse/JDK-8233994
        // Desktop.getDesktop().browseFileDirectory(file);
        String path = file.getCanonicalPath();
        if (JVM.isWindows) {
            Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select," + path});
        } else if (JVM.isMac) {
            Runtime.getRuntime().exec(new String[]{"open", "-R", path});
        } else {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                    desktop.browseFileDirectory(file);
                } else if (desktop.isSupported(Desktop.Action.OPEN)) {
                    // just open the parent directory
                    File parentDir = file.getParentFile();
                    if (parentDir != null) {
                        desktop.open(parentDir);
                    }
                }
                // else give up
            }
        }
    }

    public static Action createPrintFileAction(Composition comp,
                                               File file, Component parent) {
        return new TaskAction("Print...", () -> {
            if (comp.isDirty()) {
                String msg = "<html>The file <i>" + file.getName() +
                    "</i> contains unsaved changes.<br>" +
                    "Only saved changes can be printed.<br>" +
                    "Do you want to save your changes now?";

                String[] options = {"Save and Print", GUIText.CANCEL};
                boolean saveAndPrint = Dialogs.showOKCancelDialog(parent, msg,
                    "Unsaved Changes", options, 0, QUESTION_MESSAGE);
                if (!saveAndPrint) {
                    return;
                }

                FileIO.save(comp, false);
            }
            try {
                Desktop.getDesktop().print(file);
            } catch (IOException ex) {
                Messages.showException(ex);
            }
        });
    }

    public static void updateTaskbarProgress(int progressPercent) {
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(PROGRESS_VALUE_WINDOW)) {
                taskbar.setWindowProgressValue(PixelitorWindow.get(), progressPercent);
            }
        }
    }

    public static void paintImmediately(JComponent c) {
        c.paintImmediately(0, 0, c.getWidth(), c.getHeight());
    }

    public static JButton createRandomizeSettingsButton(ActionListener action) {
        JButton button = new JButton("Randomize", Icons.getRandomizeIcon());
        button.setToolTipText(ParamSet.RANDOMIZE_BUTTON_TOOLTIP);
        button.addActionListener(action);
        button.setName("randomize");
        return button;
    }

    public static JButton createResetAllButton(ActionListener action) {
        JButton button = new JButton("Reset All", Icons.getResetIcon());
        button.setToolTipText(Resettable.RESET_ALL_TOOLTIP);
        button.addActionListener(action);
        button.setName("resetAll");
        return button;
    }

    public static JButton createResetChannelButton(ActionListener action) {
        JButton button = new JButton("Reset Channel", Icons.getResetIcon());
        button.addActionListener(action);
        return button;
    }

    public static <E> JComboBox<E> createComboBox(ComboBoxModel<E> model,
                                                  ActionListener al) {
        JComboBox<E> cb = new JComboBox<>(model);
        cb.addActionListener(al);

        // make sure all values are visible without a scrollbar
        cb.setMaximumRowCount(model.getSize());

        return cb;
    }

    public static <E> JComboBox<E> createComboBox(E[] values,
                                                  ActionListener al) {
        JComboBox<E> cb = new JComboBox<>(values);
        cb.addActionListener(al);

        // make sure all values are visible without a scrollbar
        cb.setMaximumRowCount(values.length);

        return cb;
    }

    public static void removeAllMouseListeners(JComponent c) {
        MouseListener[] mouseListeners = c.getMouseListeners();
        for (MouseListener mouseListener : mouseListeners) {
            c.removeMouseListener(mouseListener);
        }
    }

    public static void replaceMouseListeners(JComponent c, MouseListener newMouseListener) {
        removeAllMouseListeners(c);
        c.addMouseListener(newMouseListener);
    }

    public static JCheckBox createLinkCheckBox(Linkable linkable) {
        JCheckBox linkedCB = new JCheckBox();
        linkedCB.setModel(linkable.getLinkedModel());
        linkedCB.setToolTipText(linkable.createLinkedToolTip());
        linkedCB.addActionListener(e -> linkable.setLinked(linkedCB.isSelected()));
        return linkedCB;
    }

    public static boolean isCtrlPressed(ActionEvent e) {
        return e != null // could be null in unit tests
            && ((e.getModifiers() & CTRL_MASK) == CTRL_MASK);
    }
}
