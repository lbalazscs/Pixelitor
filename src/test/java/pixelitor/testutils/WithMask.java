/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.history.AddToHistory;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;

public enum WithMask {
    YES {
        @Override
        public void init(Layer layer) {
            if (!layer.hasMask()) {
                layer.addMask(LayerMaskAddType.REVEAL_ALL);
            }
        }
    }, NO {
        @Override
        public void init(Layer layer) {
            if (layer.hasMask()) {
                layer.deleteMask(AddToHistory.NO);
            }
        }
    };

    public abstract void init(Layer layer);

    public void init(Composition comp) {
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            init(layer);
        }
    }

    public boolean isYes() {
        return this == YES;
    }
}
