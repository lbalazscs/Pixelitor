/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import pixelitor.Composition;

/**
 * A listener interface for changes in layer number, selection and order.
 * Global in the sense that it does not observe a given
 * layer, but rather the active layer of the active composition
 */
public interface GlobalLayerChangeListener {

    void activeCompLayerCountChanged(Composition comp, int newLayerCount);

    void activeLayerChanged(Layer newActiveLayer);

    void layerOrderChanged(Composition comp);
}
