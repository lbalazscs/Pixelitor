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
import pixelitor.filters.gui.FilterParam;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertions for {@link FilterParam} objects.
 */
public class FilterParamAssert<S extends FilterParamAssert<S, T>, T extends FilterParam> extends AbstractAssert<S, T> {
    public FilterParamAssert(T actual, Class<S> selfType) {
        super(actual, selfType);
    }

    public S hasName(String name) {
        isNotNull();

        assertThat(actual.getName())
            .as("name")
            .isEqualTo(name);

        return myself;
    }

    public S isAtDefault() {
        isNotNull();

        assertThat(actual.isAtDefault())
            .as("default state")
            .isTrue();

        return myself;
    }

    public S isNotAtDefault() {
        isNotNull();

        assertThat(actual.isAtDefault())
            .as("non-default state")
            .isFalse();

        return myself;
    }

    public S shouldRandomize() {
        isNotNull();

        assertThat(actual.shouldRandomize())
            .as("randomize")
            .isTrue();

        return myself;
    }

    public S shouldNotRandomize() {
        isNotNull();

        assertThat(actual.shouldRandomize())
            .as("ignore randomize")
            .isFalse();

        return myself;
    }

    public S isEnabled() {
        isNotNull();

        assertThat(actual.isEnabled())
            .as("enabled")
            .isTrue();

        return myself;
    }

    public S isDisabled() {
        isNotNull();

        assertThat(actual.isEnabled())
            .as("disabled")
            .isFalse();

        return myself;
    }

    public S valueAsStringIs(String expectedValue) {
        isNotNull();

        assertThat(actual.getValueAsString())
            .as("value")
            .isEqualTo(expectedValue);

        return myself;
    }
}
