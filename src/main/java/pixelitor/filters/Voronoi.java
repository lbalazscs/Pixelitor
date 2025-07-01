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

import pixelitor.AppMode;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.impl.VoronoiFilter;
import pixelitor.utils.Metric;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

/**
 * Voronoi Diagram filter
 */
public class Voronoi extends ParametrizedFilter {
    public static final String NAME = "Voronoi Diagram";

    @Serial
    private static final long serialVersionUID = -104154369877253021L;

    private final RangeParam distance = new RangeParam("Distance between Points", 10, 20, 400);
    private final EnumParam<Metric> metric = new EnumParam<>("Distance", Metric.class);
    private final BooleanParam showPoints = new BooleanParam("Show Points", false, IGNORE_RANDOMIZE);
    private final BooleanParam useImageColors = new BooleanParam("Use Image Colors", false, IGNORE_RANDOMIZE);
    private final IntChoiceParam antiAliasing = new IntChoiceParam("Anti-aliasing", new Item[]{
        new Item("None (Faster)", 0),
        new Item("2x2 (Better, slower)", 2),
        new Item("4x4 (Best, slowest)", 4),
    }, IGNORE_RANDOMIZE);
    private final BooleanParam debugGrid = new BooleanParam("Debug Grid", false, IGNORE_RANDOMIZE);

    private VoronoiFilter filter;

    public Voronoi() {
        super(false);

        initParams(
            distance,
            metric,
            showPoints,
            useImageColors,
            antiAliasing
        ).withReseedAction();

        if (AppMode.isDevelopment()) {
            paramSet.addParams(new FilterParam[]{debugGrid});
        }

        helpURL = "https://en.wikipedia.org/wiki/Voronoi_diagram";
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new VoronoiFilter(NAME);
        }

        filter.setRand(paramSet.getLastSeedSRandom());
        filter.setDistanceBetweenPoints(distance.getValueAsDouble());
        filter.setMetric(metric.getSelected());
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

        if (debugGrid.isChecked()) {
            filter.debugGrid(dest);
        }

        return dest;
    }
}