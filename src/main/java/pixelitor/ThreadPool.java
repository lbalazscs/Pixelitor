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

package pixelitor;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPool {
    private static final int NUM_AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(NUM_AVAILABLE_PROCESSORS);

    private ThreadPool() {
    }

    public static Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    public static void waitForFutures(Future<?>[] futures, ProgressTracker pt) {
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

    public static void waitForFutures2(BufferedImage dst, int width, Future<int[]>[] futures, ProgressTracker pt) {
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
}
