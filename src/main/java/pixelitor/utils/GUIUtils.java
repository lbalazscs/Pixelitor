/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.utils;


//import pixelitor.menus.LookAndFeelMenu;

import pixelitor.PixelitorWindow;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;

public final class GUIUtils {

    /**
     * Utility class with static methods
     */
    private GUIUtils() {
    }


    public static void testJDialog(JDialog d) {
        JComponent contentPane = (JComponent) d.getContentPane();
        testJComponent(contentPane);
    }

    public static void testJComponent(final JComponent p) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String lookAndFeelClass = AppPreferences.getDefaultLookAndFeelClass();
                    UIManager.setLookAndFeel(lookAndFeelClass);
                } catch (Exception e) {
                    Dialogs.showExceptionDialog(e);
                }

                JFrame frame = new JFrame("Test");

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout());

                frame.add(p, BorderLayout.CENTER);


                SwingUtilities.updateComponentTreeUI(frame);

                frame.pack();
                centerOnScreen(frame);
                frame.setVisible(true);
            }
        };
        EventQueue.invokeLater(runnable);
    }

    public static void centerOnScreen(Component component) {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        int maxHeight = bounds.height;
        int maxWidth = bounds.width;

        Dimension frameSize = component.getSize();

        if (frameSize.height > maxHeight) {
            frameSize.height = maxHeight;
        }

        if (frameSize.width > maxWidth) {
            frameSize.width = maxWidth;
        }

        component.setLocation((maxWidth - frameSize.width) / 2,
                (maxHeight - frameSize.height) / 2);
        component.setSize(frameSize); // maximize to the available screen space!
    }

    /**
     * @return true if any app window has focus
     */
    public static boolean appIsActive() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window.isActive()) {
                return true;
            }
        }
        return false;
    }

    public static void showTextDialog(JComponent form, String title, final String text) {
        OKCancelDialog d = new OKCancelDialog(form, PixelitorWindow.getInstance(), title,
                "Copy as Text to the Clipboard", "Close") {
            @Override
            protected void dialogAccepted() {   // "Copy as Text to Clipboard"
                super.dialogAccepted();

                Utils.copyStringToClipboard(text);
            }

            @Override
            protected void dialogCanceled() {   // "Close"
                super.dialogCanceled();
                dispose();
            }
        };
        d.setVisible(true);
    }
}
