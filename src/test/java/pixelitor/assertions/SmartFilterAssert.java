/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.SmartFilter;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Custom AssertJ assertions for {@link ContentLayer} objects.
 */
public class SmartFilterAssert extends AdjustmentLayerAssert<SmartFilterAssert, SmartFilter> {
    public SmartFilterAssert(SmartFilter actual) {
        super(actual, SmartFilterAssert.class);
    }

    public SmartFilterAssert hasCachedImage(boolean expected) {
        isNotNull();

        assertThat(actual.hasCachedImage() == expected).isTrue();

        return this;
    }
}
