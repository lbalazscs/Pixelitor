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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Result tests")
class ResultTest {
    @Nested
    @DisplayName("map tests")
    class MapTests {
        @Test
        @DisplayName("mapping a Success")
        void map_onSuccess() {
            Result<String, Integer> result = new Success<>("a");
            Result<String, Integer> mapped = result.map(s -> s.toUpperCase(Locale.ENGLISH));

            // expect successful mapping
            assertThat(mapped)
                .isSuccess()
                .hasValue("A");
        }

        @Test
        @DisplayName("mapping an Error")
        void map_onError() {
            Result<String, Integer> result = new Error<>(2);
            Result<String, Integer> mapped = result.map(s -> s.toUpperCase(Locale.ENGLISH));

            // expect the original error
            assertThat(mapped)
                .isError()
                .hasErrorDetails(2);
        }

        @Test
        @DisplayName("null mapper result")
        void mapToNull() {
            Result<String, Integer> result = new Success<>("test");
            Result<String, Integer> mapped = result.map(s -> null);

            assertThat(mapped)
                .isSuccess()
                .hasValue(null);
        }
    }

    @Nested
    @DisplayName("flatMap tests")
    class FlatMapTests {
        @Test
        @DisplayName("flatMapping Success to Success")
        void flatMap_OK_OK() {
            Result<String, Integer> result = new Success<>("a");
            Result<String, Integer> mapped = result.flatMap(s ->
                new Success<>(s.toUpperCase(Locale.ENGLISH)));

            // expect successful mapping
            assertThat(mapped)
                .isSuccess()
                .hasValue("A");
        }

        @Test
        @DisplayName("flatMapping Success to Error")
        void flatMap_OK_Error() {
            Result<String, Integer> result = new Success<>("a");
            Result<String, Integer> mapped = result.flatMap(s -> new Error<>(10));

            // expect error
            assertThat(mapped)
                .isError()
                .hasErrorDetails(10);
        }

        @Test
        @DisplayName("flatMapping Error to Success")
        void flatMap_Error_OK() {
            Result<String, Integer> result = new Error<>(2);
            Result<String, Integer> mapped = result.flatMap(s ->
                new Success<>(s.toUpperCase(Locale.ENGLISH)));

            // expect error ignoring the mapping
            assertThat(mapped)
                .isError()
                .hasErrorDetails(2);
        }

        @Test
        @DisplayName("flatMapping Error to Error")
        void flatMap_Error_Error() {
            Result<String, Integer> result = new Error<>(2);
            Result<String, Integer> mapped = result.flatMap(s -> new Error<>(10));

            // expect error with the first error detail
            assertThat(mapped)
                .isError()
                .hasErrorDetails(2);
        }
    }

    @Nested
    @DisplayName("mapError tests")
    class MapErrorTests {
        @Test
        @DisplayName("mapError on a Success")
        void mapError_onSuccess() {
            Result<String, Integer> result = new Success<>("a");
            Result<String, String> mapped = result.mapError(Object::toString);

            // expect the original success
            assertThat(mapped)
                .isSuccess()
                .hasValue("a");
        }

        @Test
        @DisplayName("mapError on an Error")
        void mapError_onError() {
            Result<String, Integer> result = new Error<>(123);
            Result<String, String> mapped = result.mapError(Object::toString);

            // expect the mapped error
            assertThat(mapped)
                .isError()
                .hasErrorDetails("123");
        }

        @Test
        @DisplayName("mapError to null")
        void mapError_toNull() {
            Result<String, Integer> result = new Error<>(123);
            Result<String, String> mapped = result.mapError(e -> null);

            // expect an error with null details
            assertThat(mapped)
                .isError()
                .hasErrorDetails(null);
        }
    }

    @Nested
    @DisplayName("orElse tests")
    class OrElseTests {
        @Test
        @DisplayName("orElse with Success")
        void orElseWithSuccess() {
            Result<String, Integer> result = new Success<>("successValue");
            String value = result.orElse("default");

            // expect the original success value
            assertThat(value).isEqualTo("successValue");
        }

        @Test
        @DisplayName("orElse with Error")
        void orElseWithError() {
            Result<String, String> result = new Error<>("error");
            String value = result.orElse("default");

            // expect the default value
            assertThat(value).isEqualTo("default");
        }

        @Test
        @DisplayName("orElseThrow with Success")
        void orElseThrowWithSuccess() {
            Result<String, Integer> result = new Success<>("successValue");
            String value = result.orElseThrow(() ->
                new IllegalStateException("Oh, no!"));

            // expect the original success value
            assertThat(value).isEqualTo("successValue");
        }

        @Test
        @DisplayName("orElseThrow with Error")
        void orElseThrowWithError() {
            Result<String, Integer> result = new Error<>(42);
            Supplier<RuntimeException> exceptionSupplier = () ->
                new RuntimeException("Oh, yes!");

            // expect throwing the exception
            assertThatThrownBy(() -> result.orElseThrow(exceptionSupplier))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Oh, yes!");
        }
    }

    @Nested
    @DisplayName("factory methods tests")
    class FactoryMethodTests {
        @Test
        @DisplayName("success()")
        void successFactory() {
            Result<String, Integer> result = Result.success("test");

            assertThat(result)
                .isSuccess()
                .hasValue("test");
        }

        @Test
        @DisplayName("error()")
        void errorFactory() {
            Result<String, Integer> result = Result.error(404);

            assertThat(result)
                .isError()
                .hasErrorDetails(404);
        }

        @Test
        @DisplayName("ofNullable() with non-null value")
        void ofNullableWithNonNull() {
            Result<String, Object> result = Result.ofNullable("test");

            // expect a Success with the original value
            assertThat(result)
                .isSuccess()
                .hasValue("test");
        }

        @Test
        @DisplayName("ofNullable() with null value")
        void ofNullableWithNull() {
            Result<String, Object> result = Result.ofNullable(null);

            assertThat(result)
                .isError()
                .hasErrorDetails(null);
        }

        @Test
        @DisplayName("ofNullable() with supplier and non-null value")
        void ofNullableWithSupplier_nonNull() {
            Result<String, String> result = Result.ofNullable("test", () -> "error");

            // expect a Success with the original value
            assertThat(result)
                .isSuccess()
                .hasValue("test");
        }

        @Test
        @DisplayName("ofNullable() with supplier and null value")
        void ofNullableWithSupplier_null() {
            Result<String, String> result = Result.ofNullable(null, () -> "error");

            // expect an Error with the supplied error details
            assertThat(result)
                .isError()
                .hasErrorDetails("error");
        }
    }

    @Nested
    @DisplayName("state tests")
    class StateTests {
        @Test
        @DisplayName("isSuccess() on Success")
        void isSuccess_onSuccess() {
            Result<String, Integer> result = new Success<>("test");
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("isSuccess() on Error")
        void isSuccess_onError() {
            Result<String, Integer> result = new Error<>(1);
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("isError() on Success")
        void isError_onSuccess() {
            Result<String, Integer> result = new Success<>("test");
            assertThat(result.isError()).isFalse();
        }

        @Test
        @DisplayName("isError() on Error")
        void isError_onError() {
            Result<String, Integer> result = new Error<>(1);
            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("conditional execution tests")
    class ConditionalExecutionTests {
        @Test
        @DisplayName("ifSuccess() on Success")
        void ifSuccess_onSuccess() {
            Result<String, Integer> result = new Success<>("value");
            var ref = new AtomicReference<String>();
            result.ifSuccess(ref::set);

            // expect the consumer to be called
            assertThat(ref.get()).isEqualTo("value");
        }

        @Test
        @DisplayName("ifSuccess() on Error")
        void ifSuccess_onError() {
            Result<String, Integer> result = new Error<>(1);
            var ref = new AtomicReference<String>();
            result.ifSuccess(ref::set);

            // expect the consumer to not be called
            assertThat(ref.get()).isNull();
        }

        @Test
        @DisplayName("ifError() on Success")
        void ifError_onSuccess() {
            Result<String, Integer> result = new Success<>("value");
            var ref = new AtomicReference<Integer>();
            result.ifError(ref::set);

            // expect the consumer to not be called
            assertThat(ref.get()).isNull();
        }

        @Test
        @DisplayName("ifError() on Error")
        void ifError_onError() {
            Result<String, Integer> result = new Error<>(42);
            var ref = new AtomicReference<Integer>();
            result.ifError(ref::set);

            // expect the consumer to be called
            assertThat(ref.get()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("error cases")
    class ErrorCaseTests {
        @Test
        @DisplayName("get() on Error")
        void getOnError() {
            Result<String, Integer> result = new Error<>(404);

            assertThatThrownBy(result::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("no success");
        }

        @Test
        @DisplayName("errorDetails() on Success")
        void errorDetailsOnSuccess() {
            Result<String, Integer> result = new Success<>("test");

            assertThatThrownBy(result::errorDetails)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("no error");
        }
    }
}
