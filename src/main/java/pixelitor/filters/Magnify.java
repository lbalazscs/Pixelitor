/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.MagnifyFilter;
import pixelitor.utils.BlurredShape;

import java.awt.image.BufferedImage;

/**
 * Magnify filter
 */
public class Magnify extends ParametrizedFilter {
    public static final String NAME = "Magnify";

    private final RangeParam magnification = new RangeParam("Magnification (%)", 1, 150, 501);
    private final GroupedRangeParam outerRadius = new GroupedRangeParam("Radius", 0, 200, 999);
    private final RangeParam outerInnerRadiusRatio = new RangeParam("Softness", 0, 100, 1000);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam shape = BlurredShape.getChoices();
    private final BooleanParam invert = new BooleanParam("Invert", false);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private MagnifyFilter filter;

    public Magnify() {
        super(ShowOriginal.YES);

        showAffectedArea();

        setParams(
                magnification,
                outerRadius.withAdjustedRange(1.0),
                outerInnerRadiusRatio,
                center,
                shape,
                invert,
                edgeAction,
                interpolation
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new MagnifyFilter(NAME);
        }

        int outerRadiusX = outerRadius.getValue(0);
        int outerRadiusY = outerRadius.getValue(1);
        filter.setOuterRadiusX(outerRadiusX);
        filter.setOuterRadiusY(outerRadiusY);

        float ratio = outerInnerRadiusRatio.getValueAsPercentage() + 1.0f;
        int innerRadiusX = (int) (outerRadiusX / ratio);
        int innerRadiusY = (int) (outerRadiusY / ratio);
        filter.setInnerRadiusX(innerRadiusX);
        filter.setInnerRadiusY(innerRadiusY);

        filter.setMagnification(magnification.getValueAsPercentage());

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        filter.setRelCenter(center.getRelativeX(), center.getRelativeY());
        filter.setInvert(invert.isChecked());

        // called twice (also in CenteredTransformFilter),
        // but there is no better solution, because the center
        // has to be calculated before the shape
        filter.calcAbsoluteCenter(src);

        filter.setShape(shape.getValue());
        dest = filter.filter(src, dest);

        setAffectedAreaShapes(filter.getAffectedAreaShapes());

        return dest;
    }
}

