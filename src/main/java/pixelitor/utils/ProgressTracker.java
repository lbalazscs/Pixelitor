/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

/**
 * Tracks the progress of an operation.
 */
public interface ProgressTracker {
    /**
     * Signals the completion of a single work unit, often a line of pixels.
     */
    void unitDone();

    /**
     * Signals the completion of multiple work units.
     *
     * @param units The number of work units completed.
     */
    void unitsDone(int units);

    /**
     * Signals that all work has been completed.
     */
    void finished();

    /**
     * A "null object" tracker that does nothing and
     * can be shared because it has no state
     */
    ProgressTracker NULL_TRACKER = new ProgressTracker() {
        @Override
        public void unitsDone(int units) {
        }

        @Override
        public void unitDone() {
        }

        @Override
        public void finished() {
        }
    };
}
