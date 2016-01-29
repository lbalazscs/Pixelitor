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

/**
 * Tracks the progress of some operation.
 */
public interface ProgressTracker {
    /**
     * One work unit - usually a line of pixels - was finished
     */
    void unitDone();

    /**
     * Multiple work units were finished
     */
    void addUnits(int units);

    /**
     * All the work is done
     */
    void finish();

    /**
     * A "null object" tracker that does nothing and
     * also can be shared because it has no state
     */
    ProgressTracker NULL_TRACKER = new ProgressTracker() {
        @Override
        public void addUnits(int units) {
        }

        @Override
        public void unitDone() {
        }

        @Override
        public void finish() {
        }
    };
}
