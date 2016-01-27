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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.MagnifyFilter;

import java.awt.image.BufferedImage;

/**
 * Magnify filter
 */
public class Magnify extends FilterWithParametrizedGUI {
    public static final String NAME = "Magnify";

    private final RangeParam magnification = new RangeParam("Magnification (%)", 1, 150, 500);
    private final GroupedRangeParam outerRadius = new GroupedRangeParam("Outer Radius", 0, 200, 999);
    private final RangeParam outerInnerRadiusRatio = new RangeParam("Outer/Inner Radius Ratio (%)", 100, 200, 999);

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private MagnifyFilter filter;

    public Magnify() {
        super(ShowOriginal.YES);
        showAffectedArea();

        setParamSet(new ParamSet(
                magnification,
                outerRadius.adjustRangeToImageSize(1.0),
                outerInnerRadiusRatio,
                center,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new MagnifyFilter(NAME);
        }

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());

        int outerRadiusX = outerRadius.getValue(0);
        int outerRadiusY = outerRadius.getValue(1);
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

