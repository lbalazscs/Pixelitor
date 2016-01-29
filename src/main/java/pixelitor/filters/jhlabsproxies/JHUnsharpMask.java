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

import com.jhlabs.image.UnsharpFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Unsharp Mask based on the JHLabs UnsharpFilter
 */
public class JHUnsharpMask extends FilterWithParametrizedGUI {
    public static final String NAME = "Unsharp Mask";

    private final RangeParam amount = new RangeParam("Amount", 1, 50, 100);
    private final RangeParam radius = new RangeParam("Radius", 0, 2, 100);
    private final RangeParam threshold = new RangeParam("Threshold", 0, 0, 100);

    private UnsharpFilter filter;

    public JHUnsharpMask() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                amount,
                radius,
                threshold
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        if (filter == null) {
            filter = new UnsharpFilter(NAME);
        }

        filter.setAmount(amount.getValueAsPercentage());
        filter.setThreshold(threshold.getValue());
        filter.setRadius(radius.getValueAsFloat());

        dest = filter.filter(src, dest);

        return dest;
    }
}