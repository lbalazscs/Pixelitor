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

import com.jhlabs.image.GlowFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Glow based on the JHLabs GlowFilter
 */
public class JHGlow extends FilterWithParametrizedGUI {
    private final RangeParam amount = new RangeParam("Amount", 0, 100, 15);
    private final RangeParam softness = new RangeParam("Softness (Blur Radius)", 0, 100, 20);

    private GlowFilter filter;

    public JHGlow() {
        super("Glow", true, false);
        setParamSet(new ParamSet(
                amount,
                softness
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float amountValue = amount.getValueAsPercentage();
        if (amountValue == 0.0f) {
            return src;
        }

        if (filter == null) {
            filter = new GlowFilter();
        }

        filter.setAmount(amountValue);
        filter.setRadius(softness.getValueAsFloat());

        dest = filter.filter(src, dest);
        return dest;
    }

}