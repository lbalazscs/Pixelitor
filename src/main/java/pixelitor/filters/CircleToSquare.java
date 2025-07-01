/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.CircleToSquareFilter;
import pixelitor.gui.GUIText;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * "Circle to Square" filter
 */
public class CircleToSquare extends ParametrizedFilter {
    public static final String NAME = "Circle to Square";

    @Serial
    private static final long serialVersionUID = 1611532516529648926L;

    private final GroupedRangeParam radius = new GroupedRangeParam(GUIText.RADIUS, 0, 200, 500);
    private final RangeParam amount = new RangeParam("Amount (%)", -200, 100, 200);
    private final ImagePositionParam center = new ImagePositionParam("Center");

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private CircleToSquareFilter filter;

    public CircleToSquare() {
        super(true);

        initParams(
            center,
            radius.withAdjustedRange(1.0),
            amount,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CircleToSquareFilter();
        }

        filter.setCenter(center.getAbsolutePoint(src));
        filter.setRadiusX(radius.getValueAsFloat(0));
        filter.setRadiusY(radius.getValueAsFloat(1));
        filter.setAmount((float) amount.getPercentage());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
//        setAffectedAreaShapes(filter.getAffectedAreaShapes());
        return dest;
    }
}

