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


import com.jhlabs.image.GaussianFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Gaussian Blur based on JHLabs GaussianFilter
 */
public class JHGaussianBlur extends FilterWithParametrizedGUI {
    public static final String NAME = "Gaussian Blur";

    private final RangeParam radius = new RangeParam("Radius", 0, 2, 100);
    private final BooleanParam hpSharpening = BooleanParam.createParamForHPSharpening();

    private GaussianFilter filter;

    public JHGaussianBlur() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                radius,
                hpSharpening
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (radius.getValue() == 0) {
            return src;
        }

        if (filter == null) {
            filter = new GaussianFilter(NAME);
        }

        filter.setRadius(radius.getValueAsFloat());
        filter.setPremultiplyAlpha(false);

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }

    @Override
    protected boolean createDefaultDestBuffer() {
        return false;
    }

    public void setRadius(int newRadius) {
        radius.setValue(newRadius);
    }
}