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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Result tests")
class ResultTest {
    @Nested
    @DisplayName("map tests")
    class MapTests {
        @Test
        @DisplayName("mapping a Success")
        void map_OK() {
            Result<String, Integer> result = new Success<>("a");
            Result<String, Integer> mapped = result.map(String::toUpperCase);

            // expect successful mapping
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.get()).isEqualTo("A");
        }

        @Test
        @DisplayName("mapping an Error")
        void map_Error() {
            Result<String, Integer> result = new Error<>(2);
            Result<String, Integer> mapped = result.map(String::toUpperCase);

            // expect the original error
            assertThat(mapped.isSuccess()).isFalse();
            assertThat(mapped.errorDetails()).isEqualTo(2);
        }

        @Test
        @DisplayName("null mapper result")
        void mapToNull() {
            Result<String, Integer> result = new Success<>("test");
            Result<String, Integer> mapped = result.map(s -> null);

            // expect successful null
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.get()).isNull();
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
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.get()).isEqualTo("A");
        }

        @Test
        @DisplayName("flatMapping Success to Error")
        void flatMap_OK_Error() {
            Result<String, Integer> result = new Success<>("a");
            Result<String, Integer> mapped = result.flatMap(s -> new Error<>(10));

            // expect error
            assertThat(mapped.isSuccess()).isFalse();
            assertThat(mapped.errorDetails()).isEqualTo(10);
        }

        @Test
        @DisplayName("flatMapping Error to Success")
        void flatMap_Error_OK() {
            Result<String, Integer> result = new Error<>(2);
            Result<String, Integer> mapped = result.flatMap(s ->
                new Success<>(s.toUpperCase(Locale.ENGLISH)));

            // expect error ignoring the mapping
            assertThat(mapped.isSuccess()).isFalse();
            assertThat(mapped.errorDetails()).isEqualTo(2);
        }

        @Test
        @DisplayName("flatMapping Error to Error")
        void flatMap_Error_Error() {
            Result<String, Integer> result = new Error<>(2);
            Result<String, Integer> mapped = result.flatMap(s -> new Error<>(10));

            // expect error with the first error detail
            assertThat(mapped.isSuccess()).isFalse();
            assertThat(mapped.errorDetails()).isEqualTo(2);
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

            // expect a Success instance
            assertThat(result).isInstanceOf(Success.class);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isEqualTo("test");
        }

        @Test
        @DisplayName("error()")
        void errorFactory() {
            Result<String, Integer> result = Result.error(404);

            // expect an Error instance
            assertThat(result).isInstanceOf(Error.class);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorDetails()).isEqualTo(404);
        }

        @Test
        @DisplayName("ofNullable() with non-null value")
        void ofNullableWithNonNull() {
            Result<String, Object> result = Result.ofNullable("test");

            // expect a Success with the original value
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isEqualTo("test");
        }

        @Test
        @DisplayName("ofNullable() with null value")
        void ofNullableWithNull() {
            Result<String, Object> result = Result.ofNullable(null);

            // expect an Error with null error details
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorDetails()).isNull();
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