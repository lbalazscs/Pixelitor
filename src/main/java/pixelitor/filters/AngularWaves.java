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

package pixelitor.filters;

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.AngularWavesFilter;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.gui.GUIText.ZOOM;

/**
 * Angular waves in a polar coordinate system
 */
public class AngularWaves extends ParametrizedFilter {
    public static final String NAME = "Angular Waves";

    @Serial
    private static final long serialVersionUID = 912678274938942074L;

    private final RangeParam radialWL = new RangeParam("Radial Wavelength", 1, 20, 100);
    private final RangeParam amount = new RangeParam("Angular Amount (Degrees)", 0, 20, 90);
    private final RangeParam phase = new RangeParam("Phase (Time)", 0, 0, 360);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam zoom = new RangeParam(ZOOM + " (%)", 1, 100, 500);
    private final IntChoiceParam waveType = IntChoiceParam.forWaveType();

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private AngularWavesFilter filter;

    public AngularWaves() {
        super(true);

        zoom.setPresetKey("Zoom (%)");

        setParams(
            waveType.configureWaveType(paramSet),
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
        if (filter == null) {
            filter = new AngularWavesFilter();
        }

        filter.setCenter(center.getAbsolutePoint(src));
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        filter.setPhase(phase.getPercentage());
        filter.setRadialWavelength(radialWL.getValueAsDouble());

        filter.setZoom(zoom.getPercentage());
        filter.setAmount(amount.getPercentage());
        filter.setWaveType(waveType.getValue());

        return filter.filter(src, dest);
    }
}