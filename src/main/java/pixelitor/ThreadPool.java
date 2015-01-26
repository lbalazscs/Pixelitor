/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPool {
    public static final int NUM_AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    // not all filters respect this setting!
    public static boolean runMultiThreaded() {
        return NUM_AVAILABLE_PROCESSORS > 1;
    }

    public static final ExecutorService executorService =
            Executors.newFixedThreadPool(NUM_AVAILABLE_PROCESSORS);

    private ThreadPool() {
    }
//    public static final ExecutorService executorService =
//            Executors.newFixedThreadPool(1);


    public static void waitForFutures(Future<?>[] futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

}
