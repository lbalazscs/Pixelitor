/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.ColorHalftoneFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.utils.AngleUnit.INTUITIVE_DEGREES;

/**
 * Color Halftone filter based on the JHLabs ColorHalftoneFilter
 */
public class JHColorHalftone extends ParametrizedFilter {
    public static final String NAME = "Color Halftone";

    @Serial
    private static final long serialVersionUID = 3114442166040830236L;

    private final RangeParam dotRadius = new RangeParam(
        "Dot Radius (pixel %)", 10, 100, 1000);
    private final AngleParam cyanScreenAngle = new AngleParam(
        "Cyan Screen Angle", 252, INTUITIVE_DEGREES);
    private final AngleParam magentaScreenAngle = new AngleParam(
        "Magenta Screen Angle", 298, INTUITIVE_DEGREES);
    private final AngleParam yellowScreenAngle = new AngleParam(
        "Yellow Screen Angle", 270, INTUITIVE_DEGREES);

    private ColorHalftoneFilter filter;

    public JHColorHalftone() {
        super(true);

        setParams(dotRadius.withAdjustedRange(1.2),
            cyanScreenAngle,
            magentaScreenAngle,
            yellowScreenAngle
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new ColorHalftoneFilter(NAME);
        }

        float cyan = (float) cyanScreenAngle.getValueInIntuitiveRadians();
        float magenta = (float) magentaScreenAngle.getValueInIntuitiveRadians();
        float yellow = (float) yellowScreenAngle.getValueInIntuitiveRadians();
        filter.setCyanScreenAngle(cyan);
        filter.setMagentaScreenAngle(magenta);
        filter.setYellowScreenAngle(yellow);
        filter.setdotRadius((float) dotRadius.getPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}