/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.test.RandomGUITest;

import java.awt.Component;
import java.io.File;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Centralized message handling for the display of status messages,
 * dialogs, and progress bars through a pluggable message handler.
 */
public class Messages {
    private static MessageHandler msgHandler;

    private Messages() {
        // only static methods, should not be instantiated
    }

    // Must be set before any other methods are used.
    public static void setHandler(MessageHandler msgHandler) {
        Messages.msgHandler = Objects.requireNonNull(msgHandler);
    }

    public static void showInfo(String title, String msg) {
        msgHandler.showInfo(title, msg, null);
    }

    public static void showInfo(String title, String msg, Component parent) {
        msgHandler.showInfo(title, msg, parent);
    }

    public static void showWarning(String title, String msg) {
        msgHandler.showWarning(title, msg, null);
    }

    public static void showWarning(String title, String msg, Component parent) {
        msgHandler.showWarning(title, msg, parent);
    }

    public static void showError(String title, String msg) {
        msgHandler.showError(title, msg, null);
    }

    public static void showError(String title, String msg, Component parent) {
        msgHandler.showError(title, msg, parent);
    }

    @SuppressWarnings("SameReturnValue")
    public static <T> T showExceptionOnEDT(Throwable exception) {
        msgHandler.showExceptionOnEDT(exception);
        // Returns a null of the desired type...
        // This way it fits into CompletableFuture.exceptionally.
        return null;
    }

    public static void showException(Throwable exception) {
        msgHandler.showException(exception);
    }

    public static void showException(Throwable exception, Thread srcThread) {
        msgHandler.showException(exception, srcThread);
    }

    public static boolean showYesNoQuestion(String title, String msg) {
        return msgHandler.showYesNoQuestion(title, msg);
    }

    /**
     * Shows an HTML text message in the status bar.
     */
    public static void showStatusMessage(String msg) {
        assert !msg.startsWith("<html>");
        msgHandler.showInStatusBar("<html>" + msg);
    }

    /**
     * Shows a non-HTML text message in the status bar.
     */
    public static void showPlainStatusMessage(String msg) {
        assert !msg.startsWith("<html>");
        msgHandler.showInStatusBar(msg);
    }

    public static void clearStatusBar() {
        showPlainStatusMessage("");
    }

    public static ProgressHandler startProgress(String msg, int max) {
        return msgHandler.startProgress(msg, max);
    }

    public static void showNotImageLayerError(Layer layer) {
        if (!RandomGUITest.isRunning()) {
            String msg = format("The active layer \"%s\" isn't an image layer.",
                layer.getName());
            showError("Not an Image Layer", msg);
        }
    }

    public static void showNotDrawableError(Layer layer) {
        String msg = format("The active layer \"%s\" isn't an image layer or mask.",
            layer.getName());
        showError("Not an image layer or mask", msg);
    }

    // Shows a performance timing message in the status bar.
    public static void showPerformanceMessage(String filterName, long timeMillis) {
        String msg;
        if (timeMillis < 1000) {
            msg = filterName + " took " + timeMillis + " ms";
        } else {
            float seconds = timeMillis / 1000.0f;
            msg = format("%s took %.1f s", filterName, seconds);
        }
        showPlainStatusMessage(msg);
    }

    public static boolean showReloadFileQuestion(File file) {
        String title = "Reload " + file.getName() + "?";
        String msg = "<html>The file <b>" + file.getAbsolutePath()
            + "</b><br> has been modified by another program." +
            "<br><br>Do you want to reload it?";
        return showYesNoQuestion(title, msg);
    }

    public static void showNoVisibleLayersError(Composition comp) {
        showError("No Visible Layers",
            "There are no visible layers in " + comp.getName());
    }

    public static void showUnrasterizableLayerGroupError(LayerGroup group, String name) {
        String msg = "<html>The active layer <i>\"%s\"</i> is a layer group." +
            "<br><br>%s can't be used on layer groups." +
            "<br>Pass through groups can't even be rasterized.";
        showError("Layer Group", msg.formatted(group.getName(), name));
    }

    public static void showSmartObjectUnsupportedWarning(String what) {
        msgHandler.showInfo("Feature Not Supported",
            what + " isn't yet supported if one of the layers is a smart object.", null);
    }

    public static void showFileOpenedMessage(Composition comp) {
        showStatusMessage("<b>" + comp.getName() + "</b> ("
            + comp.getCanvas().getSizeString() + ") was opened.");
    }

    public static void showFileSavedMessage(File file) {
        showStatusMessage("<b>" + file.getAbsolutePath() + "</b> was saved.");
    }

    public static void showBulkSaveMessage(int numFiles, File dir) {
        assert dir.isDirectory();
        showStatusMessage(numFiles + " files saved to <b>" + dir.getAbsolutePath() + "</b>");
    }
}
