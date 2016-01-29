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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.DoGFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.Invert;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Difference of Gaussians based on the JHLabs DoGFilter
 */
public class JHDifferenceOfGaussians extends FilterWithParametrizedGUI {
    public static final String NAME = "Difference of Gaussians";

    private final RangeParam radius1 = new RangeParam("Radius 1", 0, 0, 10);
    private final RangeParam radius2 = new RangeParam("Radius 2", 0, 4, 10);
    private final BooleanParam normalize = new BooleanParam("Maximize Contrast", true);
    private final BooleanParam invert = new BooleanParam("Invert", false);

    private DoGFilter filter;

    public JHDifferenceOfGaussians() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                radius1.adjustRangeToImageSize(0.01),
                radius2.adjustRangeToImageSize(0.01),
                normalize,
                invert
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new DoGFilter(NAME);
        }

        if ((src.getWidth() == 1) || (src.getHeight() == 1)) {
            // BoxBlurFilter throws ArrayIndexOutOfBoundsException for such images

            // Give up. A workaround would be to use a filter that can use another
            // blurring algorithm, but it is not worth it, because this case is very
            // unlikely to occur.
            return src;
        }

        filter.setRadius1(radius1.getValueAsFloat());
        filter.setRadius2(radius2.getValueAsFloat());
        filter.setNormalize(normalize.isChecked());

        dest = filter.filter(src, dest);

        if (invert.isChecked()) {
            Invert.invertImage(dest, dest);
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}