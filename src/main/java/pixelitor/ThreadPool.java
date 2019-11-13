/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A thread pool for parallel execution on multiple CPU cores
 */
public class ThreadPool {
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(NUM_CORES);

    private ThreadPool() {
    }

    /**
     * Submits a task that doesn't return anything
     */
    public static Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    /**
     * Submits a task that returns something, such as
     * the calculated pixels in a line
     */
    public static <T> Future<T> submit2(Callable<T> task) {
        return executorService.submit(task);
    }

    /**
     * Waits until all the given futures complete their
     * computation, and updates the given
     * {@link ProgressTracker} in the meantime.
     */
    public static void waitToFinish(Iterable<Future<?>> futures, ProgressTracker pt) {
        assert pt != null;

        for (Future<?> future : futures) {
            try {
                future.get();

                // not completely accurate because the submit order is not
                // necessarily the same as the finish order, but
                // good enough in practice
                pt.unitDone();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    // same as the above, but with array argument
    public static void waitToFinish(Future<?>[] futures, ProgressTracker pt) {
        assert pt != null;

        for (Future<?> future : futures) {
            try {
                future.get();
                pt.unitDone();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Similar to waitToFinish, but works with futures
     * that return an int array representing a line, and
     * updates the given destination image with the new pixels.
     */
    public static void waitToFinish2(Future<int[]>[] futures, BufferedImage dst, int width, ProgressTracker pt) {
        assert pt != null;

        try {
            for (int i = 0; i < futures.length; i++) {
                Future<int[]> line = futures[i];
                int[] linePixels = line.get();
                AbstractBufferedImageOp.setRGB(dst, 0, i, width, 1, linePixels);

                pt.unitDone();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static Executor getExecutor() {
        return executorService;
    }
}
