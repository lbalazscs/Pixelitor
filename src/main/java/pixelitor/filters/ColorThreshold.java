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

package pixelitor.filters;

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Color Threshold filter
 */
public class ColorThreshold extends ParametrizedFilter {
    public static final String NAME = "Color Threshold";

    private final RangeParam redThreshold = new RangeParam("Red", 0, 128, 256);
    private final RangeParam greenThreshold = new RangeParam("Green", 0, 128, 256);
    private final RangeParam blueThreshold = new RangeParam("Blue", 0, 128, 256);

    public ColorThreshold() {
        super(ShowOriginal.YES);

        GroupedRangeParam threshold = new GroupedRangeParam("Threshold",
                new RangeParam[]{
                        redThreshold,
                        greenThreshold,
                        blueThreshold
                }, false);

        setParams(threshold);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        int redTh = redThreshold.getValue();
        int greenTh = greenThreshold.getValue();
        int blueTh = blueThreshold.getValue();

        int length = srcData.length;
        for (int i = 0; i < length; i++) {
            int srcPixel = srcData[i];
            int a = (srcPixel >>> 24) & 0xFF;
            int r = (srcPixel >>> 16) & 0xFF;
            int g = (srcPixel >>> 8) & 0xFF;
            int b = srcPixel & 0xFF;

            r = r >= redTh ? 0xFF : 0;
            g = g >= greenTh ? 0xFF : 0;
            b = b >= blueTh ? 0xFF : 0;

            destData[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}