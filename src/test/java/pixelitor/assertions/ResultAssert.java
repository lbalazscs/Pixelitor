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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.utils.Result;

import java.util.Objects;

public class ResultAssert<S, E> extends AbstractAssert<ResultAssert<S, E>, Result<S, E>> {
    public ResultAssert(Result<S, E> actual) {
        super(actual, ResultAssert.class);
    }

    public ResultAssert<S, E> isSuccess() {
        isNotNull();
        if (!actual.isSuccess()) {
            failWithMessage("Expected Result to be Success but was Error with details <%s>", actual.errorDetails());
        }
        return this;
    }

    public ResultAssert<S, E> isError() {
        isNotNull();
        if (actual.isSuccess()) {
            failWithMessage("Expected Result to be Error but was Success with value <%s>", actual.get());
        }
        return this;
    }

    public ResultAssert<S, E> hasValue(S expectedValue) {
        isNotNull();
        isSuccess();
        if (!Objects.equals(actual.get(), expectedValue)) {
            failWithMessage("Expected success value to be <%s> but was <%s>", expectedValue, actual.get());
        }
        return this;
    }

    public ResultAssert<S, E> hasErrorDetails(E expectedErrorDetails) {
        isNotNull();
        isError();
        if (!Objects.equals(actual.errorDetails(), expectedErrorDetails)) {
            failWithMessage("Expected error details to be <%s> but was <%s>", expectedErrorDetails, actual.errorDetails());
        }
        return this;
    }
}
