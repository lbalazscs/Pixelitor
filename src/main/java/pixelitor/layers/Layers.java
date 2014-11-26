/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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

import pixelitor.AppLogic;
import pixelitor.Composition;

/**
 *
 */
public class Layers implements LayerChangeListener {
    private boolean activeLayerIsImageLayer = false;
    private static final Layers INSTANCE = new Layers();

    private Layers() {
    }

    public static void init() {
        AppLogic.addLayerChangeListener(INSTANCE);
    }

    @Override
    public void activeCompLayerCountChanged(Composition comp, int newLayerCount) {

    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        activeLayerIsImageLayer = (newActiveLayer instanceof ImageLayer);
    }

    @Override
    public void layerOrderChanged(Composition comp) {

    }

    public static boolean activeIsImageLayer() {
        return INSTANCE.activeLayerIsImageLayer;
    }
}
