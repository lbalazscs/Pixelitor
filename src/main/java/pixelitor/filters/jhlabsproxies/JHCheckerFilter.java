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

import com.jhlabs.image.CheckFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Checker filter based on the JHLabs CheckFilter
 */
public class JHCheckerFilter extends FilterWithParametrizedGUI {
    public static final String NAME = "Checker Pattern";

    private final GroupedRangeParam size = new GroupedRangeParam("Size", "Width", "Height", 1, 10, 100, true);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final ColorParam color1 = new ColorParam("Color 1:", Color.BLACK, NO_OPACITY);
    private final ColorParam color2 = new ColorParam("Color 1:", Color.WHITE, NO_OPACITY);
    private final RangeParam fuzziness = new RangeParam("Fuzziness", 0, 0, 50);
    private final BooleanParam bumpMap = new BooleanParam("Bump Map Original", false, IGNORE_RANDOMIZE);

//    private final RangeParam aaRes = new RangeParam("Anti-aliasing Quality", 1, 10, 2);

    private CheckFilter filter;

    public JHCheckerFilter() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                size.adjustRangeToImageSize(1.0),
                angle,
                color1,
                color2,
                fuzziness,
                bumpMap
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CheckFilter(NAME);
        }

//        filter.setAaRes(aaRes.getValue());

        filter.setFuzziness(fuzziness.getValue());
        filter.setAngle((float) angle.getValueInRadians());
        filter.setBackground(color1.getColor().getRGB());
        filter.setForeground(color2.getColor().getRGB());
        filter.setXScale(size.getValue(0));
        filter.setYScale(size.getValue(1));

        dest = filter.filter(src, dest);

        if (bumpMap.isChecked()) {
            return ImageUtils.bumpMap(src, dest, NAME);
        }

        return dest;
    }
}