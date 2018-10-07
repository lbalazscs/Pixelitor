/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.ContentLayer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertions for {@link ContentLayer} objects.
 */
public class ContentLayerAssert<S extends ContentLayerAssert<S, T>, T extends ContentLayer> extends LayerAssert<S, T> {
    public ContentLayerAssert(T actual, Class<S> selfType) {
        super(actual, selfType);
    }

    public S translationIs(int tx, int ty) {
        isNotNull();

        assertThat(actual.getTX()).isEqualTo(tx);
        assertThat(actual.getTY()).isEqualTo(ty);

        return myself;
    }

}
