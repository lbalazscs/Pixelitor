/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters;

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;
import pixelitor.filters.impl.RadialWavesFilter;

import java.awt.image.BufferedImage;

/**
 * Radial waves in a polar coordinate system
 */
public class RadialWaves extends FilterWithParametrizedGUI {
    private RangeParam angularDivisionParam = new RangeParam("Angular Division", 1, 100, 10);
    private RangeParam radialAmplitudeParam = new RangeParam("Radial Amplitude", 0, 100, 20);

    private RangeParam phaseParam = new RangeParam("Phase (time)", 0, 360, 0);

    private ImagePositionParam centerParam = new ImagePositionParam("Center");
    private RangeParam zoomParam = new RangeParam("Zoom (%)", 1, 500, 100);
    private final IntChoiceParam waveType = IntChoiceParam.getWaveTypeChoices();

    private IntChoiceParam edgeActionParam = IntChoiceParam.getEdgeActionChoices();
    private IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();

    private RadialWavesFilter filter;

    public RadialWaves() {
        super("Radial Waves", true, false);
        setParamSet(new ParamSet(
                centerParam,
                angularDivisionParam,
                radialAmplitudeParam.adjustRangeAccordingToImage(1.0),
                waveType,
                phaseParam,
                zoomParam,
                edgeActionParam,
                interpolationParam,
                new ReseedNoiseActionParam("Reseed Noise")
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new RadialWavesFilter();
        }

        filter.setCenterX(centerParam.getRelativeX());
        filter.setCenterY(centerParam.getRelativeY());
        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(interpolationParam.getValue());

        filter.setPhase(phaseParam.getValueAsPercentage());

        filter.setAngularDivision(angularDivisionParam.getValue());
        filter.setRadialAmplitude(radialAmplitudeParam.getValue());

        filter.setZoom(zoomParam.getValueAsPercentage());
        filter.setWaveType(waveType.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}