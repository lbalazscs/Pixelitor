/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.levels.RGBLookup;
import pixelitor.filters.lookup.FastLookupOp;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;
import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * Posterize filter
 */
public class Posterize extends ParametrizedFilter {
    public static final String NAME = i18n("posterize");

    @Serial
    private static final long serialVersionUID = 4448706459360371642L;

    private final RangeParam redLevels = new RangeParam(i18n("red"), 2, 2, 50);
    private final RangeParam greenLevels = new RangeParam(i18n("green"), 2, 2, 50);
    private final RangeParam blueLevels = new RangeParam(i18n("blue"), 2, 2, 50);

    public Posterize() {
        super(true);

        var levels = new GroupedRangeParam("Levels",
            new RangeParam[]{
                redLevels,
                greenLevels,
                blueLevels
            }, true);

        setParams(levels);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int numRedLevels = redLevels.getValue();
        int numGreenLevels = greenLevels.getValue();
        int numBlueLevels = blueLevels.getValue();
        var rgbLookup = new RGBLookup();
        rgbLookup.initFromPosterize(numRedLevels, numGreenLevels, numBlueLevels);

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
        filterOp.filter(src, dest);
        return dest;
    }

    @Override
    public boolean excludedFromAnimation() {
        return true;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}