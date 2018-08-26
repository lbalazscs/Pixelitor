/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.OffsetFilter;
import com.jhlabs.image.TransformFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Offset filter based on the JHLabs OffsetFilter
 */
public class JHOffset extends ParametrizedFilter {
    public static final String NAME = "Offset";

    private final ImagePositionParam center =
            new ImagePositionParam("Translate Top Left Point To");

    private OffsetFilter filter;

    public JHOffset() {
        super(ShowOriginal.YES);

        setParams(center);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new OffsetFilter(NAME);
        }

        filter.setRelativeX(center.getRelativeX());
        filter.setRelativeY(center.getRelativeY());
        filter.setUseRelative(true);
        filter.setInterpolation(TransformFilter.NEAREST_NEIGHBOUR);

        dest = filter.filter(src, dest);
        return dest;
    }
}