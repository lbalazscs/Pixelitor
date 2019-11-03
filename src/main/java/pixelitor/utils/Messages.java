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

package pixelitor.utils;

import pixelitor.Build;
import pixelitor.gui.GUIMessageHandler;

import java.io.File;

import static java.lang.String.format;

/**
 * Static methods for status bar and dialog messages
 */
public class Messages {
    private static MessageHandler msgHandler;

    static {
        try {
            if (Build.isUnitTesting()) {
                msgHandler = new TestMessageHandler();
            } else {
                msgHandler = new GUIMessageHandler();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Messages() { // should not be instantiated
    }

    public static void showInfo(String title, String message) {
        msgHandler.showInfo(title, message);
    }

    public static void showError(String title, String message) {
        msgHandler.showError(title, message);
    }

    @SuppressWarnings("SameReturnValue")
    public static <T> T showExceptionOnEDT(Throwable e) {
        msgHandler.showExceptionOnEDT(e);
        // returns a null which can be a null of the desired type...:
        // this way it fits into CompletableFuture.exceptionally
        return null;
    }

    public static void showException(Throwable e) {
        msgHandler.showException(e);
    }

    public static void showException(Throwable e, Thread t) {
        msgHandler.showException(e, t);
    }

    public static void showFileSavedMessage(File file) {
        String msg = "<html><b>" + file.getAbsolutePath() + "</b> was saved.";
        showInStatusBar(msg);
    }

    public static void showFilesSavedMessage(int numFiles, File dir) {
        assert dir.isDirectory();
        String msg = "<html>" + numFiles + " files saved to <b>" + dir.getAbsolutePath() + "</b>";
        showInStatusBar(msg);
    }

    public static void showInStatusBar(String msg) {
        msgHandler.showInStatusBar(msg);
    }

    public static ProgressHandler startProgress(String msg, int max) {
        return msgHandler.startProgress(msg, max);
    }

    public static void showNotImageLayerError() {
        msgHandler.showNotImageLayerError();
    }

    public static void showNotDrawableError() {
        msgHandler.showNotDrawableError();
    }

    public static MessageHandler getMessageHandler() {
        return msgHandler;
    }

    public static void showPerformanceMessage(String filterName, long totalTime) {
        String msg;
        if (totalTime < 1000) {
            msg = filterName + " took " + totalTime + " ms";
        } else {
            float seconds = totalTime / 1000.0f;
            msg = format("%s took %.1f s", filterName, seconds);
        }
        showInStatusBar(msg);
    }
}
