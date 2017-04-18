/*
 * Copyright 2017 Laszlo Balazs-Csiki
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
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.EmptyPolarFilter;

import java.awt.image.BufferedImage;

/**
 * A template for transform filters with polar coordinates
 */
public class EmptyPolar extends FilterWithParametrizedGUI {
    public static final String NAME = "Empty Polar";

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam zoom = new RangeParam("Zoom (%)", 1, 100, 500);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private EmptyPolarFilter filter;

    public EmptyPolar() {
        super(ShowOriginal.YES);

        setParamSet(new ParamSet(
                center,
                zoom,
                rotateResult,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new EmptyPolarFilter();
        }

        filter.setRelCenter(center.getRelativeX(), center.getRelativeY());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());
        filter.setRotateResult((float) rotateResult.getValueInIntuitiveRadians());
        filter.setZoom(zoom.getValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}