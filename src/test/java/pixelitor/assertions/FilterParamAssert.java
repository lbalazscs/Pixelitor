/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

    public S nameIs(String name) {
        isNotNull();

        assertThat(actual.getName()).isEqualTo(name);

        return myself;
    }

    public S isDefault() {
        isNotNull();

        assertThat(actual.hasDefault()).isTrue();

        return myself;
    }

    public S isNotSetToDefault() {
        isNotNull();

        assertThat(actual.hasDefault()).isFalse();

        return myself;
    }
}
