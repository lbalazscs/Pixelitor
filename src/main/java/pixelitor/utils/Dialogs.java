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

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.util.logging.Level;

/**
 * Static utility methods related to dialogs
 */
public class Dialogs {
    private static boolean mainWindowInitialized = false;

    private Dialogs() { // should not me instantiated
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
        return showYesNoQuestionDialog(getParentForDialogs(), title, msg);
    }

    public static boolean showYesNoQuestionDialog(Component parent, String title, String msg) {
        int reply = JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.YES_NO_OPTION);
        return (reply == JOptionPane.YES_OPTION);
    }

    public static boolean showYesNoWarningDialog(String title, String msg) {
        return showYesNoWarningDialog(getParentForDialogs(), title, msg);
    }

    public static boolean showYesNoWarningDialog(Component parent, String title, String msg) {
        int reply = JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return (reply == JOptionPane.YES_OPTION);
    }

    public static void showErrorDialog(String title, String msg) {
        showErrorDialog(getParentForDialogs(), title, msg);
    }

    public static void showErrorDialog(Component parent, String title, String msg) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
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

            playWarningSound();
        }

        Frame parent = getParentForDialogs();
        String basicErrorMessage = "An exception occurred: " + e.getMessage();
        ErrorInfo ii = new ErrorInfo("Program error", basicErrorMessage, null, null, e, Level.SEVERE, null);
        JXErrorPane.showDialog(parent, ii);
    }

    private static void playWarningSound() {
//        if (2 > 1) {
//            return;
//        }

        try {
//            int velocity = 127;    // max volume
            int velocity = 90;    // max volume
            int sound = 65;
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel channel = synthesizer.getChannels()[9];  // drums channel.
            for (int i = 0; i < 10; i++) {
                Thread.sleep(100);
                channel.noteOn(sound + i, velocity);
                Thread.sleep(100);
                channel.noteOff(sound + i);
            }
        } catch (MidiUnavailableException | InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    public static void showOutOfMemoryDialog() {
        String message = "<html><b>Out of memory error.</b> You can try <ul>" +
                "<li>decreasing the undo levels" +
                "<li>decreasing the number of layers" +
                "<li>working with smaller images";
        String title = "Out of memory error.";
        Dialogs.showErrorDialog(title, message);
    }
}
