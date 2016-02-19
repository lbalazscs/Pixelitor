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
package pixelitor.filters.levels;

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.levels.gui.LevelsPanel;
import pixelitor.filters.lookup.FastLookupOp;
import pixelitor.layers.ImageLayer;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;
import java.util.Objects;
import java.util.Random;

/**
 * The Levels filter
 */
public class Levels2 extends FilterWithParametrizedGUI implements LookupFilter {
    private RGBLookup rgbLookup;
    private final LevelsModel model;

    public Levels2() {
        super(ShowOriginal.YES);
        model = new LevelsModel(this);
        setParamSet(model.getParamSet());
    }

    @Override
    public AdjustPanel createAdjustPanel(ImageLayer layer) {
        return new LevelsPanel(this, layer, model);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (rgbLookup == null) {
            throw new IllegalStateException("rgbLookup not initialized");
        }

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
        filterOp.filter(src, dest);

        return dest;
    }

    @Override
    public void setRGBLookup(RGBLookup rgbLookup) {
        this.rgbLookup = Objects.requireNonNull(rgbLookup);
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