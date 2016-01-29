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
 * A progress tracker which tracks the progress
 * of a subtask within a larger task
 */
public class SubtaskProgressTracker implements ProgressTracker {
    private final ProgressTracker superTask;

    /**
     * Determines how many units in this task correspond
     * to a unit in the larger task
     */
    private final double ratio;

    private double progress;

    public SubtaskProgressTracker(double ratio, ProgressTracker superTask) {
        this.ratio = ratio;
        this.superTask = superTask;
        progress = 0.0;
    }

    @Override
    public void unitDone() {
        progress += ratio;
        update();
    }

    @Override
    public void addUnits(int units) {
        progress += (units * ratio);
        update();
    }

    private void update() {
        if (progress < 1.0) {
            return; // nothing to do
        }
        if (progress < 2.0) {
            // we are between 1.0 and 2.0, so
            // we can subtract one
            progress -= 1.0;
            superTask.unitDone();
            return;
        }
        // unlikely case: we have more than 2.0 progress

        // as long as these are positive numbers,
        // there is no need for Math.floor()
        int doneUnits = (int) progress;

        progress -= doneUnits;
        superTask.addUnits(doneUnits);
    }

    @Override
    public void finish() {
        // some fractional progress might be lost,
        // no big deal
    }
}
