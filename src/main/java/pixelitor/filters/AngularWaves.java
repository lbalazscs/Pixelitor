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
import pixelitor.filters.impl.AngularWavesFilter;

import java.awt.image.BufferedImage;

/**
 * Angular waves in a polar coordinate system
 */
public class AngularWaves extends FilterWithParametrizedGUI {
    public static final String NAME = "Angular Waves";

    private final RangeParam radialWLParam = new RangeParam("Radial Wavelength", 1, 20, 100);
    private final RangeParam amountParam = new RangeParam("Angular Amount (Degrees)", 0, 20, 90);

    private final RangeParam phaseParam = new RangeParam("Phase (time)", 0, 0, 360);

    private final ImagePositionParam centerParam = new ImagePositionParam("Center");
    private final RangeParam zoomParam = new RangeParam("Zoom (%)", 1, 100, 500);
    private final IntChoiceParam waveType = IntChoiceParam.getWaveTypeChoices();

    private final IntChoiceParam edgeActionParam = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();

    private AngularWavesFilter filter;

    public AngularWaves() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                centerParam,
                radialWLParam.adjustRangeToImageSize(0.05),
                amountParam,
                waveType,
                phaseParam,
                zoomParam,
                edgeActionParam,
                interpolationParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new AngularWavesFilter();
        }

        filter.setCenterX(centerParam.getRelativeX());
        filter.setCenterY(centerParam.getRelativeY());
        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(interpolationParam.getValue());

        filter.setPhase(phaseParam.getValueAsPercentage());
        filter.setRadialWL(radialWLParam.getValueAsDouble());

        filter.setZoom(zoomParam.getValueAsPercentage());
        filter.setAmount(amountParam.getValueAsPercentage());
        filter.setWaveType(waveType.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}