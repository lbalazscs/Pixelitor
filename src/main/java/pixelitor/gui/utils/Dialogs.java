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

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import pixelitor.Build;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.utils.Utils;
import pixelitor.utils.test.Events;
import pixelitor.utils.test.RandomGUITest;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;

import static java.lang.String.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;

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

    private static Frame getParent() {
        if (mainWindowInitialized) {
            return PixelitorWindow.getInstance();
        }
        return null;
    }

    public static void showInfoDialog(String title, String msg) {
        showInfoDialog(getParent(), title, msg);
    }

    public static void showInfoDialog(Component parent, String title, String msg) {
        GlobalKeyboardWatch.setDialogActive(true);
        JOptionPane.showMessageDialog(parent, msg, title, INFORMATION_MESSAGE);
        GlobalKeyboardWatch.setDialogActive(false);
    }

    public static boolean showYesNoQuestionDialog(String title, String msg) {
        return showYesNoQuestionDialog(getParent(), title, msg);
    }

    public static boolean showYesNoQuestionDialog(Component parent, String title,
                                                  String msg) {
        return showYesNoDialog(parent, title, msg, QUESTION_MESSAGE);
    }

    public static boolean showYesNoWarningDialog(String title, String msg) {
        return showYesNoWarningDialog(getParent(), title, msg);
    }

    public static boolean showYesNoWarningDialog(Component parent, String title,
                                                 String msg) {
        return showYesNoDialog(parent, title, msg, WARNING_MESSAGE);
    }

    public static boolean showYesNoDialog(Component parent, String title,
                                          String msg, int messageType) {
        GlobalKeyboardWatch.setDialogActive(true);
        int reply = JOptionPane.showConfirmDialog(parent, msg, title,
                YES_NO_OPTION, messageType);
        GlobalKeyboardWatch.setDialogActive(false);

        return (reply == YES_OPTION);
    }

    public static boolean showOKCancelWarningDialog(String msg, String title,
                                                    Object[] options,
                                                    int initialOptionIndex) {
        return showOKCancelDialog(msg, title, options, initialOptionIndex, WARNING_MESSAGE);
    }

    public static boolean showOKCancelDialog(String msg, String title,
                                             Object[] options,
                                             int initialOptionIndex,
                                             int messageType) {
        GlobalKeyboardWatch.setDialogActive(true);
        int userAnswer = JOptionPane.showOptionDialog(getParent(), msg, title,
                OK_CANCEL_OPTION, messageType, null,
                options, options[initialOptionIndex]);
        GlobalKeyboardWatch.setDialogActive(false);

        return userAnswer == OK_OPTION;
    }

    public static void showErrorDialog(String title, String msg) {
        showErrorDialog(getParent(), title, msg);
    }

    public static void showErrorDialog(Component parent, String title, String msg) {
        GlobalKeyboardWatch.setDialogActive(true);
        JOptionPane.showMessageDialog(parent, msg, title, ERROR_MESSAGE);
        GlobalKeyboardWatch.setDialogActive(false);
    }

    public static void showWarningDialog(String title, String msg) {
        showWarningDialog(getParent(), title, msg);
    }

    public static void showNotAColorOnClipboardDialog(Window parent) {
        showWarningDialog(parent, "Not a Color",
                "The clipboard contents could not be interpreted as a color");
    }

    public static void showWarningDialog(Component parent, String title, String msg) {
        GlobalKeyboardWatch.setDialogActive(true);
        JOptionPane.showMessageDialog(parent, msg, title, WARNING_MESSAGE);
        GlobalKeyboardWatch.setDialogActive(false);
    }

    public static void showNotImageLayerDialog() {
        if (!RandomGUITest.isRunning()) {
            showErrorDialog("Not an image layer",
                    "The active layer is not an image layer.");
        }
    }

    public static void showNotDrawableDialog() {
        if (!RandomGUITest.isRunning()) {
            showErrorDialog("Not an image layer or mask",
                    "The active layer is not an image layer or mask.");
        }
    }

    public static void showExceptionDialog(Throwable e) {
        Thread currentThread = Thread.currentThread();
        showExceptionDialog(e, currentThread);
    }

    public static void showExceptionDialog(Throwable e, Thread thread) {
        if (!EventQueue.isDispatchThread()) {
            System.err.printf("ERROR: Dialogs.showExceptionDialog called in %s%n",
                    Thread.currentThread().toString());

            // call this method on the EDT
            Throwable finalE = e;
            EventQueue.invokeLater(() -> showExceptionDialog(finalE, thread));
            return;
        }

        System.err.printf("\nDialogs.showExceptionDialog: Exception in the thread '%s'%n", thread.getName());
        e.printStackTrace();

        RandomGUITest.stop();

        if (e instanceof OutOfMemoryError) {
            showOutOfMemoryDialog((OutOfMemoryError) e);
            return;
        }

        showMoreDevelopmentInfo(e);

        if (e instanceof CompletionException) {
            e = e.getCause();
        }
        if (e instanceof UncheckedIOException) {
            e = e.getCause();
        }
        if (e instanceof InvocationTargetException) {
            e = e.getCause();
        }

        Frame parent = getParent();
        String basicErrorMessage = "An exception occurred: " + e.getMessage();
        ErrorInfo ii = new ErrorInfo("Program error",
                basicErrorMessage, null, null, e,
                Level.SEVERE, null);
        JXErrorPane.showDialog(parent, ii);
    }

    private static void showMoreDevelopmentInfo(Throwable e) {
        if (Build.CURRENT.isFinal()) {
            return;
        }

        Predicate<String> isRandomGUITest = s -> s.contains("RandomGUITest");
        Predicate<String> isAssertJSwingTest = s -> s.contains("AssertJSwingTest");
        boolean autoTest = Arrays.stream(e.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(isRandomGUITest.or(isAssertJSwingTest));

        if (!autoTest) {
            return;
        }

        // avoid the mixing of the stack trace with
        // the event dumps
        Utils.sleep(2, TimeUnit.SECONDS);

        Events.dumpForActiveComp();
        Toolkit.getDefaultToolkit().beep();
        playWarningSound();
    }

    private static void playWarningSound() {
        try {
            int maxVolume = 90;
            int sound = 65;
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel channel = synthesizer.getChannels()[9];  // drums channel.
            for (int i = 0; i < 10; i++) {
                Thread.sleep(100);
                channel.noteOn(sound + i, maxVolume);
                Thread.sleep(100);
                channel.noteOff(sound + i);
            }
        } catch (MidiUnavailableException | InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    public static void showOutOfMemoryDialog(OutOfMemoryError e) {
        if (Build.isDevelopment()) {
            e.printStackTrace();
        }
        String msg = "<html><b>Out of memory error.</b> You can try <ul>" +
                "<li>decreasing the undo levels" +
                "<li>decreasing the number of layers" +
                "<li>working with smaller images";
        String title = "Out of memory error.";
        Dialogs.showErrorDialog(title, msg);
    }

    public static int showCloseWarningDialog(String compName) {
        Object[] options = {"Save",
                "Don't Save",
                "Cancel"};
        String question = format(
                "<html><b>Do you want to save the changes made to %s?</b>" +
                        "<br>Your changes will be lost if you don't save them.</html>",
                compName);

        GlobalKeyboardWatch.setDialogActive(true);
        int answer = JOptionPane.showOptionDialog(
                PixelitorWindow.getInstance(), new JLabel(question),
                "Unsaved changes", YES_NO_CANCEL_OPTION,
                WARNING_MESSAGE, null, options, options[0]);
        GlobalKeyboardWatch.setDialogActive(false);
        return answer;
    }
}
