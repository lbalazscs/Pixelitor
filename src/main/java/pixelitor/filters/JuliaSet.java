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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.JuliaSetFilter;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * The UI for {@link JuliaSetFilter}.
 */
public class JuliaSet extends EscapeTimeFractal {
    public static final String NAME = "Julia Set";

    @Serial
    private static final long serialVersionUID = -3089167245262580096L;

    private final GroupedRangeParam cParam = new GroupedRangeParam("Complex Constant (*100)",
        new RangeParam[]{
            new RangeParam("Re", -150, -70, 50),
            new RangeParam("Im", -150, 27, 50)
        }, false);

    public JuliaSet() {
        super(300, 0.22f);

        // insert right after Zoom, before Zoom Center, to match the base param order
        insertParam(cParam.notLinkable().withDecimalPlaces(2), 3);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Julia_set");
    }

    @Override
    public BufferedImage renderFractal(BufferedImage src, BufferedImage dest) {
        int iterations = iterationsParam.getValue();

        JuliaSetFilter filter = new JuliaSetFilter(
            createIterator(),
            zoomParam.getZoomRatio(),
            zoomCenterParam.getRelativeX(),
            zoomCenterParam.getRelativeY(),
            iterations,
            getColors(colorsParam.getValue(), iterations),
            cParam.getPercentage(0),
            cParam.getPercentage(1),
            insideOutParam.isChecked()
        );

        return filter.filter(src, dest);
    }
}
