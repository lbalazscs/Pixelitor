/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.FilterButtonModel;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.GridAdjustmentPanel;
import pixelitor.layers.Filterable;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.NO_TRANSPARENCY;

/**
 * Four Color Gradient filter based on the JHLabs FourColorFilter
 */
public class JHFourColorGradient extends ParametrizedFilter {
    public static final String NAME = "Four Color Gradient";

    private final ColorParam northWestParam =
        new ColorParam("Northwest", new Color(20, 128, 20), NO_TRANSPARENCY);
    private final ColorParam northEastParam =
        new ColorParam("Northeast", new Color(200, 200, 20), NO_TRANSPARENCY);
    private final ColorParam southWestParam =
        new ColorParam("Southwest", new Color(20, 20, 200), NO_TRANSPARENCY);
    private final ColorParam southEastParam =
        new ColorParam("Southeast", new Color(200, 20, 20), NO_TRANSPARENCY);

    private FourColorFilter filter;

    public JHFourColorGradient() {
        super(false);

        var darkenAll = new FilterButtonModel("Darker",
            this::darkenColors, "Darken all colors");
        var brightenAll = new FilterButtonModel("Brighter",
            this::brightenColors, "Brighten all colors");

        setParams(
            northWestParam,
            northEastParam,
            southWestParam,
            southEastParam
        ).withActionsAtFront(darkenAll, brightenAll);
    }

    private void darkenColors() {
        northWestParam.darker();
        northEastParam.darker();
        southWestParam.darker();
        southEastParam.darker();
    }

    private void brightenColors() {
        northWestParam.brighter();
        northEastParam.brighter();
        southWestParam.brighter();
        southEastParam.brighter();
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

        return filter.filter(src, dest);
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return new GridAdjustmentPanel(this, layer, true, false, reset);
    }
}