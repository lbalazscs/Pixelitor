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

package pixelitor;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread pool for parallel execution on multiple CPU cores
 */
public class ThreadPool {
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService pool =
        Executors.newFixedThreadPool(NUM_CORES, new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "ImageProcessor-" + threadCount.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });

    private ThreadPool() {
        throw new AssertionError("utility class");
    }

    /**
     * Submits a task that doesn't return anything.
     */
    public static Future<?> submit(Runnable task) {
        return pool.submit(task);
    }

    /**
     * Submits a task that returns a value, such as
     * the calculated pixels in a line.
     */
    public static <T> Future<T> submit2(Callable<T> task) {
        return pool.submit(task);
    }

    /**
     * Waits for all futures to complete while tracking progress.
     */
    public static void waitFor(Iterable<Future<?>> futures, ProgressTracker pt) {
        assert pt != null;

        for (var future : futures) {
            try {
                future.get();

                // not completely accurate to count here, but good enough in practice
                pt.unitDone();
            } catch (InterruptedException | ExecutionException e) {
                Messages.showException(e);
            }
        }
    }

    // same as the method above, but with an array argument
    public static void waitFor(Future<?>[] futures, ProgressTracker pt) {
        assert pt != null;

        for (var future : futures) {
            try {
                future.get();
                pt.unitDone();
            } catch (InterruptedException | ExecutionException e) {
                Messages.showException(e);
            }
        }
    }

    /**
     * Similar to waitFor, but also updates given the destination image.
     * Each future represents a processed line of pixels in the image.
     */
    public static void waitFor2(Future<int[]>[] lineFutures,
                                BufferedImage dst,
                                int lineWidth,
                                ProgressTracker pt) {
        assert pt != null;

        try {
            for (int y = 0; y < lineFutures.length; y++) {
                int[] linePixels = lineFutures[y].get();
                AbstractBufferedImageOp.setRGB(dst, 0, y, lineWidth, 1, linePixels);
                pt.unitDone();
            }
        } catch (InterruptedException | ExecutionException e) {
            Messages.showException(e);
        }
    }

    public static Executor getExecutor() {
        return pool;
    }
}
