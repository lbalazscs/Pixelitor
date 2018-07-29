/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Makes sure that only one IO task runs at a time
 */
public class IOThread {
    private static final ExecutorService executor
            = Executors.newSingleThreadExecutor();

    private static final Set<String> currentPaths = new HashSet<>();

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

    /**
     * Returns true if the file is currently processed or queued to be processed,
     * otherwise it returns false and marks the file as processed
     */
    public static synchronized boolean isProcessing(File file) {
        String path = file.getAbsolutePath();
        if (currentPaths.contains(path)) {
            return true;
        }
        currentPaths.add(path);
        return false;
    }

    public static synchronized void processingFinishedFor(File file) {
        String path = file.getAbsolutePath();
        boolean contained = currentPaths.remove(path);
        assert contained;
    }
}
