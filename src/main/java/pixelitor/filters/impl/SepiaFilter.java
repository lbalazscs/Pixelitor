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
package pixelitor.filters.impl;

import com.jhlabs.image.PointFilter;
import pixelitor.filters.Sepia;

/**
 * The implementation of the {@link Sepia} filter.
 *
 * Originally this was contributed by Daniel Wreczycki
 */
public class SepiaFilter extends PointFilter {
    private int intensity = 20;

    public SepiaFilter(String filterName) {
        super(filterName);
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        int lum = (r + g + b) / 3;
        r = g = b = lum;

        int depth = 20;
        r += (depth * 2);
        g += depth;

        if (r > 255) {
            r = 255;
        }
        if (g > 255) {
            g = 255;
        }

        b -= intensity;

        if (b < 0) {
            b = 0;
        }

        return a << 24 | r << 16 | g << 8 | b;
    }
}