/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import java.util.NoSuchElementException;

/**
 * This will be replaced by java.util.Optional as soon as we
 * upgrade to Java 8
 */
public class Optional<T> {
    private final T value;

    private static final Optional<?> EMPTY = new Optional<>();

    private Optional() {
        this.value = null;
    }

    private Optional(T value) {
        if (value == null) {
            throw new IllegalStateException();
        }
        this.value = value;
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<>(value);
    }

    public static <T> Optional<T> ofNullable(T value) {
        if (value == null) {
            return (Optional<T>) EMPTY;
        } else {
            return of(value);
        }
    }

    public T get() {
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public static <T> Optional<T> empty() {
        return (Optional<T>) EMPTY;
    }
}
