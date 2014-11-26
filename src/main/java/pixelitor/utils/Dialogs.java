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

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import pixelitor.Build;
import pixelitor.PixelitorWindow;
import pixelitor.history.History;
import pixelitor.utils.test.DebugEventQueue;

import javax.swing.*;
import java.awt.Frame;
import java.awt.Toolkit;
import java.util.logging.Level;

public class Dialogs {
    private static boolean mainWindowInitialized = false;

    private Dialogs() { // only static utility methods
    }

    public static void setMainWindowInitialized(boolean mainWindowInitialized) {
        Dialogs.mainWindowInitialized = mainWindowInitialized;
    }

    public static boolean isMainWindowInitialized() {
        return mainWindowInitialized;
    }

    public static Frame getParentForDialogs() {
        if (mainWindowInitialized) {
            return PixelitorWindow.getInstance();
        }
        return null;
    }


    public static void showInfoDialog(String title, String msg) {
        JOptionPane.showMessageDialog(getParentForDialogs(), msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean showYesNoQuestionDialog(String title, String msg) {
        int reply = JOptionPane.showConfirmDialog(getParentForDialogs(), msg, title, JOptionPane.YES_NO_OPTION);
        return (reply == JOptionPane.YES_OPTION);
    }

    public static void showErrorDialog(String title, String msg) {
        JOptionPane.showMessageDialog(getParentForDialogs(), msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showNotImageLayerDialog() {
        if (!Build.CURRENT.isRobotTest()) {
            showErrorDialog("Error", "The active layer is not an image layer");
        }
    }

    public static void showExceptionDialog(Throwable e) {
        e.printStackTrace();

        if (Build.CURRENT.isRobotTest()) {
            DebugEventQueue.dump();
            History.showHistory();
            Toolkit.getDefaultToolkit().beep();

//            playWarningSound();
        }

        Frame parent = getParentForDialogs();
        String basicErrorMessage = "An exception occurred: " + e.getMessage();
        ErrorInfo ii = new ErrorInfo("Program error", basicErrorMessage, null, null, e, Level.SEVERE, null);
        JXErrorPane.showDialog(parent, ii);
    }
}
