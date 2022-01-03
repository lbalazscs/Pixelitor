/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.Layer;
import pixelitor.utils.MessageHandler;
import pixelitor.utils.ProgressHandler;

import java.awt.Component;
import java.awt.EventQueue;

import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The MessageHandler that is normally used (except in unit-testing code)
 */
public class GUIMessageHandler implements MessageHandler {
    public GUIMessageHandler() {
    }

    @Override
    public void showInStatusBar(String msg) {
        assert calledOnEDT() : threadInfo();

        StatusBar.get().setMessage(msg);
    }

    @Override
    public ProgressHandler startProgress(String msg, int max) {
        assert calledOnEDT() : threadInfo();

        return StatusBar.get().startProgress(msg, max);
    }

    @Override
    public void showInfo(String title, String msg, Component parent) {
        Dialogs.showInfoDialog(parent, title, msg);
    }

    @Override
    public void showError(String title, String msg, Component parent) {
        Dialogs.showErrorDialog(parent, title, msg);
    }

    @Override
    public void showNotImageLayerError(Layer layer) {
        Dialogs.showNotImageLayerDialog(layer);
    }

    @Override
    public void showNotDrawableError(Layer layer) {
        Dialogs.showNotDrawableDialog(layer);
    }

    @Override
    public void showException(Throwable e) {
        Dialogs.showExceptionDialog(e);
    }

    @Override
    public void showExceptionOnEDT(Throwable e) {
        EventQueue.invokeLater(() -> showException(e));
    }

    @Override
    public void showException(Throwable e, Thread srcThread) {
        Dialogs.showExceptionDialog(e, srcThread);
    }

    @Override
    public boolean showYesNoQuestion(String title, String msg) {
        return Dialogs.showYesNoQuestionDialog(title, msg);
    }
}
