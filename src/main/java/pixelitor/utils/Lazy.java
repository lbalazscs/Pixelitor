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

package pixelitor.utils;

import java.util.function.Supplier;

/**
 * A thread-safe, memoizing supplier that computes a value on the first access.
 */
public final class Lazy<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private volatile T cachedValue;

    private Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    @Override
    public T get() {
        // use double-checked locking to avoid synchronizing every call
        T result = cachedValue;
        if (result == null) { // first check (no locking)
            //noinspection SynchronizeOnThis
            synchronized (this) {
                result = cachedValue;
                if (result == null) { // second check (with locking)
                    result = supplier.get();
                    // this class does not support null-returning suppliers
                    assert result != null;
                    cachedValue = result;
                }
            }
        }
        return result;
    }

    /**
     * Invalidates the cached value, forcing re-computation on the next access.
     */
    public synchronized void invalidate() {
        cachedValue = null;
    }

    @Override
    public String toString() {
        // read the volatile field only once for a consistent view
        T value = cachedValue;
        return "Lazy[" + (value == null ? "not yet computed" : value) + "]";
    }
}
