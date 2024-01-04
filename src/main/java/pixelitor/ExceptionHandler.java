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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Handles uncaught exceptions
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static final ExceptionHandler INSTANCE = new ExceptionHandler();
    private final List<BiConsumer<Thread, Throwable>> handlers = new ArrayList<>(1);

    private ExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void addHandler(BiConsumer<Thread, Throwable> handler) {
        handlers.add(handler);
    }

    /**
     * This method can be used to make sure that the given
     * handler runs before the standard message dialog.
     */
    public void addFirstHandler(BiConsumer<Thread, Throwable> handler) {
        handlers.addFirst(handler);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        // avoid infinite loop where the exception is thrown
        // from Messages.showException on the EDT
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            if (ste.getMethodName().equals("showException")) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
                return;
            }
        }

        for (BiConsumer<Thread, Throwable> handler : handlers) {
            handler.accept(t, e);
        }
    }
}
