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

import com.jhlabs.image.DitherFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Dither based on the JHLabs DitherFilter
 */
public class JHDither extends FilterWithParametrizedGUI {
    public static final String NAME = "Dither";

    private final RangeParam levels = new RangeParam("Levels", 2, 8, 100);
    private final BooleanParam colorDither = new BooleanParam("Color Dither", true);
    private final IntChoiceParam matrixMethod = new IntChoiceParam("Matrix Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("2x2", DitherFilter.MATRIX_2x2),
            new IntChoiceParam.Value("4x4 Square", DitherFilter.MATRIX_4x4_SQUARE),
            new IntChoiceParam.Value("4x4 Ordered", DitherFilter.MATRIX_4x4_ORDERED),
            new IntChoiceParam.Value("4x4 Lines", DitherFilter.MATRIX_4x4_LINES),
            new IntChoiceParam.Value("6x6 Halftone", DitherFilter.MATRIX_6x6_HALFTONE),
            new IntChoiceParam.Value("6x6 Ordered", DitherFilter.MATRIX_6x6_ORDERED),
            new IntChoiceParam.Value("8x8 Ordered", DitherFilter.MATRIX_8x8_ORDERED),
            new IntChoiceParam.Value("Cluster 3", DitherFilter.MATRIX_CLUSTER3),
            new IntChoiceParam.Value("Cluster 4", DitherFilter.MATRIX_CLUSTER4),
            new IntChoiceParam.Value("Cluster 8", DitherFilter.MATRIX_CLUSTER8),
    });

    private DitherFilter filter;

    public JHDither() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                levels,
                colorDither,
                matrixMethod
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new DitherFilter(NAME);
        }

        filter.setLevels(levels.getValue());
        filter.setColorDither(colorDither.isChecked());
        filter.setMatrixMethod(matrixMethod.getValue());

        filter.initialize();

        dest = filter.filter(src, dest);
        return dest;
    }
}