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

import com.jhlabs.image.RippleFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Waves filter based on the JHLabs {@link RippleFilter}.
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

    public JHWaves() {
        super(true);

        initParams(
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
        int xAmplitude = amplitudeParam.getHorizontal();
        int yAmplitude = amplitudeParam.getVertical();

        if (xAmplitude == 0 && yAmplitude == 0) {
            return src;
        }

        float xWavelength = wavelengthParam.getValueAsFloat(0);
        float yWavelength = wavelengthParam.getValueAsFloat(1);

        RippleFilter filter = new RippleFilter(
            NAME,
            edgeAction.getValue(),
            interpolation.getValue(),
            xAmplitude,
            xWavelength,
            yAmplitude,
            yWavelength,
            waveType.getValue(),
            phaseParam.getHorPercentage(),
            phaseParam.getVerPercentage()
        );

        filter.setAngle(angleParam.getValueInIntuitiveRadians());

        return filter.filter(src, dest);
    }
}
