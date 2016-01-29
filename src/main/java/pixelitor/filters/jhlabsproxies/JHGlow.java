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

import com.jhlabs.image.GlowFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Glow based on the JHLabs GlowFilter
 */
public class JHGlow extends FilterWithParametrizedGUI {
    public static final String NAME = "Glow";

    private final RangeParam amount = new RangeParam("Amount", 0, 15, 100);
    private final RangeParam softness = new RangeParam("Softness Radius", 0, 20, 100);

    private GlowFilter filter;

    public JHGlow() {
        super(ShowOriginal.YES);
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

        if ((src.getWidth() == 1) || (src.getHeight() == 1)) {
            // otherwise we get ArrayIndexOutOfBoundsException in BoxBlurFilter
            return src;
        }

        if (filter == null) {
            filter = new GlowFilter(NAME);
        }

        filter.setAmount(amountValue);
        filter.setRadius(softness.getValueAsFloat());

        dest = filter.filter(src, dest);
        return dest;
    }

}