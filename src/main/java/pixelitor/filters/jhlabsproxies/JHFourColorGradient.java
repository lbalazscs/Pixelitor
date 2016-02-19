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

import com.jhlabs.image.FourColorFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.GridAdjustmentPanel;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.layers.ImageLayer;

import java.awt.image.BufferedImage;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;

/**
 * Four Color Gradient based on the JHLabs FourColorFilter
 */
public class JHFourColorGradient extends FilterWithParametrizedGUI {
    public static final String NAME = "Four Color Gradient";

    private final ColorParam northWestParam = new ColorParam("Northwest", GREEN, NO_OPACITY);
    private final ColorParam northEastParam = new ColorParam("Northeast", ORANGE, NO_OPACITY);
    private final ColorParam southWestParam = new ColorParam("Southwest", BLUE, NO_OPACITY);
    private final ColorParam southEastParam = new ColorParam("Southeast", RED, NO_OPACITY);

    private FourColorFilter filter;

    public JHFourColorGradient() {
        super(ShowOriginal.NO);
        setParamSet(new ParamSet(
                northWestParam,
                northEastParam,
                southWestParam,
                southEastParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new FourColorFilter(NAME);
        }

        filter.setColorNW(northWestParam.getColor().getRGB());
        filter.setColorNE(northEastParam.getColor().getRGB());
        filter.setColorSW(southWestParam.getColor().getRGB());
        filter.setColorSE(southEastParam.getColor().getRGB());

        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public AdjustPanel createAdjustPanel(ImageLayer layer) {
        return new GridAdjustmentPanel(this, layer, true, ShowOriginal.NO);
    }
}