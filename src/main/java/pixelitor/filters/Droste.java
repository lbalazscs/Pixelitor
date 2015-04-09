/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.filters.impl.DrosteFilter;

import java.awt.image.BufferedImage;

/**
 * Droste based on DrosteFilter
 */
public class Droste extends FilterWithParametrizedGUI {
    private RangeParam innerRadius = new RangeParam("Inner Radius", 1, 100, 25);
    private RangeParam outerRadius = new RangeParam("Outer Radius", 1, 100, 100);
    private RangeParam periodicity = new RangeParam("Periodicity", -6, 6, 1);
    private RangeParam strands = new RangeParam("Strands", -12, 12, 1);
    private RangeParam zoom = new RangeParam("Zoom", 1, 100, 1);
    private RangeParam rotate = new RangeParam("Rotate", 0, 100, 0);
    private RangeParam numberOfLevels = new RangeParam("Number of Levels", 0, 100, 0);
    private RangeParam startingLevel = new RangeParam("Starting Level", 0, 100, 0);
    private RangeParam fractalPoints = new RangeParam("Fractal Points", 1, 10, 1);
    private ImagePositionParam center = new ImagePositionParam("Center");
    private IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private DrosteFilter filter;

    public Droste() {
        super("Droste", true, false);
        setParamSet(new ParamSet(
                innerRadius,
                outerRadius,
                periodicity,
                strands,
                zoom,
                rotate,
                numberOfLevels,
                startingLevel,
                fractalPoints,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new DrosteFilter();
        }

        filter.setRadiusInside(innerRadius.getValueAsPercentage());
        filter.setRadiusOutside(outerRadius.getValueAsPercentage());
        filter.setPeriodicity(periodicity.getValue());
        filter.setStrands(strands.getValue());

//        filter.setCenterX(center.getRelativeX());
//        filter.setCenterY(center.getRelativeY());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}