/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import pixelitor.Build;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.History;
import pixelitor.utils.test.Events;
import pixelitor.utils.test.RandomGUITest;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

/**
 * Static utility methods related to dialogs
 */
public class Dialogs {
    private static boolean mainWindowInitialized = false;

    private Dialogs() { // should not be instantiated
    }

    public static void setMainWindowInitialized(boolean mainWindowInitialized) {
        Dialogs.mainWindowInitialized = mainWindowInitialized;
    }

    public static Frame getParentForDialogs() {
        if (mainWindowInitialized) {
            return PixelitorWindow.getInstance();
        }
        return null;
    }

    public static void showInfoDialog(String title, String msg) {
        showInfoDialog(getParentForDialogs(), title, msg);
    }

    public static void showInfoDialog(Component parent, String title, String msg) {
        GlobalKeyboardWatch.setDialogActive(true);
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
        GlobalKeyboardWatch.setDialogActive(false);
    }

    public static boolean showYesNoQuestionDialog(String title, String msg) {
        return showYesNoQuestionDialog(getParentForDialogs(), title, msg);
    }

    public static boolean showYesNoQuestionDialog(Component parent, String title, String msg) {
        return showYesNoDialog(parent, title, msg, JOptionPane.QUESTION_MESSAGE);
    }

    public static boolean showYesNoWarningDialog(String title, String msg) {
        return showYesNoWarningDialog(getParentForDialogs(), title, msg);
    }

    public static boolean showYesNoWarningDialog(Component parent, String title, String msg) {
        return showYesNoDialog(parent, title, msg, JOptionPane.WARNING_MESSAGE);
    }

    public static boolean showYesNoDialog(Component parent, String title, String msg, int messageType) {
        GlobalKeyboardWatch.setDialogActive(true);
        int reply = JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.YES_NO_OPTION, messageType);
        GlobalKeyboardWatch.setDialogActive(false);
        return (reply == JOptionPane.YES_OPTION);
    }

    public static boolean showOKCancelWarningDialog(String msg, String title, Object[] options, int initialOptionIndex) {
        return showOKCancelDialog(msg, title, options, initialOptionIndex, JOptionPane.WARNING_MESSAGE);
    }

    public static boolean showOKCancelDialog(String msg, String title, Object[] options, int initialOptionIndex, int messageType) {
        GlobalKeyboardWatch.setDialogActive(true);
        int userAnswer = JOptionPane.showOptionDialog(getParentForDialogs(), msg, title,
                JOptionPane.OK_CANCEL_OPTION, messageType, null,
                options, options[initialOptionIndex]);
        GlobalKeyboardWatch.setDialogActive(false);
        return userAnswer == JOptionPane.OK_OPTION;
    }

    public static void showErrorDialog(String title, String msg) {
        showErrorDialog(getParentForDialogs(), title, msg);
    }

    public static void showErrorDialog(Component parent, String title, String msg) {
        GlobalKeyboardWatch.setDialogActive(true);
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
        GlobalKeyboardWatch.setDialogActive(false);
    }

    public static void showWarningDialog(String title, String msg) {
        showWarningDialog(getParentForDialogs(), title, msg);
    }

    public static void showNotAColorOnClipboardDialog(Window parent) {
        showWarningDialog(parent, "Not a Color",
                "The clipboard contents could not be interpreted as a color");
    }

    public static void showWarningDialog(Component parent, String title, String msg) {
        GlobalKeyboardWatch.setDialogActive(true);
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.WARNING_MESSAGE);
        GlobalKeyboardWatch.setDialogActive(false);
    }

    public static void showNotImageLayerDialog() {
        if (!RandomGUITest.isRunning()) {
            showErrorDialog("Not an image layer", "The active layer is not an image layer.");
        }
    }

    public static void showNotImageLayerOrMaskDialog() {
        if (!RandomGUITest.isRunning()) {
            showErrorDialog("Not an image layer or mask", "The active layer is not an image layer or mask.");
        }
    }

    public static void showExceptionDialog(Throwable e) {
        Thread currentThread = Thread.currentThread();
        showExceptionDialog(e, currentThread);
    }

    public static void showExceptionDialog(Throwable e, Thread thread) {
        String threadName = thread.getName();
        System.err.printf("Exception in the thread '%s'%n", threadName);
        e.printStackTrace();

        if(e instanceof InvocationTargetException) {
            e = e.getCause();
        }

        if (RandomGUITest.isRunning()) {
            Events.dumpAll();
            History.showHistory();
            Toolkit.getDefaultToolkit().beep();
            playWarningSound();

            RandomGUITest.stop();
        } else if(Build.CURRENT.isDevelopment()) {
            Events.dumpActive();
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

    public static void showOutOfMemoryDialog(OutOfMemoryError e) {
        if (Build.CURRENT.isDevelopment()) {
            e.printStackTrace();
        }
        String message = "<html><b>Out of memory error.</b> You can try <ul>" +
                "<li>decreasing the undo levels" +
                "<li>decreasing the number of layers" +
                "<li>working with smaller images";
        String title = "Out of memory error.";
        Dialogs.showErrorDialog(title, message);
    }
}
