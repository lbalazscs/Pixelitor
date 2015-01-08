/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import com.jhlabs.image.TritoneFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Tritone based on the JHLabs TritoneFilter
 */
public class JHTriTone extends FilterWithParametrizedGUI {
    private final ColorParam shadowColor = new ColorParam("Shadow Color:", Color.BLACK, false, false);
    private final ColorParam midtonesColor = new ColorParam("Midtones Color:", Color.RED, false, false);
    private final ColorParam highlightsColor = new ColorParam("Highlights Color:", Color.YELLOW, false, false);

    private TritoneFilter filter;

    public JHTriTone() {
        super("Tritone", true, false);
        setParamSet(new ParamSet(
                shadowColor, midtonesColor, highlightsColor
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new TritoneFilter();
        }

        filter.setShadowColor(shadowColor.getColor().getRGB());
        filter.setHighColor(highlightsColor.getColor().getRGB());
        filter.setMidColor(midtonesColor.getColor().getRGB());

        dest = filter.filter(src, dest);
        return dest;
    }
}