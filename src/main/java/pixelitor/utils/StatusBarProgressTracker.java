/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
 * Tracks the progress of an operation and shows progress
 * in the status bar if the operation exceeds the threshold.
 */
public class StatusBarProgressTracker extends ThresholdProgressTracker {
    private ProgressHandler progressHandler;

    public StatusBarProgressTracker(String opName, int numTotalUnits) {
        super(opName, numTotalUnits);
        assert opName != null;
    }

    public static ProgressTracker create(String name, int numWorkUnits) {
        if (numWorkUnits > 0) {
            return new StatusBarProgressTracker(name, numWorkUnits);
        }
        return NO_OP_TRACKER;
    }

    @Override
    protected void onProgressStart() {
        progressHandler = Messages.startProgress(opName, 100);
    }

    @Override
    protected void onProgressUpdate(int percentComplete) {
        progressHandler.updateProgress(percentComplete);
    }

    @Override
    protected void onProgressComplete() {
        progressHandler.stopProgress();
        progressHandler = null;
    }
}
