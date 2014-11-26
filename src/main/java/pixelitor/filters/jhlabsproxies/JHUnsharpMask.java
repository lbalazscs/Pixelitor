/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.UnsharpFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Unsharp Mask based on the JHLabs UnsharpFilter
 */
public class JHUnsharpMask extends FilterWithParametrizedGUI {

    private final RangeParam amount = new RangeParam("Amount", 1, 100, 50);
    private final RangeParam radius = new RangeParam("Radius", 0, 100, 2);
    private final RangeParam threshold = new RangeParam("Threshold", 0, 100, 0);

    private UnsharpFilter filter;

    public JHUnsharpMask() {
        super("Unsharp Mask", true, false);
        setParamSet(new ParamSet(
                amount,
                radius,
                threshold
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        if (filter == null) {
            filter = new UnsharpFilter();
        }

        filter.setAmount(amount.getValueAsPercentage());
        filter.setThreshold(threshold.getValue());
        filter.setRadius(radius.getValue());

        dest = filter.filter(src, dest);

        return dest;
    }
}