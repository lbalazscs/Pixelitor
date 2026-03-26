/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Smart Blur filter based on the JHLabs {@link SmartBlurFilter}.
 */
public class JHSmartBlur extends ParametrizedFilter {
    public static final String NAME = "Smart Blur";

    @Serial
    private static final long serialVersionUID = 1462572105604633438L;

    private final RangeParam radiusParam = new RangeParam(GUIText.RADIUS, 0, 5, 100);
    private final RangeParam threshold = new RangeParam("Threshold", 0, 10, 256);
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();

    private SmartBlurFilter filter;

    public JHSmartBlur() {
        super(true);

        initParams(
            radiusParam,
            threshold,
            hpSharpening
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int radius = radiusParam.getValue();
        if (radius == 0) {
            return src;
        }

        if (filter == null) {
            filter = new SmartBlurFilter(NAME);
        }

        filter.setRadius(radius);

        filter.setThreshold(threshold.getValue());

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
    public boolean isAnimatable() {
        return false;
    }
}
