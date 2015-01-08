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

import com.jhlabs.image.BrushedMetalFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ReseedSupport;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * BrushedMetal based on the JHLabs BrushedMetalFilter
 */
public class JHBrushedMetal extends FilterWithParametrizedGUI {
    private final ColorParam color = new ColorParam("Color", Color.GRAY, false, false);
    private final RangeParam radius = new RangeParam("Length", 0, 500, 100);
    private final RangeParam amount = new RangeParam("Amount (%)", 0, 100, 50);
    private final RangeParam shine = new RangeParam("Shine (%)", 0, 100, 10);

    private BrushedMetalFilter filter;

    public JHBrushedMetal() {
        super("Brushed Metal", false, false);
        setParamSet(new ParamSet(
                color,
                radius.adjustRangeToImageSize(0.5),
                amount,
                shine,
                ReseedSupport.createParam()
        ));
        listNamePrefix = "Render ";
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        filter = new BrushedMetalFilter(color.getColor().getRGB(),
                radius.getValue(),
                amount.getValueAsPercentage(),
                true,
                shine.getValueAsPercentage());

        filter.setRandom(rand);

        dest = filter.filter(src, dest);
        return dest;
    }
}