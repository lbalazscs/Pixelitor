/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
 * An abstract superclass for progress tracking classes which
 * show progress information after a time threshold has been exceeded.
 */
public abstract class ThresholdProgressTracker implements ProgressTracker {
    private static final int THRESHOLD_MILLIS = 200;

    private final long startTime;
    private final int numComputationUnits;

    private int finished = 0;
    private int lastPercent = 0;

    private boolean showingProgress = false;

    protected ThresholdProgressTracker(int numComputationUnits) {
        this.numComputationUnits = numComputationUnits;
        startTime = System.currentTimeMillis();
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
        double millis = System.currentTimeMillis() - startTime;
        if (millis > THRESHOLD_MILLIS) {
            int percent = ((int) (finished * 100.0 / numComputationUnits));
            if (percent != lastPercent) {
                if (!showingProgress) {
                    startProgressTracking();
                    showingProgress = true;
                }
                updateProgressTracking(percent);
                lastPercent = percent;
            }
        }
    }

    @Override
    public void finish() {
        if (showingProgress) {
            finishProgressTracking();
            showingProgress = false;
        }
    }

    abstract void startProgressTracking();

    abstract void updateProgressTracking(int percent);

    abstract void finishProgressTracking();
}
