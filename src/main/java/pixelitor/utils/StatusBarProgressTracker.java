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
 * Tracks the progress of some operation and shows a
 * status bar update if it takes a long time.
 */
public class StatusBarProgressTracker extends ThresholdProgressTracker {
    private ProgressHandler progressHandler;

    public StatusBarProgressTracker(String opName, int numTotalUnits) {
        super(opName, numTotalUnits);
        assert opName != null;
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
