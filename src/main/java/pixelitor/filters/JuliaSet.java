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

import pixelitor.filters.gui.ComplexNumberParam;
import pixelitor.filters.gui.Help;
import pixelitor.filters.impl.JuliaSetFilter;
import pixelitor.filters.util.Complex;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * The UI for {@link JuliaSetFilter}.
 */
public class JuliaSet extends EscapeTimeFractal {
    public static final String NAME = "Julia Set";

    @Serial
    private static final long serialVersionUID = -3089167245262580096L;

    // iterationTypeParam is a superclass field, so it is already initialized here
    private final ComplexNumberParam cParam = new ComplexNumberParam("Complex Constant",
        new Complex(-0.7, 0.27),
        iterationTypeParam.getValue().bounds());

    public JuliaSet() {
        super(300, 0.22f);

        // ensure that the bounds for c are updated when the iteration type changes
        iterationTypeParam.updateOtherWhen(cParam,
            (c, type) -> c.updateBounds(type.bounds()));

        // insert right after Zoom, before Zoom Center, to match the base param order
        insertParam(cParam, 3);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Julia_set");
    }

    @Override
    public BufferedImage renderFractal(BufferedImage src, BufferedImage dest) {
        int iterations = iterationsParam.getValue();

        JuliaSetFilter filter = new JuliaSetFilter(
            iterationTypeParam.getValue().strategy(),
            zoomParam.getZoomRatio(),
            zoomCenterParam.getRelativePoint(),
            iterations,
            getColors(colorsParam.getValue(), iterations),
            cParam.getValue(),
            insideOutParam.isChecked()
        );

        return filter.filter(src, dest);
    }
}
