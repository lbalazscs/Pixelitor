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

package pixelitor.utils;

import pixelitor.layers.Layer;

import java.awt.Component;
import java.io.File;

import static java.lang.String.format;

/**
 * Static methods for status bar and dialog messages
 */
public class Messages {
    private static MessageHandler msgHandler;

    private Messages() { // should not be instantiated
    }

    public static void setMsgHandler(MessageHandler msgHandler) {
        Messages.msgHandler = msgHandler;
    }

    public static void showInfo(String title, String message) {
        msgHandler.showInfo(title, message, null);
    }

    public static void showInfo(String title, String message, Component parent) {
        msgHandler.showInfo(title, message, parent);
    }

    public static void showError(String title, String message) {
        msgHandler.showError(title, message, null);
    }

    public static void showError(String title, String message, Component parent) {
        msgHandler.showError(title, message, parent);
    }

    @SuppressWarnings("SameReturnValue")
    public static <T> T showExceptionOnEDT(Throwable e) {
        msgHandler.showExceptionOnEDT(e);
        // returns a null of the desired type...:
        // this way it fits into CompletableFuture.exceptionally
        return null;
    }

    public static void showException(Throwable e) {
        msgHandler.showException(e);
    }

    public static void showException(Throwable e, Thread srcThread) {
        msgHandler.showException(e, srcThread);
    }

    public static void showFileSavedMessage(File file) {
        showInStatusBar("<b>" + file.getAbsolutePath() + "</b> was saved.");
    }

    public static void showFileOpenedMessage(File file) {
        showInStatusBar("<b>" + file.getName() + "</b> was opened.");
    }

    public static void showFilesSavedMessage(int numFiles, File dir) {
        assert dir.isDirectory();
        showInStatusBar(numFiles + " files saved to <b>" + dir.getAbsolutePath() + "</b>");
    }

    /**
     * Shows a HTML text message in the status bar.
     */
    public static void showInStatusBar(String msg) {
        assert !msg.startsWith("<html>");
        msgHandler.showInStatusBar("<html>" + msg);
    }

    /**
     * Shows a non-HTML text message in the status bar.
     */
    public static void showPlainInStatusBar(String msg) {
        assert !msg.startsWith("<html>");
        msgHandler.showInStatusBar(msg);
    }

    public static ProgressHandler startProgress(String msg, int max) {
        return msgHandler.startProgress(msg, max);
    }

    public static void showNotImageLayerError(Layer layer) {
        msgHandler.showNotImageLayerError(layer);
    }

    public static void showNotDrawableError(Layer layer) {
        msgHandler.showNotDrawableError(layer);
    }

    public static void showPerformanceMessage(String filterName, long totalTime) {
        String msg;
        if (totalTime < 1000) {
            msg = filterName + " took " + totalTime + " ms";
        } else {
            float seconds = totalTime / 1000.0f;
            msg = format("%s took %.1f s", filterName, seconds);
        }
        showPlainInStatusBar(msg);
    }
}
