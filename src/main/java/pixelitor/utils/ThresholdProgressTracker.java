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
 * Base class for progress trackers that only show visual feedback
 * after a time threshold is exceeded. This prevents UI updates
 * for operations that complete quickly.
 */
public abstract class ThresholdProgressTracker implements ProgressTracker {
    private static final int VISIBILITY_THRESHOLD_MS = 200;

    private final long startTimeMillis;
    private final int numTotalUnits;

    private int completedUnits = 0;
    private int lastReportedPercent = 0;

    private boolean isTrackingVisible = false;
    private final boolean isRunningOnEDT;

    // In this class this field is used only for debugging.
    // The status bar progress tracker subclass uses it to label the progress bar.
    protected final String opName;

    protected ThresholdProgressTracker(String opName, int numTotalUnits) {
        assert numTotalUnits > 0;

        this.numTotalUnits = numTotalUnits;
        this.opName = opName;
        startTimeMillis = System.currentTimeMillis();
        isRunningOnEDT = Threads.calledOnEDT();
    }

    @Override
    public void unitDone() {
        completedUnits++;
        update();
    }

    @Override
    public void unitsDone(int units) {
        assert units > 0;
        this.completedUnits += units;
        update();
    }

    private void update() {
        // Check if we should start showing progress
        if (!isTrackingVisible) {
            double elapsedTime = System.currentTimeMillis() - startTimeMillis;
            if (elapsedTime > VISIBILITY_THRESHOLD_MS) {
                if (isRunningOnEDT) {
                    onProgressStart();
                } else {
                    EventQueue.invokeLater(this::onProgressStart);
                }
                isTrackingVisible = true;
            }
        }

        // Update progress if visible
        if (isTrackingVisible) {
            int currentPercent = (int) (completedUnits * 100.0 / numTotalUnits);
            if (currentPercent > lastReportedPercent) {
                if (isRunningOnEDT) {
                    onProgressUpdate(currentPercent);
                } else {
                    EventQueue.invokeLater(() -> onProgressUpdate(currentPercent));
                }
                lastReportedPercent = currentPercent;
            }
        }
    }

    @Override
    public void finished() {
        if (isTrackingVisible) {
            if (isRunningOnEDT) {
                onProgressComplete();
            } else {
                EventQueue.invokeLater(this::onProgressComplete);
            }
            isTrackingVisible = false;
            lastReportedPercent = 0;
        }
    }

    /**
     * Called when progress tracking should become visible.
     * This is called on the EDT after the time threshold is exceeded.
     */
    protected abstract void onProgressStart();

    /**
     * Called when progress percentage has increased.
     * This is called on the EDT with the new percentage value.
     */
    protected abstract void onProgressUpdate(int percentComplete);

    /**
     * Called when progress tracking should be hidden.
     * This is called on the EDT when the operation completes.
     */
    protected abstract void onProgressComplete();
}
