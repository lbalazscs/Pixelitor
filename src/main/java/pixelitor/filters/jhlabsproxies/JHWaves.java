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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.RippleFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Ripple based on the JHLabs RippleFilter
 */
public class JHWaves extends FilterWithParametrizedGUI {
    public static final String NAME = "Waves";

    private final GroupedRangeParam wavelengthParam = new GroupedRangeParam("Wavelength", 1, 20, 200);
    private final GroupedRangeParam amplitudeParam = new GroupedRangeParam("Amplitude", 0, 10, 200);
    private final GroupedRangeParam phaseParam = new GroupedRangeParam("Phase (Time)", 0, 0, 100, false);
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();
    private final IntChoiceParam waveType = IntChoiceParam.getWaveTypeChoices();

    private RippleFilter filter;

    public JHWaves() {
        super(ShowOriginal.YES);

        setParamSet(new ParamSet(
                wavelengthParam.adjustRangeToImageSize(0.2),
                amplitudeParam.adjustRangeToImageSize(0.2),
                waveType,
                phaseParam,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int xAmplitude = amplitudeParam.getValue(0);
        int yAmplitude = amplitudeParam.getValue(1);

        if (xAmplitude == 0 && yAmplitude == 0) {
            return src;
        }

        if (filter == null) {
            filter = new RippleFilter(NAME);
        }

        int xWavelength = wavelengthParam.getValue(0);
        int yWavelength = wavelengthParam.getValue(1);

        filter.setXAmplitude(xAmplitude);
        filter.setXWavelength(xWavelength);
        filter.setYAmplitude(yAmplitude);
        filter.setYWavelength(yWavelength);
        filter.setWaveType(waveType.getValue());
        filter.setPhaseX(phaseParam.getValueAsPercentage(0));
        filter.setPhaseY(phaseParam.getValueAsPercentage(1));

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}