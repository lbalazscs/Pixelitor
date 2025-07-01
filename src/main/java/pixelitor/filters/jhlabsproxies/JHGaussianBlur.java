/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Gaussian Blur filter based on JHLabs GaussianFilter
 */
public class JHGaussianBlur extends ParametrizedFilter {
    public static final String NAME = "Gaussian Blur";

    @Serial
    private static final long serialVersionUID = 5650559334811606541L;

    private final RangeParam radius = new RangeParam(GUIText.RADIUS, 1, 2, 101);
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();

    private GaussianFilter filter;

    public JHGaussianBlur() {
        super(true);

        initParams(
            radius.withDecimalPlaces(1),
            hpSharpening
        );

        helpURL = "https://en.wikipedia.org/wiki/Gaussian_blur";
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new GaussianFilter(NAME);
        }

        filter.setRadius(radius.getValueAsFloat());
        filter.setPremultiplyAlpha(false);

        dest = ImageUtils.filterPremultiplied(src, dest, filter);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.toHighPassSharpenedImage(src, dest);
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return !hpSharpening.isChecked();
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}