/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.WaterFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Water Ripple filter based on the JHLabs WaterFilter
 */
public class JHWaterRipple extends ParametrizedFilter {
    public static final String NAME = "Water Ripple";

    @Serial
    private static final long serialVersionUID = -1049818778564803544L;

    private final ImagePositionParam center = new ImagePositionParam("Center");

    private final RangeParam radius = new RangeParam(GUIText.RADIUS, 1, 300, 999);
    private final RangeParam wavelength = new RangeParam("Wavelength", 1, 25, 250);
    private final RangeParam amplitude = new RangeParam("Amplitude", 0, 50, 100);
    private final RangeParam phase = new RangeParam("Phase (Time)", 0, 0, 360);

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private WaterFilter filter;

    public JHWaterRipple() {
        super(true);

        setParams(
            center,
            radius.withAdjustedRange(1.0),
            wavelength.withAdjustedRange(0.25),
            amplitude,
            phase,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (amplitude.isZero()) {
            return src;
        }

        if (filter == null) {
            filter = new WaterFilter(NAME);
        }

        filter.setCentre(center.getRelativePoint());
        filter.setRadius(radius.getValueAsFloat());
        filter.setWavelength(wavelength.getValueAsFloat());
        filter.setAmplitude((float) amplitude.getPercentage());
        filter.setPhase(phase.getValueInRadians());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
//        setAffectedAreaShapes(filter.getAffectedAreaShapes());
        return dest;
    }
}