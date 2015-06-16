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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.VoronoiFilter;
import pixelitor.utils.Metric;
import pixelitor.utils.ReseedSupport;

import java.awt.image.BufferedImage;

/**
 * Voronoi based on VoronoiFilter
 */
public class Voronoi extends FilterWithParametrizedGUI {
    private RangeParam numberOfPoints = new RangeParam("Number of Points", 1, 100, 10);
    private EnumParam<Metric> distance = new EnumParam<>("Distance", Metric.class);
    private BooleanParam showPoints = new BooleanParam("Show Points", false, true);
    private BooleanParam useImageColors = new BooleanParam("Use Image Colors", false, true);

    private VoronoiFilter filter;

    public Voronoi() {
        super("Voronoi Diagram", true, false);
        setParamSet(new ParamSet(
                numberOfPoints,
                distance,
                showPoints,
                useImageColors
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new VoronoiFilter();
        }

        filter.setNumPoints(numberOfPoints.getValue());
        filter.setMetric((Metric) distance.getSelectedItem());
        filter.setShowPoints(showPoints.isChecked());
        filter.setUseImageColors(useImageColors.isChecked());

        dest = filter.filter(src, dest);
        return dest;
    }
}