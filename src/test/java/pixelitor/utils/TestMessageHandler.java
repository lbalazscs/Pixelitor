/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

/**
 * A non-GUI message handler for tests
 */
public class TestMessageHandler implements MessageHandler {
    @Override
    public void showInStatusBar(String msg) {
    }

    @Override
    public ProgressHandler startProgress(String msg, int max) {
        return ProgressHandler.EMPTY;
    }

    @Override
    public void showInfo(String title, String msg, Component parent) {
    }

    @Override
    public void showError(String title, String msg, Component parent) {
        throw new AssertionError(title + ":" + msg);
    }

    @Override
    public void showNotImageLayerError(Layer layer) {
        throw new AssertionError("not image layer");
    }

    @Override
    public void showNotDrawableError(Layer layer) {
        throw new AssertionError("not image layer or mask");
    }

    @Override
    public void showException(Throwable e) {
        throw new AssertionError(e);
    }

    @Override
    public void showException(Throwable e, Thread srcThread) {
        throw new AssertionError("exception in " + srcThread.getName() + ", e = " + e);
    }

    @Override
    public void showExceptionOnEDT(Throwable e) {
        // still on this thread
        throw new AssertionError(e);
    }
}
