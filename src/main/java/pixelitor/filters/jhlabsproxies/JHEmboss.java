/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.EmbossFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ElevationAngleParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;

import static pixelitor.utils.AngleUnit.CCW_DEGREES;

/**
 * Emboss filter based on the JHLabs EmbossFilter
 */
public class JHEmboss extends ParametrizedFilter {
    public static final String NAME = Texts.i18n("emboss");

    private final AngleParam lightDirection = new AngleParam(
        "Light Direction (Azimuth)", 0);
    private final ElevationAngleParam lightElevation = new ElevationAngleParam(
        "Light Elevation Angle", 30, CCW_DEGREES);
    private final RangeParam depth = new RangeParam(
        "Depth", 1, 7, 15);

    private final BooleanParam texture = new BooleanParam(
        "Texture (Multiply with the Source Image)", false);

    private EmbossFilter filter;

    public JHEmboss() {
        super(true);

        setParams(
            lightDirection,
            lightElevation,
            depth,
            texture
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new EmbossFilter(NAME);
        }

        filter.setAzimuth((float) lightDirection.getValueInIntuitiveRadians());

        float adjustedDepth = (float) (Math.pow(2, this.depth.getValue()) / 100.0);
        filter.setBumpHeight(adjustedDepth);

        filter.setElevation((float) lightElevation.getValueInIntuitiveRadians());
        filter.setEmboss(texture.isChecked());

        return filter.filter(src, dest);
    }
}