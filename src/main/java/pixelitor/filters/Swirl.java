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

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.SwirlFilter;

import java.awt.image.BufferedImage;

/**
 * Swirl filter
 */
public class Swirl extends FilterWithParametrizedGUI {
    private final RangeParam amount = new RangeParam("Amount", -400, 400, 100);
    private final RangeParam radius = new RangeParam("Radius", 0, 1000, 300);
    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 500, 100);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private SwirlFilter filter;

    public Swirl() {
        super("Swirl", true, false);
        setParamSet(new ParamSet(
                amount,
                radius.adjustRangeToImageSize(1.0),
                center,
                zoom,
                rotateResult,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new SwirlFilter();
        }

        filter.setSwirlAmount(amount.getValueAsPercentage());
        filter.setRadius(radius.getValueAsFloat());
        filter.setZoom(zoom.getValueAsPercentage());
        filter.setRotateResultAngle((float) rotateResult.getValueInIntuitiveRadians());

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}