/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import com.jhlabs.image.FourColorFilter.ColorSpaceType;
import com.jhlabs.image.FourColorFilter.InterpolationType;
import com.jhlabs.image.FourColorRectFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.impl.FourColorAngularFilter;
import pixelitor.filters.impl.FourColorMetaballFilter;
import pixelitor.filters.impl.FourColorPolarFilter;
import pixelitor.filters.impl.FourColorTriangularFilter;
import pixelitor.filters.util.ColorSpace;
import pixelitor.gui.GUIText;
import pixelitor.layers.Filterable;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;

import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * The "Four Color Gradient" filter based on JHLabs {@link FourColorFilter} subclasses.
 */
public class JHFourColorGradient extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = -3344171912935129684L;

    public static final String NAME = "Four Color Gradient";

    private static final int TYPE_RECTANGULAR = 0;
    private static final int TYPE_ANGULAR = 1;
    private static final int TYPE_POLAR = 2;
    private static final int TYPE_METABALL = 3;
    private static final int TYPE_TRIANGULAR = 4;

    private final ColorParam northWestParam =
        new ColorParam("Northwest", new Color(20, 128, 20), MANUAL_ALPHA_ONLY);
    private final ColorParam northEastParam =
        new ColorParam("Northeast", new Color(200, 200, 20), MANUAL_ALPHA_ONLY);
    private final ColorParam southWestParam =
        new ColorParam("Southwest", new Color(20, 20, 200), MANUAL_ALPHA_ONLY);
    private final ColorParam southEastParam =
        new ColorParam("Southeast", new Color(200, 20, 20), MANUAL_ALPHA_ONLY);

    private final ImagePositionParam midpoint = new ImagePositionParam("Midpoint");

    private final EnumParam<InterpolationType> interpolation = new EnumParam<>(
        "Interpolation", InterpolationType.class);

    private final EnumParam<ColorSpaceType> space = new EnumParam<>(
        GUIText.COLOR_SPACE, ColorSpace.PRESET_KEY, ColorSpaceType.class);

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[]{
        new Item("Rectangular", TYPE_RECTANGULAR),
        new Item("Angular", TYPE_ANGULAR),
        new Item("Polar", TYPE_POLAR),
        new Item("Metaball", TYPE_METABALL),
        new Item("Triangular", TYPE_TRIANGULAR),
    });

    public JHFourColorGradient() {
        super(false);

        var darkenAll = new FilterButtonModel("Darker",
            this::darkenColors, "Darken all colors");
        var brightenAll = new FilterButtonModel("Brighter",
            this::brightenColors, "Brighten all colors");

        initParams(
            northWestParam,
            northEastParam,
            southWestParam,
            southEastParam,
            midpoint,
            type,
            interpolation,
            space
        ).withActionsAtFront(List.of(brightenAll, darkenAll));
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
        int colorNW = northWestParam.getColor().getRGB();
        int colorNE = northEastParam.getColor().getRGB();
        int colorSW = southWestParam.getColor().getRGB();
        int colorSE = southEastParam.getColor().getRGB();

        FourColorFilter filter = switch (type.getValue()) {
            case TYPE_RECTANGULAR -> new FourColorRectFilter(NAME,
                colorNW, colorNE, colorSW, colorSE,
                interpolation.getSelected(),
                space.getSelected(),
                midpoint.getRelativeX(), midpoint.getRelativeY(),
                src.getWidth(), src.getHeight());
            case TYPE_ANGULAR -> new FourColorAngularFilter(NAME,
                colorNW, colorNE, colorSW, colorSE,
                interpolation.getSelected(),
                space.getSelected(),
                midpoint.getRelativeX(), midpoint.getRelativeY(),
                src.getWidth(), src.getHeight());
            case TYPE_POLAR -> new FourColorPolarFilter(NAME,
                colorNW, colorNE, colorSW, colorSE,
                interpolation.getSelected(),
                space.getSelected(),
                midpoint.getRelativeX(), midpoint.getRelativeY(),
                src.getWidth(), src.getHeight());
            case TYPE_METABALL -> new FourColorMetaballFilter(NAME,
                colorNW, colorNE, colorSW, colorSE,
                interpolation.getSelected(),
                space.getSelected(),
                midpoint.getRelativeX(), midpoint.getRelativeY(),
                src.getWidth(), src.getHeight());
            case TYPE_TRIANGULAR -> new FourColorTriangularFilter(NAME,
                colorNW, colorNE, colorSW, colorSE,
                interpolation.getSelected(),
                space.getSelected(),
                midpoint.getRelativeX(), midpoint.getRelativeY(),
                src.getWidth(), src.getHeight());
            default -> throw new IllegalStateException("Unexpected value: " + type.getValue());
        };

        return filter.filter(src, dest);
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean resetSettings) {
        return new GridAdjustmentPanel(this, layer, true, false, resetSettings);
    }
}
