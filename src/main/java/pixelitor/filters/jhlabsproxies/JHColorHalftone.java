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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.ColorHalftoneFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * ColorHalftone based on the JHLabs ColorHalftoneFilter
 */
public class JHColorHalftone extends FilterWithParametrizedGUI {
    public static final String NAME = "Color Halftone";

    private final RangeParam dotRadius = new RangeParam("Dot Radius (pixel %)", 10, 100, 1000);

    private final AngleParam cyanScreenAngle = new AngleParam("Cyan Screen Angle", 1.8849555921538759);
    private final AngleParam magentaScreenAngle = new AngleParam("Magenta Screen Angle", 1.0821041362364843);
    private final AngleParam yellowScreenAngle = new AngleParam("Yellow Screen Angle", 1.5707963267948966);

    private ColorHalftoneFilter filter;

    public JHColorHalftone() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(dotRadius.adjustRangeToImageSize(1.2),
                cyanScreenAngle,
                magentaScreenAngle,
                yellowScreenAngle
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new ColorHalftoneFilter(NAME);
        }

        float cyan = (float) cyanScreenAngle.getValueInIntuitiveRadians();
        float magenta = (float) magentaScreenAngle.getValueInIntuitiveRadians();
        float yellow = (float) yellowScreenAngle.getValueInIntuitiveRadians();
        filter.setCyanScreenAngle(cyan);
        filter.setMagentaScreenAngle(magenta);
        filter.setYellowScreenAngle(yellow);
        filter.setdotRadius(dotRadius.getValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}