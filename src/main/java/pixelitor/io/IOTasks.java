/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
 * Coordinates I/O operations by serializing their execution
 * and tracking actively read and written file paths.
 */
public class IOTasks {
    private static final Executor executor
        = new SerialExecutor(ThreadPool.getExecutor());

    private static final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    private static final Lock stateReadLock = stateLock.readLock();
    private static final Lock stateWriteLock = stateLock.writeLock();

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

    public static boolean isPathInUse(String path) {
        stateReadLock.lock();
        try {
            return activeReadPaths.contains(path) || activeWritePaths.contains(path);
        } finally {
            stateReadLock.unlock();
        }
    }

    public static void markReadingStarted(String path) {
        stateWriteLock.lock();
        try {
            activeReadPaths.add(path);
        } finally {
            stateWriteLock.unlock();
        }
    }

    public static void markWritingStarted(String path) {
        stateWriteLock.lock();
        try {
            activeWritePaths.add(path);
        } finally {
            stateWriteLock.unlock();
        }
    }

    public static void markReadingComplete(String path) {
        stateWriteLock.lock();
        try {
            boolean wasPresent = activeReadPaths.remove(path);
            assert wasPresent : "Path was not being tracked for reading: " + path;
        } finally {
            stateWriteLock.unlock();
        }
    }

    public static void markWritingComplete(String path) {
        stateWriteLock.lock();
        try {
            boolean wasPresent = activeWritePaths.remove(path);
            assert wasPresent : "Path was not being tracked for writing: " + path;
        } finally {
            stateWriteLock.unlock();
        }
    }

    public static boolean hasActiveWrites() {
        stateReadLock.lock();
        try {
            return !activeWritePaths.isEmpty();
        } finally {
            stateReadLock.unlock();
        }
    }

    public static Set<String> getActiveWritePaths() {
        stateReadLock.lock();
        try {
            return Set.copyOf(activeWritePaths);
        } finally {
            stateReadLock.unlock();
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
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        Utils.sleep(500, TimeUnit.MILLISECONDS);
    }
}
