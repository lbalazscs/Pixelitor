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
 * A progress tracker that tracks the progress
 * of a subtask within a larger task.
 */
public class SubtaskProgressTracker implements ProgressTracker {
    private final ProgressTracker superTask;

    // Determines how many units in this task correspond
    // to a unit in the larger task.
    private final double ratio;

    // the progress in the super task
    private double totalProgress;

    public SubtaskProgressTracker(double ratio, ProgressTracker superTask) {
        this.ratio = ratio;
        this.superTask = superTask;
        totalProgress = 0.0;
    }

    @Override
    public void unitDone() {
        totalProgress += ratio;
        update();
    }

    @Override
    public void unitsDone(int units) {
        totalProgress += (units * ratio);
        update();
    }

    private void update() {
        if (totalProgress < 1.0) {
            return; // nothing to do as we don't have yet a super work unit
        }
        if (totalProgress < 2.0) {
            totalProgress -= 1.0;
            superTask.unitDone();
            return;
        }

        // as these are positive numbers,
        // there is no need for Math.floor()
        int doneUnits = (int) totalProgress;

        totalProgress -= doneUnits;
        superTask.unitsDone(doneUnits);
    }

    @Override
    public void finished() {
        // some fractional progress might be lost,
        // no big deal
    }
}
