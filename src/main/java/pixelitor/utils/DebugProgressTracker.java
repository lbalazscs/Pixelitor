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

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * A diagnostic progress tracker that verifies work unit estimations
 * and collects timing statistics.
 *
 * It can be a decorator for another {@link ProgressTracker}
 * to get both the statistics and the visual feedback.
 */
public class DebugProgressTracker implements ProgressTracker {
    private final long startTimeMillis;
    private final List<ProgressUpdate> progressUpdates;
    private final String opName;
    private final int expectedTotalUnits;
    private final ProgressTracker delegateTracker;
    private long lastUpdateTimeMillis;
    private int numCompletedUnits;

    public DebugProgressTracker(String opName, int expectedTotalUnits,
                                ProgressTracker delegateTracker) {
        assert expectedTotalUnits > 0;

        this.opName = opName;
        this.expectedTotalUnits = expectedTotalUnits;
        this.delegateTracker = delegateTracker;

        startTimeMillis = System.currentTimeMillis();
        lastUpdateTimeMillis = 0;
        numCompletedUnits = 0;
        progressUpdates = new ArrayList<>();

        log("created " + opName + ", expectedTotalUnits = " + expectedTotalUnits);
    }

    @Override
    public void unitDone() {
        if (delegateTracker != null) {
            delegateTracker.unitDone();
        }

        numCompletedUnits++;
        log("unitDone");
    }

    @Override
    public void unitsDone(int completedUnits) {
        if (delegateTracker != null) {
            delegateTracker.unitsDone(completedUnits);
        }

        numCompletedUnits += completedUnits;
        log("unitsDone " + completedUnits);
    }

    @Override
    public void finished() {
        if (delegateTracker != null) {
            delegateTracker.finished();
        }

        log("finished");

        printSummary();
    }

    private void log(String method) {
        long time = System.currentTimeMillis() - startTimeMillis;
        StackTraceElement caller = new Throwable().getStackTrace()[2];

        progressUpdates.add(new ProgressUpdate(
            method, time, lastUpdateTimeMillis, caller, expectedTotalUnits));

        lastUpdateTimeMillis = time;
    }

    private void printSummary() {
        System.out.print("Progress for " + opName + ", received units = " + numCompletedUnits);
        if (numCompletedUnits == expectedTotalUnits) {
            System.out.println(", OK");
        } else {
            System.out.println(", NOK, expectedTotalUnits = " + expectedTotalUnits);
        }

        long totalDuration = System.currentTimeMillis() - startTimeMillis;

        progressUpdates.stream()
            .map(progressUpdate -> progressUpdate.asString(totalDuration))
            .map(DebugProgressTracker::simplifyPackageNames)
            .forEach(System.out::println);
        System.out.println();
    }

    private static String simplifyPackageNames(String name) {
        return name
            .replace("pixelitor.", "")
            .replace("filters.", "")
            .replace("jhlabsproxies.", "")
            .replace("utils.", "");
    }

    /**
     * Information about one call to a {@link DebugProgressTracker} method.
     */
    private static class ProgressUpdate {
        private final StackTraceElement caller;
        private final String method;
        private final long time;
        private final int totalUnits;
        private final long duration;

        public ProgressUpdate(String method, long time, long lastTime,
                              StackTraceElement caller, int totalUnits) {
            this.method = method;
            this.time = time;
            this.totalUnits = totalUnits;
            this.duration = time - lastTime;
            this.caller = caller;
        }

        public String asString(long totalDuration) {
            double timeSeconds = time / 1000.0;
            double durationSeconds = duration / 1000.0;
            double durationPercentage = (duration * 100.0) / totalDuration;

            String whatWithPercent = format("%s (%.1f%%=>%.2fu)",
                method, durationPercentage, (durationPercentage / 100.0) * totalUnits);

            return format("%.2fs (dur=%.2fs): %-21s at %s.%s(%s:%d)",
                timeSeconds, durationSeconds, whatWithPercent,
                caller.getClassName(), caller.getMethodName(),
                caller.getFileName(), caller.getLineNumber());
        }
    }
}
