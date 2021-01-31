/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.GlintFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static java.awt.Color.WHITE;

/**
 * Glint filter based on the JHLabs GlintFilter
 */
public class JHGlint extends ParametrizedFilter {
    public static final String NAME = "Glint";

    private final RangeParam threshold = new RangeParam("Threshold (%)", 0, 70, 100);
    private final RangeParam coverage = new RangeParam("Coverage (%)", 0, 50, 100);
    private final RangeParam intensity = new RangeParam("Intensity (%)", 0, 15, 100);

    private final RangeParam lengthParam = new RangeParam("Length", 0, 20, 100);
    private final RangeParam blur = new RangeParam("Blur", 0, 1, 20);
//    private BooleanParam glintOnly = new BooleanParam("Glint Only", false);

    private final GradientParam colors = new GradientParam("Colors",
        new float[]{0.0f, 0.5f, 1.0f},
        new Color[]{WHITE, WHITE, WHITE});

    private GlintFilter filter;

    public JHGlint() {
        super(true);

        setParams(
            threshold,
            coverage,
            intensity,
            lengthParam, // slow for large images if it's adjusted to the image size
            blur,
            colors
//                glintOnly
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int length = lengthParam.getValue();
        if (length == 0) {
            // mot just for performance, a 0 length would cause division by 0
            return src;
        }

        if (filter == null) {
            filter = new GlintFilter(NAME);
        }

        filter.setThreshold(threshold.getPercentageValF());
        filter.setCoverage(coverage.getPercentageValF());
        filter.setAmount(intensity.getPercentageValF());
        filter.setLength(length);
        filter.setBlur(blur.getValueAsFloat());
        filter.setColormap(colors.getValue());

        return filter.filter(src, dest);
    }
}