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

import com.jhlabs.image.FourColorFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.GeographicalAdjustmentPanel;
import pixelitor.filters.gui.ParamSet;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Four Color Gradient based on the JHLabs FourColorFilter
 */
public class JHFourColorGradient extends FilterWithParametrizedGUI {
    private final ColorParam northWestParam = new ColorParam("Northwest", Color.GREEN, false, false);
    private final ColorParam northEastParam = new ColorParam("Northeast", Color.ORANGE, false, false);
    private final ColorParam southWestParam = new ColorParam("Southwest", Color.BLUE, false, false);
    private final ColorParam southEastParam = new ColorParam("Southeast", Color.RED, false, false);

    private FourColorFilter filter;

    public JHFourColorGradient() {
        super("Four Color Gradient", false, false);
        setParamSet(new ParamSet(
                northWestParam,
                northEastParam,
                southWestParam,
                southEastParam
        ));
        listNamePrefix = "Fill with ";
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new FourColorFilter();
        }

        filter.setColorNW(northWestParam.getColor().getRGB());
        filter.setColorNE(northEastParam.getColor().getRGB());
        filter.setColorSW(southWestParam.getColor().getRGB());
        filter.setColorSE(southEastParam.getColor().getRGB());

        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public AdjustPanel createAdjustPanel() {
        return new GeographicalAdjustmentPanel(this, true, false);
    }
}