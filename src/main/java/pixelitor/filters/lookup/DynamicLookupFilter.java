/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.filters.lookup;

import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.levels.GrayScaleLookup;
import pixelitor.filters.levels.RGBLookup;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;
import java.util.Objects;
import java.util.Random;

/**
 * A lookup filter that is initialized at runtime with an RGBLookup
 */
public abstract class DynamicLookupFilter extends FilterWithGUI {
    private RGBLookup rgbLookup;

    DynamicLookupFilter(String name) {
        super(name);
    }

    public void setRGBLookup(RGBLookup rgbLookup) {
        this.rgbLookup = Objects.requireNonNull(rgbLookup);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {

        if (rgbLookup == null) {
            throw new IllegalStateException("rgbLookup not initialized in DynamicLookupOp");
        }

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
        filterOp.filter(src, dest);

        return dest;
    }

    @Override
    public void randomizeSettings() {
        Random r = new Random();

        int inputBlackValue = r.nextInt(255);
        int inputWhiteValue = r.nextInt(255);
        int outputBlackValue = r.nextInt(255);
        int outputWhiteValue = r.nextInt(255);
        GrayScaleLookup g = new GrayScaleLookup(inputBlackValue, inputWhiteValue, outputBlackValue, outputWhiteValue);
        rgbLookup = new RGBLookup(g, g, g, g, g, g, g);
    }
}
