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

/**
 * A thread pool for parallel execution on multiple CPU cores
 */
public class ThreadPool {
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService pool =
        Executors.newFixedThreadPool(NUM_CORES);

    private ThreadPool() {
    }

    /**
     * Submits a task that doesn't return anything
     */
    public static Future<?> submit(Runnable task) {
        return pool.submit(task);
    }

    /**
     * Submits a task that returns something, such as
     * the calculated pixels in a line
     */
    public static <T> Future<T> submit2(Callable<T> task) {
        return pool.submit(task);
    }

    /**
     * Blocks the current thread until all the given futures finish
     * their tasks. During this time, it updates the progress using
     * the given {@link ProgressTracker}.
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

    // same as the above, but with an array argument
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
     * Similar to waitFor, but works with futures
     * that return an int array representing a line, and
     * updates the given destination image with the new pixels.
     */
    public static void waitFor2(Future<int[]>[] futures, BufferedImage dst, int width, ProgressTracker pt) {
        assert pt != null;

        try {
            for (int i = 0; i < futures.length; i++) {
                var lineFuture = futures[i];
                int[] linePixels = lineFuture.get();
                AbstractBufferedImageOp.setRGB(dst, 0, i, width, 1, linePixels);

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
