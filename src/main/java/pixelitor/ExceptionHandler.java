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

package pixelitor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * A central registry for uncaught exception handlers.
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static final ExceptionHandler INSTANCE = new ExceptionHandler();
    private final List<BiConsumer<Thread, Throwable>> handlers = new CopyOnWriteArrayList<>();

    // tracks if a handler is active on a given thread
    private static final ThreadLocal<Boolean> isHandling = ThreadLocal.withInitial(() -> false);

    private ExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Adds a handler to be executed after all other handlers.
     */
    public void appendHandler(BiConsumer<Thread, Throwable> handler) {
        handlers.add(handler);
    }

    /**
     * Adds a handler to be executed before all other registered handlers.
     * This method can be used to make sure that the given
     * handler runs before the standard message dialog.
     */
    public void prependHandler(BiConsumer<Thread, Throwable> handler) {
        handlers.addFirst(handler);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (isHandling.get()) {
            // necessary to avoid infinite loops despite the try-catch:
            // if an exception handler shows a modal dialog, Swing starts
            // a secondary event loop, which can throw an uncaught
            // exception before the finally block can clear the flag
            System.err.println("Re-entrant call detected:");
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return;
        }

        try {
            isHandling.set(true);
            for (BiConsumer<Thread, Throwable> handler : handlers) {
                handler.accept(t, e);
            }
        } catch (Throwable newEx) {
            // an exception was thrown from an exception handler
            System.err.println("Exception thrown from an exception handler:");
            //noinspection CallToPrintStackTrace
            newEx.printStackTrace();
        } finally {
            isHandling.remove(); // clear the flag
        }
    }
}
