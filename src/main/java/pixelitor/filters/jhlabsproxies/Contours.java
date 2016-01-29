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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.EdgeFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.Invert;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.MorphologyFilter;
import pixelitor.filters.lookup.Luminosity;

import java.awt.image.BufferedImage;

/**
 * Contours filter
 */
public class Contours extends FilterWithParametrizedGUI {
    public static final String NAME = "Contours";

    private final RangeParam lineThickness = new RangeParam("Increase Line Thickness", 0, 0, 20);

    public Contours() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(lineThickness));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        EdgeFilter edgeFilter = new EdgeFilter(NAME);
        edgeFilter.setHEdgeMatrix(EdgeFilter.SOBEL_H);
        edgeFilter.setVEdgeMatrix(EdgeFilter.SOBEL_V);

        dest = edgeFilter.filter(src, dest);
        Invert.invertImage(dest, dest);

        Luminosity luminosity = new Luminosity();
        dest = luminosity.transform(dest, dest);

        int iterations = lineThickness.getValue();
        if (iterations > 0) {
            MorphologyFilter morphologyFilter = new MorphologyFilter(NAME);
            morphologyFilter.setIterations(iterations);
            morphologyFilter.setKernel(MorphologyFilter.KERNEL_DIAMOND);
            morphologyFilter.setOp(MorphologyFilter.OP_ERODE);

            dest = morphologyFilter.filter(dest, dest);
        }

        return dest;
    }
}