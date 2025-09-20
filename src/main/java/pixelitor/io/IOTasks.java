/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Makes sure that only one IO task runs at a time
 */
public class IOTasks {
    private static final Executor executor
        = new SerialExecutor(ThreadPool.getExecutor());

    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final Lock readLock = readWriteLock.readLock();
    private static final Lock writeLock = readWriteLock.writeLock();

    private static final Set<String> activeReadPaths = new HashSet<>();
    private static final Set<String> activeWritePaths = new HashSet<>();

    // can be set to true for testing things like
    // multiple progress bars, but normally this is false
    private static final boolean ALLOW_CONCURRENT_IO = false;

    private IOTasks() {
        // should not be instantiated
    }

    public static Executor getExecutor() {
        if (ALLOW_CONCURRENT_IO) {
            return ThreadPool.getExecutor();
        }
        return executor;
    }

    public static synchronized boolean isPathProcessing(String path) {
        readLock.lock();
        try {
            return activeReadPaths.contains(path) || activeWritePaths.contains(path);
        } finally {
            readLock.unlock();
        }
    }

    public static void markPathForReading(String path) {
        writeLock.lock();
        try {
            activeReadPaths.add(path);
        } finally {
            writeLock.unlock();
        }
    }

    public static void markPathForWriting(String path) {
        writeLock.lock();
        try {
            activeWritePaths.add(path);
        } finally {
            writeLock.unlock();
        }
    }

    public static void markReadingComplete(String path) {
        writeLock.lock();
        try {
            boolean contained = activeReadPaths.remove(path);
            assert contained : "Path was not being tracked for reading: " + path;
        } finally {
            writeLock.unlock();
        }
    }

    public static void markWritingComplete(String path) {
        writeLock.lock();
        try {
            boolean contained = activeWritePaths.remove(path);
            assert contained : "Path was not being tracked for writing: " + path;
        } finally {
            writeLock.unlock();
        }
    }

    public static boolean hasActiveWrites() {
        readLock.lock();
        try {
            return !activeWritePaths.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    public static Set<String> getActiveWritePaths() {
        readLock.lock();
        try {
            return new HashSet<>(activeWritePaths);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Waits for all IO operations to complete.
     */
    public static void waitForIdle() {
        // waiting until an empty task finishes works
        // because the IO executor is serialized
        var latch = new CountDownLatch(1);
        getExecutor().execute(latch::countDown);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Utils.sleep(500, TimeUnit.MILLISECONDS);
    }
}
