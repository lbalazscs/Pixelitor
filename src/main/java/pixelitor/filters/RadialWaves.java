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

package pixelitor.filters;

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.RadialWavesFilter;

import java.awt.image.BufferedImage;

import static pixelitor.gui.GUIText.ZOOM;

/**
 * Radial waves in a polar coordinate system
 */
public class RadialWaves extends ParametrizedFilter {
    public static final String NAME = "Radial Waves";

    private final RangeParam angularDivision = new RangeParam("Angular Division", 1, 10, 101);
    private final RangeParam radialAmplitude = new RangeParam("Radial Amplitude (Amount)", 0, 20, 100);
    private final RangeParam phase = new RangeParam("Phase (Time)", 0, 0, 360);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam zoom = new RangeParam(ZOOM + " (%)", 1, 100, 500);
    private final IntChoiceParam waveType = IntChoiceParam.forWaveType();
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private RadialWavesFilter filter;

    public RadialWaves() {
        super(true);

        setParams(
            waveType,
            center,
            angularDivision,
            radialAmplitude.withAdjustedRange(1.0),
            phase,
            zoom,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new RadialWavesFilter();
        }

        filter.setRelCenter(center.getRelativeX(), center.getRelativeY());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());
        filter.setPhase(phase.getPercentageValF());
        filter.setAngularDivision(angularDivision.getValue());
        filter.setRadialAmplitude(radialAmplitude.getValueAsDouble());
        filter.setZoom(zoom.getPercentageValF());
        filter.setWaveType(waveType.getValue());

        return filter.filter(src, dest);
    }
}