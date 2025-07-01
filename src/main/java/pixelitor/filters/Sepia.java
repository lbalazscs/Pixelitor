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

import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.SepiaFilter;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Sepia filter based on Daniel Wreczycki's sepia filter
 */
public class Sepia extends ParametrizedFilter {
    public static final String NAME = "Sepia";

    @Serial
    private static final long serialVersionUID = 7124387979100909841L;

    private final RangeParam intensity = new RangeParam("Intensity", 0, 20, 100);

    private SepiaFilter filter;

    public Sepia() {
        super(true);

        initParams(intensity);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new SepiaFilter(NAME);
        }

        filter.setIntensity(intensity.getValue());

        return filter.filter(src, dest);
    }
}