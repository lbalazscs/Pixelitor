/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * This class (taken from the {@link Executor}'s Javadoc)
 * serializes the submission of tasks to a second executor
 * (it guarantees that there is only one task running at a time).
 */
public class SerialExecutor implements Executor {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Executor delegateExecutor;
    private Runnable activeTask;

    public SerialExecutor(Executor delegateExecutor) {
        this.delegateExecutor = delegateExecutor;
    }

    @Override
    public synchronized void execute(Runnable task) {
        tasks.add(() -> {
            try {
                task.run();
            } finally {
                scheduleNext();
            }
        });
        if (activeTask == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        // if there are any tasks in the queue,
        // execute the first one, otherwise do nothing
        if ((activeTask = tasks.poll()) != null) {
            delegateExecutor.execute(activeTask);
        }
    }
}
