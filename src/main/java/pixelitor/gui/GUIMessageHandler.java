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

package pixelitor.gui;

import pixelitor.MessageHandler;
import pixelitor.gui.utils.Dialogs;

import javax.swing.*;

/**
 * The MessageHandler that is normally used in non-testing code
 */
public class GUIMessageHandler implements MessageHandler {
    public GUIMessageHandler() {
    }

    @Override
    public void showInStatusBar(String msg) {
        StatusBar.INSTANCE.setMessage(msg);
    }

    @Override
    public void startProgress(String msg, int max) {
        assert SwingUtilities.isEventDispatchThread() : "not EDT thread";

        StatusBar.INSTANCE.startProgress(msg, max);
    }

    @Override
    public void updateProgress(int value) {
        StatusBar.INSTANCE.updateProgress(value);
    }

    @Override
    public void stopProgress() {
        StatusBar.INSTANCE.stopProgress();
    }

    @Override
    public void showInfo(String title, String msg) {
        Dialogs.showInfoDialog(title, msg);
    }

    @Override
    public void showError(String title, String msg) {
        Dialogs.showErrorDialog(title, msg);
    }

    @Override
    public void showNotImageLayerError() {
        Dialogs.showNotImageLayerDialog();
    }

    @Override
    public void showNotImageLayerOrMaskError() {
        Dialogs.showNotImageLayerOrMaskDialog();
    }

    @Override
    public void showException(Throwable e) {
        Dialogs.showExceptionDialog(e);
    }

    @Override
    public void showException(Throwable e, Thread t) {
        Dialogs.showExceptionDialog(e, t);
    }
}
