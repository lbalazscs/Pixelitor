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

import pixelitor.filters.AddNoise;

/**
 * Tracks the progress of a long-running operation by counting
 * completed work units. A work unit represents an atomic piece
 * of work, such as processing one line of pixels.
 */
public interface ProgressTracker {
    /**
     * Signals that a single work unit has been completed.
     */
    void unitDone();

    /**
     * Signals that multiple work units have been completed.
     */
    void unitsDone(int completedUnits);

    /**
     * Signals that all work has been completed.
     */
    void finished();

    static ProgressTracker createSafeTracker(int numWorkUnits) {
        if (numWorkUnits > 0) {
            return new StatusBarProgressTracker(AddNoise.NAME, numWorkUnits);
        }
        return NULL_TRACKER;
    }

    /**
     * A no-op implementation that discards all progress updates.
     */
    ProgressTracker NULL_TRACKER = new ProgressTracker() {
        @Override
        public void unitsDone(int completedUnits) {
        }

        @Override
        public void unitDone() {
        }

        @Override
        public void finished() {
        }
    };
}
