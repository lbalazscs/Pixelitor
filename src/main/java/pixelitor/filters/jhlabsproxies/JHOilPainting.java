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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.OilFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Oil Painting filter based on the JHLabs {@link OilFilter}.
 */
public class JHOilPainting extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = -9168810692489222200L;

    public static final String NAME = Texts.i18n("oil_painting");

    private final GroupedRangeParam brushSize = new GroupedRangeParam(
        "Brush Size", 0, 1, 10, true);
    private final RangeParam coarseness = new RangeParam(
        "Coarseness", 0, 25, 200);

    public JHOilPainting() {
        super(true);

        initParams(
            brushSize.withAdjustedRange(0.04),
            coarseness
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int brushX = brushSize.getHorizontal();
        int brushY = brushSize.getVertical();
        if (brushX == 0 && brushY == 0) {
            return src;
        }

        OilFilter filter = new OilFilter(NAME,
            brushX, brushY,
            coarseness.getValue() + 1);

        dest = filter.filter(src, dest);

        return dest;
    }

    @Override
    public boolean isAnimatable() {
        return false;
    }
}
