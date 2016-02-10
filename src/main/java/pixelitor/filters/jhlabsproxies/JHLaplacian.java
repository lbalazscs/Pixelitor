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

import com.jhlabs.image.LaplaceFilter;
import pixelitor.filters.Filter;

import java.awt.image.BufferedImage;

/**
 * Laplacian edge detection based on the JHLabs LaplaceFilter
 */
public class JHLaplacian extends Filter {
    public static final String NAME = "Laplacian";

    private final LaplaceFilter filter;

    public JHLaplacian() {
        filter = new LaplaceFilter(NAME);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public void randomizeSettings() {
        // nothing to randomize
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}