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

package pixelitor.layers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * A utility class that helps to check that the image layer icon images
 * and the mask icon images are updated the correct number of times.
 */
public class IconUpdateChecker {
    private final Layer layer;
    private final LayerMask mask;
    private final LayerGUI ui;
    private final int layerIconUpdatesAtStart;
    private final int maskIconUpdatesAtStart;

    public IconUpdateChecker(LayerGUI ui, Layer layer, LayerMask mask, int layerIconUpdatesAtStart, int maskIconUpdatesAtStart) {
        this.ui = ui;
        this.layer = layer;
        this.mask = mask;
        this.layerIconUpdatesAtStart = layerIconUpdatesAtStart;
        this.maskIconUpdatesAtStart = maskIconUpdatesAtStart;

        // check that the start update counts are correct
        check(0, 0);
    }

    public void check(int numLayer, int numMask) {
        checkLayer(numLayer);
        checkMask(numMask);
    }

    private void checkLayer(int num) {
        if (layer instanceof ImageLayer) {
            verify(ui, times(layerIconUpdatesAtStart + num))
                    .updateLayerIconImage((ImageLayer) layer);
        }
    }

    private void checkMask(int num) {
        if (mask == null) {
            return;
        }
        verify(ui, times(maskIconUpdatesAtStart + num)).updateLayerIconImage(mask);
    }
}
