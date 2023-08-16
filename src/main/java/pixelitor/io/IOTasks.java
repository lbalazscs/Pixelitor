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

package pixelitor.io;

import pixelitor.ThreadPool;
import pixelitor.utils.SerialExecutor;
import pixelitor.utils.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * Makes sure that only one IO task runs at a time
 */
public class IOTasks {
    private static final Executor executor
        = new SerialExecutor(ThreadPool.getExecutor());

    private static final Set<String> currentReadPaths = new HashSet<>();
    private static final Set<String> currentWritePaths = new HashSet<>();

    // can be set to true for testing things like
    // multiple progress bars, but normally this is false
    private static final boolean ALLOW_MULTIPLE_IO_THREADS = false;

    private IOTasks() {
        // should not be instantiated
    }

    public static Executor getExecutor() {
        if (ALLOW_MULTIPLE_IO_THREADS) {
            return ThreadPool.getExecutor();
        }
        return executor;
    }

    public static synchronized boolean isProcessing(String path) {
        assert calledOnEDT() : threadInfo();

        return currentReadPaths.contains(path)
            || currentWritePaths.contains(path);
    }

    public static synchronized void markReadProcessing(String path) {
        mark(currentReadPaths, path);
    }

    public static void markWriteProcessing(String path) {
        mark(currentWritePaths, path);
    }

    private static void mark(Set<String> trackingSet, String path) {
        assert calledOnEDT() : threadInfo();

        trackingSet.add(path);
    }

    public static void readingFinishedFor(String path) {
        unMark(currentReadPaths, path);
    }

    public static void writingFinishedFor(String path) {
        unMark(currentWritePaths, path);
    }

    private static void unMark(Set<String> trackingSet, String path) {
        assert calledOnEDT() : threadInfo();

        boolean contained = trackingSet.remove(path);
        assert contained;
    }

    public static boolean isBusyWriting() {
        assert calledOnEDT() : threadInfo();

        return !currentWritePaths.isEmpty();
    }

    public static Set<String> getCurrentWritePaths() {
        assert calledOnEDT() : threadInfo();

        return currentWritePaths;
    }

    /**
     * Waits until all IO operations have finished
     */
    public static void waitForIdle() {
        // make sure that the IO task is started
        Utils.sleep(200, TimeUnit.MILLISECONDS);

        // waiting until an empty task finishes works
        // because the IO executor is serialized
        var latch = new CountDownLatch(1);
        getExecutor().execute(latch::countDown);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Utils.sleep(200, TimeUnit.MILLISECONDS);
    }
}
