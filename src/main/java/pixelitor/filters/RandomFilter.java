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
package pixelitor.filters;

import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.ImageLayer;

import java.awt.image.BufferedImage;

/**
 * A filter that delegates the filtering to a randomly chosen filter
 */
public class RandomFilter extends FilterWithGUI {
    @Override
    public AdjustPanel createAdjustPanel(ImageLayer layer) {
        return new RandomFilterAdjustPanel(layer);
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        /**
         * The real work is done by other filters
         */
        throw new UnsupportedOperationException("this should not be called");
    }

    @Override
    public void randomizeSettings() {
        throw new UnsupportedOperationException("this should not be called");
    }
}
