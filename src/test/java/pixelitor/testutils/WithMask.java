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

package pixelitor.testutils;

import pixelitor.Composition;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;

public enum WithMask {
    YES {
        @Override
        public void setupFor(Layer layer) {
            if (!layer.hasMask()) {
                layer.addMask(LayerMaskAddType.REVEAL_ALL);
            }
        }
    }, NO {
        @Override
        public void setupFor(Layer layer) {
            if (layer.hasMask()) {
                layer.deleteMask(false);
            }
        }
    };

    public abstract void setupFor(Layer layer);

    public void setupFor(Composition comp) {
        comp.forEachLayer(this::setupFor);
    }

    public boolean isYes() {
        return this == YES;
    }
}
