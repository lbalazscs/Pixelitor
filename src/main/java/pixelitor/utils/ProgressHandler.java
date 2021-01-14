/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
 * An object that can be used to update and stop
 * the displayed progress value in a UI progress indicator.
 */
public interface ProgressHandler {
    void updateProgress(int value);

    void stopProgress();

    default void stopProgressOnEDT() {
        if (Threads.calledOnEDT()) {
            stopProgress();
        } else {
            EventQueue.invokeLater(this::stopProgress);
        }
    }

    ProgressHandler EMPTY = new ProgressHandler() {
        @Override
        public void updateProgress(int value) {
            // do nothing
        }

        @Override
        public void stopProgress() {
            // do nothing
        }
    };
}
