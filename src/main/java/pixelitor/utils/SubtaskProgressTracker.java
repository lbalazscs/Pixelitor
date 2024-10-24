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

/**
 * A progress tracker that tracks the progress
 * of a subtask within a larger task.
 */
public class SubtaskProgressTracker implements ProgressTracker {
    private final ProgressTracker parentTracker;

    // how many subtask units equal one parent task unit
    private final double conversionRatio;

    // the unreported progress in the parent task
    private double accumulatedParentProgress;

    public SubtaskProgressTracker(double conversionRatio, ProgressTracker parentTracker) {
        assert conversionRatio > 0 && conversionRatio <= 1 : "conversionRatio = " + conversionRatio;

        this.conversionRatio = conversionRatio;
        this.parentTracker = parentTracker;
        accumulatedParentProgress = 0.0;
    }

    @Override
    public void unitDone() {
        accumulatedParentProgress += conversionRatio;
        notifyParent();
    }

    @Override
    public void unitsDone(int completedUnits) {
        assert completedUnits > 0;
        accumulatedParentProgress += (completedUnits * conversionRatio);
        notifyParent();
    }

    private void notifyParent() {
        if (accumulatedParentProgress < 1.0) {
            return; // nothing to do as we don't have accumulated yet a parent work unit
        }
        if (accumulatedParentProgress < 2.0) {
            accumulatedParentProgress -= 1.0;
            parentTracker.unitDone();
            return;
        }

        // as these are positive numbers,
        // there is no need for Math.floor()
        int parentUnits = (int) accumulatedParentProgress;

        accumulatedParentProgress -= parentUnits;
        parentTracker.unitsDone(parentUnits);
    }

    @Override
    public void finished() {
        // Handle any remaining fractional progress
        if (accumulatedParentProgress >= 0.5) {
            // Round up to one final parent unit if we're at least halfway
            parentTracker.unitDone();
        }

        // Doesn't finish the parent as it may have other subtasks
    }
}
