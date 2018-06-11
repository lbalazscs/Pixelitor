/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Pixelitor;
import pixelitor.filters.gui.FilterParam;
import pixelitor.gui.PixelitorWindow;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Window;

/**
 * Static GUI-related utility methods
 */
public final class GUIUtils {
    private GUIUtils() {
    }

    public static void testJDialog(JDialog d) {
        JComponent contentPane = (JComponent) d.getContentPane();
        testJComponent(contentPane);
    }

    public static void testJComponent(JComponent p) {
        Runnable runnable = () -> {
            try {
                String lookAndFeelClass = Pixelitor.getLFClassName();
                UIManager.setLookAndFeel(lookAndFeelClass);
            } catch (Exception e) {
                Messages.showException(e);
            }

            JFrame frame = new JFrame("Test");

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            frame.add(p, BorderLayout.CENTER);

            SwingUtilities.updateComponentTreeUI(frame);

            frame.pack();
            centerOnScreen(frame);
            frame.setVisible(true);
        };
        EventQueue.invokeLater(runnable);
    }

    public static void centerOnScreen(Component component) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        Dimension window = component.getSize();

        if (window.height > screen.height) {
            window.height = screen.height;
        }

        if (window.width > screen.width) {
            window.width = screen.width;
        }

        // center it
        component.setLocation((screen.width - window.width) / 2,
                (screen.height - window.height) / 2);

        // if it was bigger than the screen, restrict it to screen size
        component.setSize(window);
    }

    /**
     * @return true if any app window has focus
     */
    public static boolean appHasFocus() {
        return Utils.anyMatch(Window.getWindows(), Window::isActive);
    }

    public static void showClipboardTextDialog(JComponent form, String title, String text) {
        new DialogBuilder()
                .okText("Copy as Text to the Clipboard")
                .cancelText("Close")
                .okAction(() -> Utils.copyStringToClipboard(text))
                .title(title)
                .form(form)
                .parent(PixelitorWindow.getInstance())
                .show();
    }

    public static JPanel arrangeParamsInVerticalGridBag(Iterable<FilterParam> params) {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());

        int row = 0;

        GridBagHelper gbh = new GridBagHelper(p);
        for (FilterParam param : params) {
            JComponent control = param.createGUI();

            int numColumns = param.getNumGridBagCols();
            if (numColumns == 1) {
                gbh.addOnlyControlToRow(control, row);
            } else if (numColumns == 2) {
                gbh.addLabel(param.getName() + ':', 0, row);
                gbh.addLastControl(control);
            }

            row++;
        }
        return p;
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

    public static void showDialog(JDialog d) {
        centerOnScreen(d);
        d.setVisible(true);
    }

    public static void closeDialog(JDialog d) {
        if (d != null && d.isVisible()) {
            d.setVisible(false);
            d.dispose();
        }
    }
}
