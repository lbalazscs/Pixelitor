/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.InvertAlphaFilter;
import pixelitor.filters.Filter;

import java.awt.image.BufferedImage;

/**
 * Invert Transparency filter based on
 * the JHLabs {@link InvertAlphaFilter}
 */
public class JHInvertTransparency extends Filter {
    public static final String NAME = "Invert Transparency";

    private final InvertAlphaFilter filter;

    public JHInvertTransparency() {
        filter = new InvertAlphaFilter(NAME);
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return filter.filter(src, dest);
    }
}