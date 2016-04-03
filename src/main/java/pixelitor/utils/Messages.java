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

package pixelitor.utils;

import pixelitor.MessageHandler;

import java.io.File;

public class Messages {
    private static MessageHandler messageHandler;

    private Messages() { // should not be instantiated
    }

    public static void setMessageHandler(MessageHandler messageHandler) {
        Messages.messageHandler = messageHandler;
    }

    public static void showInfo(String title, String message) {
        messageHandler.showInfo(title, message);
    }

    public static void showError(String title, String message) {
        messageHandler.showError(title, message);
    }

    public static void showException(Throwable e) {
        messageHandler.showException(e);
    }

    public static void showException(Throwable e, Thread t) {
        messageHandler.showException(e, t);
    }

    public static void showFileSavedMessage(File file) {
        String msg = "File " + file.getAbsolutePath() + " saved.";
        showStatusMessage(msg);
    }

    public static void showStatusMessage(String msg) {
        messageHandler.showStatusMessage(msg);
    }

    public static void showNotImageLayerError() {
        messageHandler.showNotImageLayerError();
    }

    public static void showNotImageLayerOrMaskError() {
        messageHandler.showNotImageLayerOrMaskError();
    }

    public static MessageHandler getMessageHandler() {
        return messageHandler;
    }
}
