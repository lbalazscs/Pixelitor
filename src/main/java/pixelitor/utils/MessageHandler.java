/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Component;

/**
 * Abstracts away messages sent through the GUI
 * in order to enable GUI-independent testing.
 */
public interface MessageHandler {
    // *** status bar messages ***

    void showInStatusBar(String msg);

    ProgressHandler startProgress(String msg, int maxValue);

    // *** dialog messages ***

    void showInfo(String title, String msg, Component parent);

    void showWarning(String title, String msg, Component parent);

    void showError(String title, String msg, Component parent);

    boolean showYesNoQuestion(String title, String msg);

    void showException(Throwable exception);

    // Shows an exception dialog with information about the source thread.
    void showException(Throwable exception, Thread srcThread);

    void showExceptionOnEDT(Throwable exception);
}
