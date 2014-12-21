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
package pixelitor.filters;

import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.levels.RGBLookup;
import pixelitor.filters.lookup.FastLookupOp;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;

/**
 * Posterize
 */
public class Posterize extends FilterWithParametrizedGUI {
    private final RangeParam levels = new RangeParam("Levels", 2, 255, 2);

    public Posterize() {
        super("Posterize", true, false);
        setParamSet(new ParamSet(levels));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int numLevels = levels.getValue();
        RGBLookup rgbLookup = new RGBLookup();
        rgbLookup.initFromPosterize(numLevels);

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
        filterOp.filter(src, dest);

        return dest;
    }

    @Override
    public boolean excludeFromAnimation() {
        return true;
    }
}