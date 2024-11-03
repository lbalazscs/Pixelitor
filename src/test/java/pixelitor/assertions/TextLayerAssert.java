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

package pixelitor.assertions;

import pixelitor.layers.TextLayer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertions for {@link TextLayer} objects.
 */
public class TextLayerAssert extends ContentLayerAssert<TextLayerAssert, TextLayer> {
    public TextLayerAssert(TextLayer actual) {
        super(actual, TextLayerAssert.class);
    }

    public TextLayerAssert textIs(String expected) {
        isNotNull();

        assertThat(actual.getSettings().getText()).isEqualTo(expected);

        return this;
    }

    public <S> TextLayerAssert hasNumEffects(int expected) {
        isNotNull();

        int actualNumEffects = actual.getSettings().getEffects().getEnabledEffects().length;
        assertThat(actualNumEffects).isEqualTo(expected);

        return this;
    }
}
