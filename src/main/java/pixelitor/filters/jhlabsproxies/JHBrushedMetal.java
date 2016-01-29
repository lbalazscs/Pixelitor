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

import com.jhlabs.image.BrushedMetalFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ReseedSupport;

import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.Color.GRAY;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;

/**
 * BrushedMetal based on the JHLabs BrushedMetalFilter
 */
public class JHBrushedMetal extends FilterWithParametrizedGUI {
    public static final String NAME = "Brushed Metal";

    private final ColorParam color = new ColorParam("Color", GRAY, NO_OPACITY);
    private final RangeParam radius = new RangeParam("Length", 0, 100, 500);
    private final RangeParam amount = new RangeParam("Amount (%)", 0, 50, 100);
    private final RangeParam shine = new RangeParam("Shine (%)", 0, 10, 100);

    public JHBrushedMetal() {
        super(ShowOriginal.NO);

        setParamSet(new ParamSet(
                color,
                radius.adjustRangeToImageSize(0.5),
                amount,
                shine
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        BrushedMetalFilter filter = new BrushedMetalFilter(color.getColor().getRGB(),
                radius.getValue(),
                amount.getValueAsPercentage(),
                true,
                shine.getValueAsPercentage(),
                NAME);

        filter.setRandom(rand);

        dest = filter.filter(src, dest);
        return dest;
    }
}