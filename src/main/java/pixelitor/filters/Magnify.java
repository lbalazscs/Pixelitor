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
package pixelitor.filters;

import pixelitor.filters.gui.CoupledRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.MagnifyFilter;

import java.awt.image.BufferedImage;

/**
 * Magnify filter
 */
public class Magnify extends FilterWithParametrizedGUI {
    private final RangeParam magnification = new RangeParam("Magnification (%)", 1, 500, 150);
    private final CoupledRangeParam outerRadius = new CoupledRangeParam("Outer Radius", 0, 999, 200);
    private final RangeParam outerInnerRadiusRatio = new RangeParam("Outer/Inner Radius Ratio (%)", 100, 999, 200);

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private MagnifyFilter filter;

    public Magnify() {
        super("Magnify", true, true);
        setParamSet(new ParamSet(
                magnification,
                outerRadius.adjustRangeAccordingToImage(1.0),
                outerInnerRadiusRatio,
                center,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new MagnifyFilter();
        }

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());

        int outerRadiusX = outerRadius.getFirstValue();
        int outerRadiusY = outerRadius.getSecondValue();
        filter.setOuterRadiusX(outerRadiusX);
        filter.setOuterRadiusY(outerRadiusY);

        float ratio = outerInnerRadiusRatio.getValueAsPercentage();
        int innerRadiusX = (int) (outerRadiusX / ratio);
        int innerRadiusY = (int) (outerRadiusY / ratio);
        filter.setInnerRadiusX(innerRadiusX);
        filter.setInnerRadiusY(innerRadiusY);

        filter.setMagnification(magnification.getValueAsPercentage());

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);

        setAffectedAreaShapes(filter.getAffectedAreaShapes());

        return dest;
    }
}

