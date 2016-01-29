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

import pixelitor.MessageHandler;

/**
 * Tracks the progress of some operation and shows a
 * status bar update if it takes a long time.
 */
public class BasicProgressTracker implements ProgressTracker {
    private static final int THRESHOLD_MILLIS = 200;
    private static final MessageHandler messageHandler = Messages.getMessageHandler();

    private final long startTime;
    private final String name;
    private final int numComputationUnits;

    private int finished = 0;
    private int lastPercent = 0;
    private boolean progressBar = false;

    public BasicProgressTracker(String name, int numComputationUnits) {
        this.name = name + ":";
        this.numComputationUnits = numComputationUnits;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void unitDone() {
        finished++;
        update();
    }

    @Override
    public void addUnits(int units) {
        finished += units;
        update();
    }

    private void update() {
        double millis = System.currentTimeMillis() - startTime;
        if (millis > THRESHOLD_MILLIS) {
            int percent = ((int) (finished * 100.0 / numComputationUnits));
            if (percent != lastPercent) {
                if (!progressBar) {
                    messageHandler.startProgress(name, 100);
                    progressBar = true;
                }
                messageHandler.updateProgress(percent);
                lastPercent = percent;
            }
        }
    }

    @Override
    public void finish() {
        if (progressBar) {
            messageHandler.stopProgress();
        }
    }
}
