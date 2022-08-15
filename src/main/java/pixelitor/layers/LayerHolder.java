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

package pixelitor.layers;

import pixelitor.Composition;

/**
 * Something that has an ordered collection of layers inside it.
 */
public interface LayerHolder {

    int getActiveLayerIndex();

    int getNumLayers();

    Layer getLayer(int index);

    Composition getComp();

    String getName();

    void moveActiveLayerUp();

    void moveActiveLayerDown();

    void deleteLayer(Layer layer, boolean addToHistory);

    boolean allowZeroLayers();
}
