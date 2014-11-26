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
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.OilFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Dry Brush based on the JHLabs OilFilter
 */
public class JHDryBrush extends FilterWithParametrizedGUI {
    private final RangeParam brushSize = new RangeParam("Brush Size", 0, 5, 0);
    private final RangeParam numberOfLevels = new RangeParam("Coarseness", 1, 255, 128);

    private OilFilter filter;

    public JHDryBrush() {
        super("Dry Brush", true, false);
        setParamSet(new ParamSet(
                brushSize, // takes forever for large images if this is scaled with the size of image
                numberOfLevels
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int range = brushSize.getValue();
        if (range == 0) {
            return src;
        }

        if (filter == null) {
            filter = new OilFilter();
        }

        filter.setRange(range);
        filter.setLevels(numberOfLevels.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}