/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.GridAdjustmentPanel;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.layers.Drawable;

import java.awt.image.BufferedImage;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.NO_TRANSPARENCY;

/**
 * Four Color Gradient filter based on the JHLabs FourColorFilter
 */
public class JHFourColorGradient extends ParametrizedFilter {
    public static final String NAME = "Four Color Gradient";

    private final ColorParam northWestParam = new ColorParam("Northwest", GREEN, NO_TRANSPARENCY);
    private final ColorParam northEastParam = new ColorParam("Northeast", ORANGE, NO_TRANSPARENCY);
    private final ColorParam southWestParam = new ColorParam("Southwest", BLUE, NO_TRANSPARENCY);
    private final ColorParam southEastParam = new ColorParam("Southeast", RED, NO_TRANSPARENCY);

    private FourColorFilter filter;

    public JHFourColorGradient() {
        super(ShowOriginal.NO);

        setParams(
                northWestParam,
                northEastParam,
                southWestParam,
                southEastParam
        );
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
    public FilterGUI createGUI(Drawable dr) {
        return new GridAdjustmentPanel(this, dr, true, ShowOriginal.NO);
    }
}