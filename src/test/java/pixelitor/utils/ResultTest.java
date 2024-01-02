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

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {
    @Test
    void map_OK() {
        Result<String, Integer> result = new Success<>("a");
        Result<String, Integer> mapped = result.map(String::toUpperCase);

        // expect successful mapping
        assertThat(mapped.wasSuccess()).isTrue();
        assertThat(mapped.get()).isEqualTo("A");
    }

    @Test
    void map_Error() {
        Result<String, Integer> result = new Error<>(2);
        Result<String, Integer> mapped = result.map(String::toUpperCase);

        // expect the original error
        assertThat(mapped.wasSuccess()).isFalse();
        assertThat(mapped.errorDetail()).isEqualTo(2);
    }

    @Test
    void flatMap_OK_OK() {
        Result<String, Integer> result = new Success<>("a");
        Result<String, Integer> mapped = result.flatMap(s ->
            new Success<>(s.toUpperCase(Locale.ENGLISH)));

        // expect successful mapping
        assertThat(mapped.wasSuccess()).isTrue();
        assertThat(mapped.get()).isEqualTo("A");
    }

    @Test
    void flatMap_OK_Error() {
        Result<String, Integer> result = new Success<>("a");
        Result<String, Integer> mapped = result.flatMap(s -> new Error<>(10));

        // expect error
        assertThat(mapped.wasSuccess()).isFalse();
        assertThat(mapped.errorDetail()).isEqualTo(10);
    }

    @Test
    void flatMap_Error_OK() {
        Result<String, Integer> result = new Error<>(2);
        Result<String, Integer> mapped = result.flatMap(s ->
            new Success<>(s.toUpperCase(Locale.ENGLISH)));

        // expect error ignoring the mapping
        assertThat(mapped.wasSuccess()).isFalse();
        assertThat(mapped.errorDetail()).isEqualTo(2);
    }

    @Test
    void flatMap_Error_Error() {
        Result<String, Integer> result = new Error<>(2);
        Result<String, Integer> mapped = result.flatMap(s -> new Error<>(10));

        // expect error with the first error detail
        assertThat(mapped.wasSuccess()).isFalse();
        assertThat(mapped.errorDetail()).isEqualTo(2);
    }
}