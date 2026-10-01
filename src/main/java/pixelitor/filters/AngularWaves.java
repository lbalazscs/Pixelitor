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

package pixelitor.filters;

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.AngularWavesFilter;
import pixelitor.filters.impl.PolarMetric;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.gui.GUIText.ZOOM;

/**
 * Angular waves in a polar coordinate system.
 */
public class AngularWaves extends ParametrizedFilter {
    public static final String NAME = "Angular Waves";

    @Serial
    private static final long serialVersionUID = 912678274938942074L;

    private final IntChoiceParam waveType = IntChoiceParam.forWaveType();
    private final EnumParam<PolarMetric> metricType = PolarMetric.asParam();

    private final RangeParam radialWL = new RangeParam("Radial Wavelength", 1, 20, 100);
    private final RangeParam amount = new RangeParam("Angular Amount (Degrees)", 0, 20, 90);
    private final RangeParam phase = new RangeParam("Phase (Time)", 0, 0, 360);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam zoom = new RangeParam(ZOOM + " (%)", 1, 100, 500);

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    public AngularWaves() {
        super(true);

        zoom.setPresetKey("Zoom (%)");

        initParams(
            waveType.configureWaveType(paramSet),
            metricType,
            center,
            radialWL.withAdjustedRange(0.05).withDecimalPlaces(1),
            amount,
            phase,
            zoom,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        AngularWavesFilter filter = new AngularWavesFilter(NAME,
            edgeAction.getValue(),
            interpolation.getValue(),
            center.getAbsolutePoint(src),
            radialWL.getValueAsDouble(), // TODO should be scaled by 2π?
            phase.getPercentage(), // TODO should use phase.getValueInRadians()
            zoom.getPercentage(),
            amount.getPercentage(), // TODO should use amount.getValueInRadians()
            waveType.getValue(),
            metricType.getValue());

        return filter.filter(src, dest);
    }
}
