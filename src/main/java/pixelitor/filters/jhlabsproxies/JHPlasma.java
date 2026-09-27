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

import com.jhlabs.image.PlasmaFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.GradientPreset;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;

import static java.awt.Color.*;

/**
 * Plasma filter based on the JHLabs {@link PlasmaFilter}.
 */
public class JHPlasma extends ParametrizedFilter {
    public static final String NAME = "Plasma";

    @Serial
    private static final long serialVersionUID = 5878378907746251031L;

    private final RangeParam turbulence = new RangeParam("Turbulence", 0, 100, 600);

    private static final int LESS_COLORS = 0;
    private static final int MORE_COLORS = 1;
    private static final int GRADIENT_COLORS = 2;

    private final IntChoiceParam type = new IntChoiceParam("Colors", new Item[]{
        new Item("Less", LESS_COLORS),
        new Item("More", MORE_COLORS),
        new Item("Use Gradient", GRADIENT_COLORS),
    });

    // initialize here, otherwise it doesn't load from pxc smart filter
    private final PlasmaFilter filter = new PlasmaFilter(NAME);

    private final float[] fourPoints = {0.0f, 0.3f, 0.7f, 1.0f};
    private final GradientPreset FIRE = new GradientPreset("Fire",
        fourPoints,
        new Color[]{
            BLACK,
            RED,
            ORANGE,
            YELLOW,
        });
    private final GradientPreset OCEAN = new GradientPreset("Ocean",
        fourPoints,
        new Color[]{
            new Color(8, 20, 40),
            new Color(7, 49, 67),
            new Color(60, 180, 180),
            new Color(190, 237, 227)
        });
    private final GradientPreset NEON = new GradientPreset("Neon",
        fourPoints,
        new Color[]{
            new Color(15, 10, 30),
            new Color(90, 20, 140),
            new Color(255, 80, 150),
            new Color(255, 200, 120)
        });

    private final GradientParam gradient = new GradientParam("Gradient",
        List.of(FIRE, OCEAN, NEON));

    public JHPlasma() {
        super(false);

        // enable the gradient selector only if the color type is "use gradient"
        type.enableOtherWhen(gradient, v -> v.hasValue(GRADIENT_COLORS));

        initParams(
            turbulence,
            type,
            gradient
        ).withAction(paramSet.createReseedAction(filter::setSeed));
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int colorType = type.getValue();

        filter.setUniformChannelVariation(colorType != MORE_COLORS); // also true for gradients
        filter.setTurbulence((float) turbulence.getPercentage());
        filter.setColormap(colorType == GRADIENT_COLORS ? gradient.getColorMap() : null);
        filter.setSeed(paramSet.getLastSeed());

        return filter.filter(src, dest);
    }
}
