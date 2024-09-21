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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the outcome of a computation that may either succeed with a value or fail with an error.
 *
 * @param <S> the type of the successful result value
 * @param <E> the type of the error details in case of failure
 */
public sealed interface Result<S, E> permits Success, Error {
    boolean isSuccess();

    /**
     * Returns the successful result value if present.
     */
    S get();

    /**
     * Returns the error details if present.
     */
    E errorDetails();

    /**
     * Maps the successful result using an S->T mapper function.
     */
    <T> Result<T, E> map(Function<? super S, ? extends T> mapper);

    /**
     * Maps and flattens the successful result using an S->Result mapper function.
     */
    <T> Result<T, E> flatMap(Function<? super S, ? extends Result<? extends T, E>> mapper);

    /**
     * Returns the success value if present,
     * or the given default value if this is an error.
     */
    default S orElse(S defaultValue) {
        return isSuccess() ? get() : defaultValue;
    }

    /**
     * Returns the success value if present, or throws the exception
     * created by the supplier if this is an error.
     */
    default <T extends Throwable> S orElseThrow(Supplier<? extends T> exceptionSupplier) throws T {
        if (isSuccess()) {
            return get();
        }
        throw exceptionSupplier.get();
    }

    /**
     * Creates a new Result representing a successful result.
     */
    static <V, E> Result<V, E> success(V value) {
        return new Success<>(value);
    }

    /**
     * Creates a new Result representing an error.
     */
    static <V, E> Result<V, E> error(E errorDetails) {
        return new Error<>(errorDetails);
    }

    /**
     * Returns a successful result if the value is non-null,
     * or an error result with null error details if the value is null.
     */
    static <V, E> Result<V, E> ofNullable(V value) {
        if (value == null) {
            return error(null);
        } else {
            return success(value);
        }
    }
}

