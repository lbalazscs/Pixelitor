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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.VoronoiFilter;
import pixelitor.utils.Metric;
import pixelitor.utils.ReseedSupport;

import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Voronoi based on VoronoiFilter
 */
public class Voronoi extends FilterWithParametrizedGUI {
    public static final String NAME = "Voronoi Diagram";

    private final RangeParam numberOfPoints = new RangeParam("Number of Points", 1, 10, 200);
    private final EnumParam<Metric> distance = new EnumParam<>("Distance", Metric.class);
    private final BooleanParam showPoints = new BooleanParam("Show Points", false, IGNORE_RANDOMIZE);
    private final BooleanParam useImageColors = new BooleanParam("Use Image Colors", false, IGNORE_RANDOMIZE);
    private final IntChoiceParam antiAliasing = new IntChoiceParam("Anti-aliasing",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("None (Faster)", 0),
                    new IntChoiceParam.Value("2x2 (Better, slower)", 2),
                    new IntChoiceParam.Value("4x4 (Best, slowest)", 4),
            }, IGNORE_RANDOMIZE);

    private VoronoiFilter filter;

    public Voronoi() {
        super(ShowOriginal.NO);
        setParamSet(new ParamSet(
                numberOfPoints,
                distance,
                showPoints,
                useImageColors,
                antiAliasing
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new VoronoiFilter(NAME);
        }

        filter.setNumPoints(numberOfPoints.getValue());
        filter.setMetric(distance.getSelected());
        filter.setUseImageColors(useImageColors.isChecked());

        dest = filter.filter(src, dest);

        int aaRes = antiAliasing.getValue();

        if (aaRes != 0) {
            filter.setAaRes(aaRes);
            filter.antiAlias(dest);
        }

        if (showPoints.isChecked()) {
            filter.showPoints(dest);
        }

        return dest;
    }
}