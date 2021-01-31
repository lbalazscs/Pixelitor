/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.ReduceNoiseFilter;
import pixelitor.filters.Filter;

import java.awt.image.BufferedImage;

/**
 * "Reduce Single Pixel Noise" filter based on the
 * JHLabs {@link ReduceNoiseFilter}
 */
public class JHReduceNoise extends Filter {
    public static final String NAME = "Reduce Single Pixel Noise";

    private final ReduceNoiseFilter filter;

    public JHReduceNoise() {
        filter = new ReduceNoiseFilter(NAME);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return filter.filter(src, dest);
    }
}