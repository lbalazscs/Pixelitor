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
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.impl.TilesFilter;

import java.awt.image.BufferedImage;

/**
 * Glass Tiles filter
 */
public class GlassTiles extends FilterWithParametrizedGUI {
    private final CoupledRangeParam size = new CoupledRangeParam("Tile Size", 5, 500, 100);
    private final CoupledRangeParam curvature = new CoupledRangeParam("Curvature", 0, 20, 10);
//    private final RangeParam shiftXParam = new RangeParam("Shift Horizontal", 0, 10, 0);
//    private final RangeParam shiftYParam = new RangeParam("Shift Vertical", 0, 10, 0);
    private final CoupledRangeParam phase = new CoupledRangeParam("Shift Effect", 0, 10, 0, false);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private TilesFilter filter;

    public GlassTiles() {
        super("Glass Tiles", true, false);
        setParamSet(new ParamSet(
                size.adjustRangeAccordingToImage(0.5),
                curvature,
                phase,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new TilesFilter();
        }

        filter.setSizeX(size.getFirstValue());
        filter.setSizeY(size.getSecondValue());
        filter.setCurvatureX(curvature.getFirstValue());
        filter.setCurvatureY(curvature.getSecondValue());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        filter.setShiftX(phase.getFirstValueAsPercentage());
        filter.setShiftY(phase.getSecondValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}

