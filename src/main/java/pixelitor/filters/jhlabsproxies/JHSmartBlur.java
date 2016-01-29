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

import com.jhlabs.image.SmartBlurFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Smart Blur based on the JHLabs SmartBlurFilter
 */
public class JHSmartBlur extends FilterWithParametrizedGUI {
    public static final String NAME = "Smart Blur";

    private final RangeParam radiusParam = new RangeParam("Radius", 0, 0, 100);
    private final RangeParam threshold = new RangeParam("Threshold", 0, 50, 255);
    private final BooleanParam hpSharpening = BooleanParam.createParamForHPSharpening();

    private SmartBlurFilter filter;

    public JHSmartBlur() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                radiusParam,
                threshold,
                hpSharpening
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int horizontalValue = radiusParam.getValue();
        if (horizontalValue == 0) {
            return src;
        }

        if (filter == null) {
            filter = new SmartBlurFilter(NAME);
        }

        filter.setRadius(horizontalValue);
        filter.setThreshold(threshold.getValue());

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }

    @Override
    public boolean excludeFromAnimation() {
        return true;
    }
}