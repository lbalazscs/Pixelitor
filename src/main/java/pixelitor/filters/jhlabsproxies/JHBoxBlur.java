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

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Box Blur filter based on the JHLabs {@link BoxBlurFilter}.
 */
public class JHBoxBlur extends ParametrizedFilter {
    public static final String NAME = "Box Blur";

    @Serial
    private static final long serialVersionUID = -3687029298672175297L;

    private final GroupedRangeParam radius = new GroupedRangeParam(GUIText.RADIUS, 0, 0, 100);
    private final RangeParam numIterations = new RangeParam("Iterations (Quality)", 1, 3, 10);
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();

    public JHBoxBlur() {
        super(true);

        initParams(
            radius,
            numIterations,
            hpSharpening
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        float hRadius = radius.getValueAsFloat(0);
        float vRadius = radius.getValueAsFloat(1);

        if (hRadius == 0.0f && vRadius == 0.0f) {
            return src;
        }

        BoxBlurFilter filter = new BoxBlurFilter(NAME, hRadius, vRadius, numIterations.getValue());

        dest = filter.filter(src, dest);

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
