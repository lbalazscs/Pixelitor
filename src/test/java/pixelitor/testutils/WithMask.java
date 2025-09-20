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

package pixelitor.testutils;

import pixelitor.Composition;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;

/**
 * Whether a layer mask is added to a layer during tests.
 */
public enum WithMask {
    YES {
        @Override
        public void configure(Layer layer) {
            if (!layer.hasMask()) {
                layer.addMask(LayerMaskAddType.REVEAL_ALL, false);
            }
        }
    }, NO {
        @Override
        public void configure(Layer layer) {
            if (layer.hasMask()) {
                layer.deleteMask(false);
            }
        }
    };

    /**
     * Configures the given layer for testing according to the enum constant.
     */
    public abstract void configure(Layer layer);

    public void configure(Composition comp) {
        comp.forEachNestedLayer(this::configure, false);
    }

    public boolean isTrue() {
        return this == YES;
    }

    public boolean isFalse() {
        return this == NO;
    }
}
