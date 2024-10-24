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

import java.awt.EventQueue;

/**
 * Manages the lifecycle and updates of a UI progress bar.
 * This is a lower-level interface than {@link ProgressTracker}.
 */
public interface ProgressHandler {
    /**
     * Updates the current progress value in the UI.
     */
    void updateProgress(int currentValue);

    /**
     * Terminates the progress bar.
     * Should be called when the operation is complete or cancelled.
     */
    void stopProgress();

    /**
     * Safely stops the progress bar on the EDT.
     */
    default void stopProgressOnEDT() {
        if (Threads.calledOnEDT()) {
            stopProgress();
        } else {
            EventQueue.invokeLater(this::stopProgress);
        }
    }
}
