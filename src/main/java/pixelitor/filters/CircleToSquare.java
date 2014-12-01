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

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.CircleToSquareFilter;

import java.awt.image.BufferedImage;

/**
 * Circle To Square
 */
public class CircleToSquare extends FilterWithParametrizedGUI {
//    private final CoupledRangeParam radius = new CoupledRangeParam("Radius", 0, 500, 200);
    private final RangeParam radius = new RangeParam("Radius", 0, 500, 200);

    private final RangeParam amount = new RangeParam("Amount (%)", -200, 200, 100);


    private final ImagePositionParam center = new ImagePositionParam("Center");

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private CircleToSquareFilter filter;

    public CircleToSquare() {
        super("Circle to Square", true, true);
        setParamSet(new ParamSet(
                center,
                radius.adjustRangeAccordingToImage(1.0),
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

//        filter.setRadiusX(radius.getValue(0));
//        filter.setRadiusY(radius.getValue(1));

        filter.setRadiusX(radius.getValue());
        filter.setRadiusY(radius.getValue());


        filter.setAmount(amount.getValueAsPercentage());

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);

        setAffectedAreaShapes(filter.getAffectedAreaShapes());

        return dest;
    }
}

