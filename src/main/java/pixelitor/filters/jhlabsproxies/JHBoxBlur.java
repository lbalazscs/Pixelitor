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
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * JH Box Blur based on the JHLabs BoxBlurFilter
 */
public class JHBoxBlur extends FilterWithParametrizedGUI {

    private final GroupedRangeParam radius = new GroupedRangeParam("Radius", 0, 100, 0);

    private final RangeParam numberOfIterations = new RangeParam("Number of Iterations", 1, 10, 3);

    private final BooleanParam hpSharpening = BooleanParam.createParamForHPSharpening();

    private BoxBlurFilter filter;

    public JHBoxBlur() {
        super("Box Blur", true, false);
        setParamSet(new ParamSet(
                radius,
                numberOfIterations,
                hpSharpening
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float hRadius = radius.getValueAsFloat(0);
        float vRadius = radius.getValueAsFloat(1);
        if ((hRadius == 0) && (vRadius == 0)) {
            return src;
        }

        if (filter == null) {
            filter = new BoxBlurFilter();
        }

        filter.setHRadius(hRadius);
        filter.setVRadius(vRadius);
        filter.setIterations(numberOfIterations.getValue());
        filter.setPremultiplyAlpha(false);

        dest = filter.filter(src, dest);

        if (hpSharpening.getValue()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }
}