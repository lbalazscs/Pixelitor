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

import java.util.ArrayList;
import java.util.List;

/**
 * A progress tracker that is used only for development
 */
public class DebugProgressTracker implements ProgressTracker {
    private final long startTimeMillis;
    private final List<CallInfo> logs;
    private final String name;
    private final int expectedTotalUnits;
    private long lastTime;
    private int calledUnits;

    public DebugProgressTracker(String name, int expectedTotalUnits) {
        this.name = name;
        this.expectedTotalUnits = expectedTotalUnits;
        startTimeMillis = System.currentTimeMillis();
        lastTime = 0;
        calledUnits = 0;
        logs = new ArrayList<>();
        log("created");
    }

    @Override
    public void unitDone() {
        calledUnits++;
        log("unitDone");
    }

    @Override
    public void addUnits(int units) {
        calledUnits += units;
        log("addUnits " + units);
    }

    @Override
    public void finish() {
        log("finish");
        long totalDuration = System.currentTimeMillis() - startTimeMillis;

        System.out.print("Progress for " + name + ", received units = " + calledUnits);
        if (calledUnits == expectedTotalUnits) {
            System.out.println(", OK");
        } else {
            System.out.println(", NOK, expectedTotalUnits = " + expectedTotalUnits);
        }
        logs.stream()
                .map(callInfo -> callInfo.asString(totalDuration))
                .map(s -> s.replace("pixelitor.", ""))
                .map(s -> s.replace("filters.", ""))
                .map(s -> s.replace("jhlabsproxies.", ""))
                .map(s -> s.replace("utils.", ""))
                .forEach(System.out::println);
        System.out.println();
    }

    private void log(String what) {
        long time = System.currentTimeMillis() - startTimeMillis;
        StackTraceElement ste = new Throwable().getStackTrace()[2];

        logs.add(new CallInfo(what, time, lastTime, ste));

        lastTime = time;
    }


    private static class CallInfo {
        private final StackTraceElement ste;
        private final String what;
        private final long time;
        private final long duration;

        public CallInfo(String what, long time, long lastTime, StackTraceElement ste) {
            this.what = what;
            this.time = time;
            this.duration = time - lastTime;
            this.ste = ste;
        }

        public String asString(long totalDuration) {
            double timeSeconds = time / 1000.0;
            double durationSeconds = duration / 1000.0;

            double durationPercentage = (duration * 100.0) / totalDuration;

            String whatWithPercent = String.format("%s (%.1f%%)", what, durationPercentage);

            return String.format("%.2f:%.2f: %-21s at %s.%s(%s:%d)", timeSeconds, durationSeconds, whatWithPercent,
                    ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber());
        }
    }
}
