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

package pixelitor.filters.levels;

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.levels.gui.LevelsGUI;
import pixelitor.filters.lookup.FastLookupOp;
import pixelitor.layers.Drawable;
import pixelitor.utils.RandomUtils;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;
import java.util.Objects;

/**
 * The Levels filter
 */
public class Levels extends FilterWithGUI {
    private RGBLookup rgbLookup;

    public Levels() {
    }

    @Override
    public FilterGUI createGUI(Drawable dr) {
        return new LevelsGUI(this, dr, new LevelsModel(this));
    }

    public void setRGBLookup(RGBLookup rgbLookup) {
        this.rgbLookup = Objects.requireNonNull(rgbLookup);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {

        if (rgbLookup == null) {
            throw new IllegalStateException("rgbLookup not initialized");
        }

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
        filterOp.filter(src, dest);

        return dest;
    }

    @Override
    public void randomizeSettings() {
        int inputBlackValue = RandomUtils.nextInt(255);
        int inputWhiteValue = RandomUtils.nextInt(255);
        int outputBlackValue = RandomUtils.nextInt(255);
        int outputWhiteValue = RandomUtils.nextInt(255);
        GrayScaleLookup g = new GrayScaleLookup(inputBlackValue, inputWhiteValue, outputBlackValue, outputWhiteValue);
        rgbLookup = new RGBLookup(g, g, g, g, g, g, g);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}