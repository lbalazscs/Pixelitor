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

package pixelitor.gui.utils;

import com.bric.util.JVM;
import pixelitor.Composition;
import pixelitor.filters.gui.FilterSetting;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.Resettable;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.PixelitorWindow;
import pixelitor.io.IO;
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

import static java.awt.FlowLayout.CENTER;
import static java.awt.FlowLayout.RIGHT;
import static java.awt.Taskbar.Feature.PROGRESS_VALUE_WINDOW;
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
 * Static GUI-related utility methods
 */
public final class GUIUtils {
    private static final int BUSY_CURSOR_DELAY = 300; // in milliseconds

    private GUIUtils() {
    }

    private static final Map<String, Point> lastDialogLocationsByTitle = new HashMap<>();

    /**
     * @return true if any app window has focus
     */
    public static boolean appHasFocus() {
        return Utils.anyMatch(Window.getWindows(), Window::isActive);
    }

    public static void showCopyTextToClipboardDialog(JComponent content,
                                                     String text,
                                                     String title) {
        new DialogBuilder()
            .okText("Copy as Text to the Clipboard")
            .cancelText(CLOSE_DIALOG)
            .validator(d -> {
                Utils.copyStringToClipboard(text);
                return false; // prevents the dialog from closing
            })
            .title(title)
            .content(content)
            .show();
    }

    public static JPanel arrangeVertically(ParamSet paramSet) {
        var p = new JPanel();
        arrangeVertically(p, paramSet);
        return p;
    }

    public static void arrangeVertically(JPanel p, ParamSet paramSet) {
        p.setLayout(new GridBagLayout());
        var gbh = new GridBagHelper(p);
        gbh.arrangeVertically(paramSet.getParams());
    }

    public static JPanel arrangeVertically(Iterable<? extends FilterSetting> settings) {
        var p = new JPanel();
        arrangeVertically(p, settings);
        return p;
    }

    public static void arrangeVertically(JPanel p, Iterable<? extends FilterSetting> settings) {
        p.setLayout(new GridBagLayout());
        var gbh = new GridBagHelper(p);
        gbh.arrangeVertically(settings);
    }

    public static Container getTopContainer(Container c) {
        while (c.getParent() != null) {
            c = c.getParent();
            if (c instanceof Dialog) {
                // don't jump from dialogs to their parents
                return c;
            }
        }
        return c;
    }

    public static JDialog getDialogAncestor(Component c) {
        return (JDialog) SwingUtilities.getWindowAncestor(c);
    }

    public static void showDialog(JDialog d, JComponent parent) {
        Point loc = lastDialogLocationsByTitle.get(d.getTitle());
        if (loc != null) {
            d.setLocation(loc);
        } else {
            d.setLocationRelativeTo(parent);
        }
        d.setVisible(true);
    }

    public static void showDialog(JDialog d) {
        showDialog(d, SCREEN_CENTER);
    }

    public static void showDialog(JDialog d, Screens.Align align) {
        Point loc = lastDialogLocationsByTitle.get(d.getTitle());
        if (loc != null) {
            d.setLocation(loc);
        } else {
            Screens.position(d, align);
        }

        d.setVisible(true);
    }

    public static void closeDialog(JDialog d, boolean dispose) {
        if (d != null && d.isVisible()) {
            lastDialogLocationsByTitle.put(d.getTitle(), d.getLocationOnScreen());
            d.setVisible(false);
            // dispose should not be called if the dialog will be re-shown
            // because then AssertJ-Swing doesn't find it even if it is there
            if (dispose) {
                d.dispose();
            }
        }
    }

    public static void randomizeWidgetsOn(JPanel panel) {
        int count = panel.getComponentCount();

        for (int i = 0; i < count; i++) {
            Component child = panel.getComponent(i);
            //noinspection ChainOfInstanceofChecks
            if (child instanceof JComboBox box) {
                int itemCount = box.getItemCount();
                box.setSelectedIndex(Rnd.nextInt(itemCount));
            } else if (child instanceof JCheckBox box) {
                box.setSelected(Rnd.nextBoolean());
            } else if (child instanceof SliderSpinner spinner) {
                spinner.getModel().randomize();
            } else if (child instanceof BlendingModePanel bmp) {
                bmp.randomize();
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
            Messages.showExceptionOnEDT(e);
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

    public static void setupCancelWhenTheDialogIsClosed(JDialog d, Runnable cancelAction) {
        d.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // the user pressed the X button...
                cancelAction.run();
            }
        });
    }

    public static void setupCancelWhenEscIsPressed(JDialog d, Runnable cancelAction) {
        JComponent contentPane = (JComponent) d.getContentPane();
        contentPane.registerKeyboardAction(e -> cancelAction.run(),
            ESC, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public static void runWithBusyCursor(Runnable task) {
        runWithBusyCursor(PixelitorWindow.get(), task);
    }

    public static void runWithBusyCursor(Component parent, Runnable task) {
        java.util.Timer timer = new Timer();
        var startBusyCursorTask = new TimerTask() {
            @Override
            public void run() {
                parent.setCursor(BUSY);
            }
        };

        try {
            // if after BUSY_CURSOR_DELAY the original task is still running,
            // set the cursor to the delay cursor
            timer.schedule(startBusyCursorTask, BUSY_CURSOR_DELAY);
            task.run(); // on the current thread!
        } finally {
            // when the original task has stopped running, the cursor is reset
            timer.cancel();
            parent.setCursor(DEFAULT);
        }
    }

    public static void shareScrollModels(JScrollPane from, JScrollPane to) {
        var sharedVerticalModel = from.getVerticalScrollBar().getModel();
        to.getVerticalScrollBar().setModel(sharedVerticalModel);

        var sharedHorizontalModel = from.getHorizontalScrollBar().getModel();
        to.getHorizontalScrollBar().setModel(sharedHorizontalModel);
    }

    public static void addColorDialogListener(JComponent colorSwatch, Runnable showDialogAction) {
        colorSwatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // on Linux/Mac the popup trigger check is not enough
                // probably because the popups are started by mousePressed
                boolean showDialog = colorSwatch.isEnabled()
                    && !e.isPopupTrigger()
                    && SwingUtilities.isLeftMouseButton(e);

                if (showDialog) {
                    showDialogAction.run();
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
            Runtime.getRuntime().exec("explorer.exe /select," + path);
        } else if (JVM.isMac) {
            Runtime.getRuntime().exec("open -R " + path);
        } else {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                    desktop.browseFileDirectory(file);
                } else if (desktop.isSupported(Desktop.Action.OPEN)) {
                    // just open the parent directory
                    File dir = file.getParentFile();
                    desktop.open(dir);
                }
                // else give up
            }
        }
    }

    public static AbstractAction createPrintFileAction(Composition comp,
                                                       File file, Component parent) {
        return new PAction("Print...") {
            @Override
            public void onClick() {
                if (comp.isDirty()) {
                    String msg = "<html>The file <i>" + file.getName() +
                        "</i> contains unsaved changes.<br>" +
                        "Only the saved changes can be printed.<br>" +
                        "Do you want to save your changes now?";

                    String[] options = {"Save and Print", GUIText.CANCEL};
                    boolean saveAndPrint = Dialogs.showOKCancelDialog(parent, msg,
                        "Unsaved Changes", options, 0, QUESTION_MESSAGE);
                    if (!saveAndPrint) {
                        return;
                    }

                    IO.save(comp, false);
                }
                try {
                    Desktop.getDesktop().print(file);
                } catch (IOException ex) {
                    Messages.showException(ex);
                }
            }
        };
    }

    public static void showTaskbarProgress(int progressPercent) {
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

    public static JButton createResetAllButton(ActionListener action) {
        JButton button = new JButton("Reset all", Icons.getWestArrowIcon());
        button.setToolTipText(Resettable.RESET_ALL_TOOLTIP);
        button.addActionListener(action);
        return button;
    }

    public static JButton createResetChannelButton(ActionListener action) {
        JButton resetChannel = new JButton("Reset channel", Icons.getWestArrowIcon());
        resetChannel.addActionListener(action);
        return resetChannel;
    }

    public static <E> JComboBox<E> createComboBox(ComboBoxModel<E> model,
                                                  ActionListener al) {
        JComboBox<E> cb = new JComboBox<>(model);
        cb.addActionListener(al);

        // make sure all values are visible without a scrollbar
        cb.setMaximumRowCount(model.getSize());

        return cb;
    }

    public static <E> JComboBox<E> createComboBox(E[] values) {
        JComboBox<E> cb = new JComboBox<>(values);

        // make sure all values are visible without a scrollbar
        cb.setMaximumRowCount(values.length);

        return cb;
    }

    public static Font createFont(String family, int size, boolean bold, boolean italic) {
        int style = Font.PLAIN;
        if (bold) {
            style |= Font.BOLD;
        }
        if (italic) {
            style |= Font.ITALIC;
        }
        return new Font(family, style, size);
    }

    public static void removeAllMouseListeners(JComponent c) {
        MouseListener[] mouseListeners = c.getMouseListeners();
        for (MouseListener mouseListener : mouseListeners) {
            c.removeMouseListener(mouseListener);
        }
    }
}
