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

package pixelitor.filters;

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.RadialWavesFilter;

import java.awt.image.BufferedImage;

/**
 * Radial waves in a polar coordinate system
 */
public class RadialWaves extends FilterWithParametrizedGUI {
    public static final String NAME = "Radial Waves";

    private final RangeParam angularDivision = new RangeParam("Angular Division", 1, 10, 100);
    private final RangeParam radialAmplitude = new RangeParam("Radial Amplitude", 0, 20, 100);

    private final RangeParam phase = new RangeParam("Phase (time)", 0, 0, 360);

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 100, 500);
    private final IntChoiceParam waveType = IntChoiceParam.getWaveTypeChoices();

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private RadialWavesFilter filter;

    public RadialWaves() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                center,
                angularDivision,
                radialAmplitude.adjustRangeToImageSize(1.0),
                waveType,
                phase,
                zoom,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new RadialWavesFilter();
        }

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        filter.setPhase(phase.getValueAsPercentage());

        filter.setAngularDivision(angularDivision.getValue());
        filter.setRadialAmplitude(radialAmplitude.getValueAsDouble());

        filter.setZoom(zoom.getValueAsPercentage());
        filter.setWaveType(waveType.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}