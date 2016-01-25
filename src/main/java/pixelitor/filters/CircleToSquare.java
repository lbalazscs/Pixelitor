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
import pixelitor.filters.impl.CircleToSquareFilter;

import java.awt.image.BufferedImage;

/**
 * "Circle to Square" filter
 */
public class CircleToSquare extends FilterWithParametrizedGUI {
    public static final String NAME = "Circle to Square";

    // private final GroupedRangeParam radius = new GroupedRangeParam("Radius", 0, 500, 200);
    private final RangeParam radius = new RangeParam("Radius", 0, 200, 500);

    private final RangeParam amount = new RangeParam("Amount (%)", -200, 100, 200);
    private final ImagePositionParam center = new ImagePositionParam("Center");

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private CircleToSquareFilter filter;

    public CircleToSquare() {
        super(ShowOriginal.YES);
        showAffectedArea();

        setParamSet(new ParamSet(
                center,
                radius.adjustRangeToImageSize(1.0),
                amount,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CircleToSquareFilter();
        }

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());

// ellipse
//        filter.setRadiusX(radius.getValue(0));
//        filter.setRadiusY(radius.getValue(1));

// circle
        filter.setRadiusX(radius.getValueAsFloat());
        filter.setRadiusY(radius.getValueAsFloat());

        filter.setAmount(amount.getValueAsPercentage());

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);

        setAffectedAreaShapes(filter.getAffectedAreaShapes());

        return dest;
    }
}

