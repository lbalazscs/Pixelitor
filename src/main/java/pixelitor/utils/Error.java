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

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a failed computation result.
 */
public record Error<S, E>(E errorDetails) implements Result<S, E> {
    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public void ifSuccess(Consumer<? super S> consumer) {
        // do nothing
    }

    @Override
    public void ifError(Consumer<? super E> consumer) {
        consumer.accept(errorDetails);
    }

    @Override
    public S get() {
        throw new IllegalStateException("no success");
    }

    @Override
    public <T> Result<T, E> map(Function<? super S, ? extends T> mapper) {
        return new Error<>(errorDetails);
    }

    @Override
    public <T> Result<T, E> flatMap(Function<? super S, ? extends Result<? extends T, E>> mapper) {
        return new Error<>(errorDetails);
    }

    @Override
    public <F> Result<S, F> mapError(Function<? super E, ? extends F> mapper) {
        return new Error<>(mapper.apply(errorDetails));
    }
}
