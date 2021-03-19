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
 * Abstracts away messages sent through the GUI
 * in order to enable GUI-independent testing.
 */
public interface MessageHandler {
    // *** status bar messages ***

    void showInStatusBar(String msg);

    ProgressHandler startProgress(String msg, int max);

    // *** dialog messages ***

    void showInfo(String title, String msg, Component parent);

    void showError(String title, String msg, Component parent);

    void showNotImageLayerError(Layer layer);

    void showNotDrawableError(Layer layer);

    void showException(Throwable e);

    void showException(Throwable e, Thread srcThread);

    void showExceptionOnEDT(Throwable e);
}
