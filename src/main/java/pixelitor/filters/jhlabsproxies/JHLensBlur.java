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

import com.jhlabs.image.LensBlurFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Lens Blur filter based on the JHLabs LensBlurFilter
 */
public class JHLensBlur extends ParametrizedFilter {
    public static final String NAME = "Lens Blur";

    @Serial
    private static final long serialVersionUID = 788524159732735827L;

    private final RangeParam amount = new RangeParam("Amount (Radius)", 1, 2, 101);
    private final RangeParam numSides = new RangeParam("Number of Sides of the Aperture", 3, 5, 12);
    private final RangeParam bloomFactor = new RangeParam("Bloom Factor", 1, 1, 8);
    private final RangeParam bloomThreshold = new RangeParam("Bloom Threshold", 0, 200, 255);

    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();

    private LensBlurFilter filter;

    public JHLensBlur() {
        super(true);

        initParams(
            amount.withDecimalPlaces(1),
            numSides,
            bloomFactor,
            bloomThreshold,
            hpSharpening
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new LensBlurFilter(NAME);
        }

        filter.setRadius(amount.getValueAsFloat());
        filter.setSides(numSides.getValue());
        filter.setBloom(bloomFactor.getValueAsFloat());
        filter.setBloomThreshold(bloomThreshold.getValueAsFloat());

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
}