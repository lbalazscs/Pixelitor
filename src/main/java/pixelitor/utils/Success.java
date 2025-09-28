/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a successful computation result.
 */
public record Success<S, E>(S value) implements Result<S, E> {
    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public void ifSuccess(Consumer<? super S> consumer) {
        consumer.accept(value);
    }

    @Override
    public void ifError(Consumer<? super E> consumer) {
        // do nothing
    }

    @Override
    public S get() {
        return value;
    }

    @Override
    public E errorDetails() {
        throw new IllegalStateException("no error");
    }

    @Override
    public <T> Result<T, E> map(Function<? super S, ? extends T> mapper) {
        return new Success<>(mapper.apply(value));
    }

    @Override
    public <T> Result<T, E> flatMap(Function<? super S, ? extends Result<? extends T, E>> mapper) {
        @SuppressWarnings("unchecked")
        Result<T, E> returnValue = (Result<T, E>) mapper.apply(value);
        return Objects.requireNonNull(returnValue);
    }

    @Override
    public <F> Result<S, F> mapError(Function<? super E, ? extends F> mapper) {
        return new Success<>(value);
    }
}
