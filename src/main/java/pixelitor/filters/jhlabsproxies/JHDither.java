/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static com.jhlabs.image.DitherFilter.MATRIX_2x2;
import static com.jhlabs.image.DitherFilter.MATRIX_4x4_LINES;
import static com.jhlabs.image.DitherFilter.MATRIX_4x4_ORDERED;
import static com.jhlabs.image.DitherFilter.MATRIX_4x4_SQUARE;
import static com.jhlabs.image.DitherFilter.MATRIX_6x6_HALFTONE;
import static com.jhlabs.image.DitherFilter.MATRIX_6x6_ORDERED;
import static com.jhlabs.image.DitherFilter.MATRIX_8x8_ORDERED;
import static com.jhlabs.image.DitherFilter.MATRIX_CLUSTER3;
import static com.jhlabs.image.DitherFilter.MATRIX_CLUSTER4;
import static com.jhlabs.image.DitherFilter.MATRIX_CLUSTER8;

/**
 * Ordered dithering filter based on the JHLabs DitherFilter.
 */
public class JHDither extends ParametrizedFilter {
    public static final String NAME = "Ordered Dithering";

    @Serial
    private static final long serialVersionUID = 2507052624030584415L;

    private final RangeParam levels = new RangeParam("Levels", 2, 2, 8);
    private final BooleanParam colorDither = new BooleanParam("Color Dither", true);
    private final IntChoiceParam matrixMethod = new IntChoiceParam("Matrix Type", new Item[]{
        new Item("2x2", MATRIX_2x2),
        new Item("4x4 Square", MATRIX_4x4_SQUARE),
        new Item("4x4 Ordered", MATRIX_4x4_ORDERED),
        new Item("4x4 Lines", MATRIX_4x4_LINES),
        new Item("6x6 Halftone", MATRIX_6x6_HALFTONE),
        new Item("6x6 Ordered", MATRIX_6x6_ORDERED),
        new Item("8x8 Ordered", MATRIX_8x8_ORDERED),
        new Item("Cluster 3", MATRIX_CLUSTER3),
        new Item("Cluster 4", MATRIX_CLUSTER4),
        new Item("Cluster 8", MATRIX_CLUSTER8),
    });

    private DitherFilter filter;

    public JHDither() {
        super(true);

        setParams(
            levels,
            colorDither,
            matrixMethod
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new DitherFilter(NAME);
        }

        filter.setLevels(levels.getValue());
        filter.setColorDither(colorDither.isChecked());
        filter.setMatrixMethod(matrixMethod.getValue());

        filter.initialize();

        return filter.filter(src, dest);
    }
}