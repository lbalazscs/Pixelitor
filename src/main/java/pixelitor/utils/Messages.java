/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerGroup;

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

    public static void showNotImplementedForSmartObjects(String what) {
        msgHandler.showInfo("Not Supported Yet",
            what + " is not implemented yet if one of the layers is a smart object.", null);
    }

    public static void showInfo(String title, String message, Component parent) {
        msgHandler.showInfo(title, message, parent);
    }

    public static void showWarning(String title, String message) {
        msgHandler.showWarning(title, message, null);
    }

    public static void showWarning(String title, String message, Component parent) {
        msgHandler.showWarning(title, message, parent);
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

    public static void showFileOpenedMessage(Composition comp) {
        showInStatusBar("<b>" + comp.getName() + "</b> ("
            + comp.getCanvas().getSizeString() + ") was opened.");
    }

    public static void showFilesSavedMessage(int numFiles, File dir) {
        assert dir.isDirectory();
        showInStatusBar(numFiles + " files saved to <b>" + dir.getAbsolutePath() + "</b>");
    }

    /**
     * Shows an HTML text message in the status bar.
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

    public static boolean showYesNoQuestion(String title, String msg) {
        return msgHandler.showYesNoQuestion(title, msg);
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

    public static boolean reloadFileQuestion(File file) {
        String title = "Reload " + file.getName() + "?";
        String msg = "<html>The file <b>" + file.getAbsolutePath()
                     + "</b><br> has been modified by another program." +
                     "<br><br>Do you want to reload it?";
        return showYesNoQuestion(title, msg);
    }

    public static void showNoVisibleLayersError(Composition comp) {
        showError("No visible layers",
            "There are no visible layers in " + comp.getName());
    }

    public static void showUnrasterizableLayerGroupError(LayerGroup group, String name) {
        String msg = "<html>The active layer <i>\"%s\"</i> is a layer group." +
                "<br><br>%s can't be used on layer groups." +
                "<br>Pass through groups can't even be rasterized.";
        showError("Layer Group", msg.formatted(group.getName(), name));
    }
}
