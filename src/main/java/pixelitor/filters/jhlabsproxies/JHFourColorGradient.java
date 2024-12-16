/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.layers.Filterable;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

/**
 * Four Color Gradient filter based on the JHLabs FourColorFilter
 */
public class JHFourColorGradient extends ParametrizedFilter {
    public static final String NAME = "Four Color Gradient";

    @Serial
    private static final long serialVersionUID = -3344171912935129684L;

    private final ColorParam northWestParam =
        new ColorParam("Northwest", new Color(20, 128, 20), USER_ONLY_TRANSPARENCY);
    private final ColorParam northEastParam =
        new ColorParam("Northeast", new Color(200, 200, 20), USER_ONLY_TRANSPARENCY);
    private final ColorParam southWestParam =
        new ColorParam("Southwest", new Color(20, 20, 200), USER_ONLY_TRANSPARENCY);
    private final ColorParam southEastParam =
        new ColorParam("Southeast", new Color(200, 20, 20), USER_ONLY_TRANSPARENCY);

    private final IntChoiceParam interpolation = new IntChoiceParam("Interpolation", new Item[]{
        new Item("Linear", FourColorFilter.INTERPOLATION_LINEAR),
        new Item("Cubic", FourColorFilter.INTERPOLATION_CUBIC),
        new Item("Quintic", FourColorFilter.INTERPOLATION_QUINTIC),
        new Item("Septic", FourColorFilter.INTERPOLATION_SEPTIC),
    });

    private final IntChoiceParam space = new IntChoiceParam("Color Space", new Item[]{
        new Item("Linear RGB", FourColorFilter.SPACE_LINEAR),
        new Item("sRGB", FourColorFilter.SPACE_SRGB),
    });

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
            southEastParam,
            interpolation,
            space
        ).withActionsAtFront(darkenAll, brightenAll);
    }

    private void darkenColors() {
        northWestParam.darken();
        northEastParam.darken();
        southWestParam.darken();
        southEastParam.darken();
    }

    private void brightenColors() {
        northWestParam.brighten();
        northEastParam.brighten();
        southWestParam.brighten();
        southEastParam.brighten();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new FourColorFilter(NAME);
        }

        filter.setColorNW(northWestParam.getColor().getRGB());
        filter.setColorNE(northEastParam.getColor().getRGB());
        filter.setColorSW(southWestParam.getColor().getRGB());
        filter.setColorSE(southEastParam.getColor().getRGB());
        filter.setInterpolation(interpolation.getValue());
        filter.setLinearSpace(space.getValue() == FourColorFilter.SPACE_LINEAR);

        return filter.filter(src, dest);
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return new GridAdjustmentPanel(this, layer, true, false, reset);
    }
}