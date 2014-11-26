/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.PolarFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Polar Coordinates based on the JHLabs PolarFilter
 */
public class JHPolarCoordinates extends FilterWithParametrizedGUI {
    private final ImagePositionParam center = new ImagePositionParam("Center");

    private static final IntChoiceParam.Value[] gridTypeChoices = {
            new IntChoiceParam.Value("Rectangular to Polar ", PolarFilter.RECT_TO_POLAR),
            new IntChoiceParam.Value("Polar to Rectangular", PolarFilter.POLAR_TO_RECT),
            new IntChoiceParam.Value("Invert in Circle", PolarFilter.INVERT_IN_CIRCLE),
    };
    private final IntChoiceParam type = new IntChoiceParam("Type", gridTypeChoices);
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();
    private RangeParam zoomParam = new RangeParam("Zoom (%)", 1, 500, 100);
    private AngleParam angleParam = new AngleParam("Angle", 0);

    private PolarFilter filter;

    public JHPolarCoordinates() {
        super("Polar Coordinates", true, false);
        setParamSet(new ParamSet(center, type, zoomParam, angleParam, edgeAction, interpolation));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PolarFilter();
        }
        filter.setType(type.getValue());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setRelativeCentreX(center.getRelativeX());
        filter.setRelativeCentreY(center.getRelativeY());
        filter.setInterpolation(interpolation.getValue());
        filter.setZoom(zoomParam.getValueAsPercentage());
        filter.setAngle(angleParam.getValueInIntuitiveRadians());

        dest = filter.filter(src, dest);
        return dest;
    }
}