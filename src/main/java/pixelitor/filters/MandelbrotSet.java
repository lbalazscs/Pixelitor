/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.Help;
import pixelitor.filters.impl.EscapeTimeFilter.IterationStrategy;
import pixelitor.filters.impl.MandelbrotSetFilter;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * The UI for {@link MandelbrotSetFilter}.
 */
public class MandelbrotSet extends EscapeTimeFractal {
    public static final String NAME = "Mandelbrot Set";

    @Serial
    private static final long serialVersionUID = 6726131928523590000L;

    public MandelbrotSet() {
        super(100, 0.2028f);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Mandelbrot_set");
    }

    @Override
    public BufferedImage renderFractal(BufferedImage src, BufferedImage dest) {
        IterationStrategy iterator = createIterator();
        Rectangle2D view = iterator.getComplexView();
        int iterations = iterationsParam.getValue();

        MandelbrotSetFilter filter = new MandelbrotSetFilter(
            view,
            iterator,
            zoomParam.getZoomRatio(),
            zoomCenterParam.getRelativeX(),
            zoomCenterParam.getRelativeY(),
            iterations,
            getColors(colorsParam.getValue(), iterations),
            insideOutParam.isChecked()
        );

        return filter.filter(src, dest);
    }
}
