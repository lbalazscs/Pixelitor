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

import java.util.function.Function;

/**
 * Represents either the result of a computation or an error.
 * Similar idea to Rust's Result or Haskell's Either.
 *
 * @param <V> the type of the successful result
 * @param <E> the type of the error details
 */
public interface Result<V, E> {
    boolean isOK();

    V get();

    E getError();

    <W> Result<W, E> map(Function<? super V, ? extends W> mapper);

    static <V, E> Result<V, E> ok(V value) {
        return new OK<>(value);
    }

    static <V, E> Result<V, E> error(E errorDetail) {
        return new Error<>(errorDetail);
    }
}

class OK<V, E> implements Result<V, E> {
    private final V value;

    OK(V value) {
        this.value = value;
    }

    @Override
    public boolean isOK() {
        return true;
    }

    @Override
    public V get() {
        return value;
    }

    @Override
    public E getError() {
        throw new IllegalStateException("no error");
    }

    @Override
    public <W> Result<W, E> map(Function<? super V, ? extends W> mapper) {
        return new OK<>(mapper.apply(value));
    }
}

class Error<V, E> implements Result<V, E> {
    private final E errorDetail;

    Error(E errorDetail) {
        this.errorDetail = errorDetail;
    }

    @Override
    public boolean isOK() {
        return false;
    }

    @Override
    public V get() {
        throw new IllegalStateException("no value");
    }

    @Override
    public E getError() {
        return errorDetail;
    }

    @Override
    public <W> Result<W, E> map(Function<? super V, ? extends W> mapper) {
        return new Error<>(errorDetail);
    }
}
