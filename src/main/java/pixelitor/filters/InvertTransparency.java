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

package pixelitor.filters;

import com.jhlabs.image.InvertAlphaFilter;

import java.awt.image.BufferedImage;

/**
 * Invert Transparency
 */
public class InvertTransparency extends Filter {
    public static final String NAME = "Invert Transparency";

    private final InvertAlphaFilter filter;

    public InvertTransparency() {
        filter = new InvertAlphaFilter(NAME);
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return filter.filter(src, dest);
    }

    @Override
    public void randomizeSettings() {

    }
}