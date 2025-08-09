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

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.EqualizeFilter;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * The Equalize filter (histogram equalization).
 */
public class Equalize extends ParametrizedFilter {
    public static final String NAME = "Equalize";

    @Serial
    private static final long serialVersionUID = 1L;

    private final EnumParam<EqualizeFilter.Equalizer> channel
        = new EnumParam<>("Channel", EqualizeFilter.Equalizer.class);
    private final RangeParam details = new RangeParam("Brightness Levels", 1, 8, 10);

    public Equalize() {
        super(true);

        initParams(
            channel,
            details
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int numBins = 1 << details.getValue();
        EqualizeFilter filter = new EqualizeFilter(NAME, numBins, channel.getSelected());

        return filter.filter(src, dest);
    }
}