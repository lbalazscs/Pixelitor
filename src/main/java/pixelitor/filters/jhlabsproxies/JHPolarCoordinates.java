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

import com.jhlabs.image.PolarFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Polar Coordinates based on the JHLabs PolarFilter
 */
public class JHPolarCoordinates extends FilterWithParametrizedGUI {
    public static final String NAME = "Polar Coordinates";

    private final ImagePositionParam center = new ImagePositionParam("Center");

    private static final IntChoiceParam.Value[] gridTypeChoices = {
            new IntChoiceParam.Value("Rectangular to Polar ", PolarFilter.RECT_TO_POLAR),
            new IntChoiceParam.Value("Polar to Rectangular", PolarFilter.POLAR_TO_RECT),
            new IntChoiceParam.Value("Invert in Circle", PolarFilter.INVERT_IN_CIRCLE),
    };
    private final IntChoiceParam type = new IntChoiceParam("Type", gridTypeChoices);
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();
    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 100, 500);
    private final AngleParam angle = new AngleParam("Angle", 0);

    private PolarFilter filter;

    public JHPolarCoordinates() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(center, type, zoom, angle, edgeAction, interpolation));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PolarFilter(NAME);
        }
        filter.setType(type.getValue());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setRelativeCentreX(center.getRelativeX());
        filter.setRelativeCentreY(center.getRelativeY());
        filter.setInterpolation(interpolation.getValue());
        filter.setZoom(zoom.getValueAsPercentage());
        filter.setAngle(angle.getValueInIntuitiveRadians());

        dest = filter.filter(src, dest);
        return dest;
    }
}