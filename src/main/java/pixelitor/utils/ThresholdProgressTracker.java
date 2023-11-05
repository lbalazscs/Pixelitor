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

import java.awt.EventQueue;

/**
 * An abstract superclass for progress tracking classes that
 * show progress information after a time threshold has been exceeded.
 */
public abstract class ThresholdProgressTracker implements ProgressTracker {
    private static final int THRESHOLD_MILLIS = 200;

    private final long startTime;
    private final int numComputationUnits;

    private int finished = 0;
    private int lastPercent = 0;

    private boolean showingProgress = false;
    private final boolean runningOnEDT;

    // In this class this field is used only for debugging.
    // The status bar progress tracker subclass uses it to label the progress bar.
    protected final String name;

    protected ThresholdProgressTracker(int numComputationUnits, String name) {
        this.numComputationUnits = numComputationUnits;
        this.name = name;
        startTime = System.currentTimeMillis();
        runningOnEDT = Threads.calledOnEDT();
    }

    @Override
    public void unitDone() {
        finished++;
        update();
    }

    @Override
    public void unitsDone(int units) {
        finished += units;
        update();
    }

    private void update() {
        if (!showingProgress) {
            double millis = System.currentTimeMillis() - startTime;
            if (millis > THRESHOLD_MILLIS) {
                if (runningOnEDT) {
                    startProgressTracking();
                } else {
                    EventQueue.invokeLater(this::startProgressTracking);
                }
                showingProgress = true;
            }
        }

        if (showingProgress) {
            int percent = (int) (finished * 100.0 / numComputationUnits);
            if (percent > lastPercent) {
                if (runningOnEDT) {
                    updateProgressTracking(percent);
                } else {
                    EventQueue.invokeLater(() -> updateProgressTracking(percent));
                }
                lastPercent = percent;
            }
        }
    }

    @Override
    public void finished() {
        if (showingProgress) {
            if (runningOnEDT) {
                finishProgressTracking();
            } else {
                EventQueue.invokeLater(this::finishProgressTracking);
            }
            showingProgress = false;
            lastPercent = 0;
        }
    }

    abstract void startProgressTracking();

    abstract void updateProgressTracking(int percent);

    abstract void finishProgressTracking();
}
