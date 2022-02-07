/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents either the result of a computation or an error.
 * Similar idea to Rust's Result or Haskell's Either.
 *
 * @param <V> the type of the successful result
 * @param <E> the type of the error details
 */
public sealed interface Result<V, E> {
    boolean isOK();

    V get();

    E errorDetail();

    <W> Result<W, E> map(Function<? super V, ? extends W> mapper);

    <W> Result<W, E> flatMap(Function<? super V, ? extends Result<? extends W, E>> mapper);

    static <V, E> Result<V, E> ok(V value) {
        return new OK<>(value);
    }

    static <V, E> Result<V, E> error(E errorDetail) {
        return new Error<>(errorDetail);
    }
}

record OK<V, E>(V value) implements Result<V, E> {
    @Override
    public boolean isOK() {
        return true;
    }

    @Override
    public V get() {
        return value;
    }

    @Override
    public E errorDetail() {
        throw new IllegalStateException("no error");
    }

    @Override
    public <W> Result<W, E> map(Function<? super V, ? extends W> mapper) {
        return new OK<>(mapper.apply(value));
    }

    @Override
    public <W> Result<W, E> flatMap(Function<? super V, ? extends Result<? extends W, E>> mapper) {
        @SuppressWarnings("unchecked")
        Result<W, E> returnValue = (Result<W, E>) mapper.apply(value);
        return Objects.requireNonNull(returnValue);
    }
}

record Error<V, E>(E errorDetail) implements Result<V, E> {
    @Override
    public boolean isOK() {
        return false;
    }

    @Override
    public V get() {
        throw new IllegalStateException("no value");
    }

    @Override
    public <W> Result<W, E> map(Function<? super V, ? extends W> mapper) {
        return new Error<>(errorDetail);
    }

    @Override
    public <W> Result<W, E> flatMap(Function<? super V, ? extends Result<? extends W, E>> mapper) {
        return new Error<>(errorDetail);
    }
}
