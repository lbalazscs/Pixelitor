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

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Box Blur based on the JHLabs BoxBlurFilter
 */
public class JHBoxBlur extends FilterWithParametrizedGUI {
    public static final String NAME = "Box Blur";

    private final GroupedRangeParam radius = new GroupedRangeParam("Radius", 0, 0, 100);
    private final RangeParam numberOfIterations = new RangeParam("Iterations (Quality)", 1, 3, 10);
    private final BooleanParam hpSharpening = BooleanParam.createParamForHPSharpening();

    private BoxBlurFilter filter;

    public JHBoxBlur() {
        super(ShowOriginal.YES);
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
        if ((src.getWidth() == 1) || (src.getHeight() == 1)) {
            // otherwise we get ArrayIndexOutOfBoundsException in BoxBlurFilter
            return src;
        }

        if (filter == null) {
            filter = new BoxBlurFilter(NAME);
        }

        filter.setHRadius(hRadius);
        filter.setVRadius(vRadius);
        filter.setIterations(numberOfIterations.getValue());
        filter.setPremultiplyAlpha(false);

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }
}