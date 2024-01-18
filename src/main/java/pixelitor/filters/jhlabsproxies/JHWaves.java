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

import com.jhlabs.image.RippleFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Waves filter based on the JHLabs RippleFilter
 */
public class JHWaves extends ParametrizedFilter {
    public static final String NAME = "Waves";

    @Serial
    private static final long serialVersionUID = -3414927980090919384L;

    private final GroupedRangeParam wavelengthParam = new GroupedRangeParam("Wavelength", 1, 20, 200);
    private final GroupedRangeParam amplitudeParam = new GroupedRangeParam("Amplitude", 0, 10, 200);
    private final GroupedRangeParam phaseParam = new GroupedRangeParam("Phase (Time)", 0, 0, 100, false);
    private final AngleParam angleParam = new AngleParam("Angle", 0);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();
    private final IntChoiceParam waveType = IntChoiceParam.forWaveType();

    private RippleFilter filter;

    public JHWaves() {
        super(true);

        setParams(
            waveType.configureWaveType(paramSet),
            wavelengthParam.withAdjustedRange(0.2).withDecimalPlaces(1),
            amplitudeParam.withAdjustedRange(0.2),
            phaseParam,
            angleParam,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int xAmplitude = amplitudeParam.getValue(0);
        int yAmplitude = amplitudeParam.getValue(1);

        if (xAmplitude == 0 && yAmplitude == 0) {
            return src;
        }

        if (filter == null) {
            filter = new RippleFilter(NAME);
        }

        float xWavelength = wavelengthParam.getValueAsFloat(0);
        float yWavelength = wavelengthParam.getValueAsFloat(1);

        filter.setXAmplitude(xAmplitude);
        filter.setXWavelength(xWavelength);
        filter.setYAmplitude(yAmplitude);
        filter.setYWavelength(yWavelength);
        filter.setAngle(angleParam.getValueInIntuitiveRadians());
        filter.setWaveType(waveType.getValue());
        filter.setPhaseX(phaseParam.getPercentage(0));
        filter.setPhaseY(phaseParam.getPercentage(1));

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        return filter.filter(src, dest);
    }
}