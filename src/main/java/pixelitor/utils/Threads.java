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

package pixelitor.utils;

import pixelitor.ThreadPool;
import pixelitor.io.IOTasks;

import java.awt.EventQueue;
import java.util.concurrent.Executor;

/**
 * Static executors and thread-related utility methods.
 */
public class Threads {
    public static final Executor onEDT = EventQueue::invokeLater;

    public static final Executor onIOThread = IOTasks.getExecutor();

    public static final Executor onPool = ThreadPool.getExecutor();

    private Threads() {
        // don't instantiate
    }

    public static boolean calledOnEDT() {
        return EventQueue.isDispatchThread();
    }

    public static boolean calledOn(String expectedThreadName) {
        return threadName().equals(expectedThreadName);
    }

    public static boolean calledOutsideEDT() {
        return !EventQueue.isDispatchThread();
    }

    public static String threadInfo() {
        return "Called on " + threadName();
    }

    public static String threadName() {
        return Thread.currentThread().getName();
    }

    public static void dumpStack() {
        Thread thread = Thread.currentThread();
        System.out.printf("Threads::dumpStack: called on ('%s', %d)%n",
            thread.getName(), thread.threadId());

        Thread.dumpStack();
    }
}
