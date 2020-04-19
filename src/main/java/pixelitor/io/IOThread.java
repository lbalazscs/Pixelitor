/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.io;

import pixelitor.ThreadPool;

import java.awt.EventQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Makes sure that only one IO task runs at a time
 */
public class IOThread {
    private static final ThreadFactory threadFactory
            = r -> new Thread(r, "[IO thread]");
    private static final ExecutorService executor
            = Executors.newSingleThreadExecutor(threadFactory);

    private static final Set<String> currentReadPaths = new HashSet<>();
    private static final Set<String> currentWritePaths = new HashSet<>();

    // can be set to true for testing things like
    // multiple progress bars, but normally this is false
    private static final boolean ALLOW_MULTIPLE_IO_THREADS = false;

    private IOThread() {
        // should not be instantiated
    }

    public static Executor getExecutor() {
        if (ALLOW_MULTIPLE_IO_THREADS) {
            return ThreadPool.getExecutor();
        }
        return executor;
    }

    public static synchronized boolean isProcessing(String absolutePath) {
        assert EventQueue.isDispatchThread() : "not on EDT";

        if (currentReadPaths.contains(absolutePath)) {
            return true;
        }
        if (currentWritePaths.contains(absolutePath)) {
            return true;
        }
        return false;
    }

    public static synchronized void markReadProcessing(String path) {
        mark(currentReadPaths, path);
    }

    public static void markWriteProcessing(String path) {
        mark(currentWritePaths, path);
    }

    private static void mark(Set<String> trackingSet, String path) {
        assert EventQueue.isDispatchThread() : "not on EDT";

        trackingSet.add(path);
    }

    public static void readingFinishedFor(String path) {
        unMark(currentReadPaths, path);
    }

    public static void writingFinishedFor(String path) {
        unMark(currentWritePaths, path);
    }

    private static void unMark(Set<String> trackingSet, String path) {
        assert EventQueue.isDispatchThread() : "not on EDT";

        boolean contained = trackingSet.remove(path);
        assert contained;
    }

    public static boolean isBusyWriting() {
        assert EventQueue.isDispatchThread() : "not on EDT";

        return !currentWritePaths.isEmpty();
    }

    public static Set<String> getCurrentWritePaths() {
        assert EventQueue.isDispatchThread() : "not on EDT";

        return currentWritePaths;
    }
}
