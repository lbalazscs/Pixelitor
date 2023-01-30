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
package pixelitor.layers;

import pixelitor.Composition;

/**
 * A listener for changes in layer number, selection and order within
 * the active {@link LayerHolder} of the active {@link Composition}.
 * It doesn't observe a specific {@link LayerHolder},but rather the active one.
 */
public interface ActiveHolderListener {
    void numLayersChanged(LayerHolder holder, int newLayerCount);

    void layerActivated(Layer layer);

    void layersReordered(LayerHolder holder);
}
