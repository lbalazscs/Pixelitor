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
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.TilesFilter;

import java.awt.image.BufferedImage;

/**
 * Glass Tiles filter
 */
public class GlassTiles extends FilterWithParametrizedGUI {
    public static final String NAME = "Glass Tiles";

    private final GroupedRangeParam size = new GroupedRangeParam("Tile Size", 5, 100, 500);
    private final GroupedRangeParam curvature = new GroupedRangeParam("Curvature", 0, 10, 20);
    private final GroupedRangeParam phase = new GroupedRangeParam("Shift Effect", 0, 0, 10, false);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices(true);
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private TilesFilter filter;

    public GlassTiles() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                size.adjustRangeToImageSize(0.5),
                curvature,
                phase.setShowLinkedCB(false),
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new TilesFilter(NAME);
        }

        filter.setSizeX(size.getValue(0));
        filter.setSizeY(size.getValue(1));
        filter.setCurvatureX(curvature.getValue(0));
        filter.setCurvatureY(curvature.getValue(1));
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        filter.setShiftX(phase.getValueAsPercentage(0));
        filter.setShiftY(phase.getValueAsPercentage(1));

        dest = filter.filter(src, dest);
        return dest;
    }
}

