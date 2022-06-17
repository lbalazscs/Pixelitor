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

import com.jhlabs.image.PlasmaFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static java.awt.Color.*;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.filters.gui.ReseedActions.reseedByCalling;

/**
 * Plasma filter based on the JHLabs PlasmaFilter
 */
public class JHPlasma extends ParametrizedFilter {
    public static final String NAME = "Plasma";

    private final RangeParam turbulence = new RangeParam("Turbulence", 0, 100, 600);

    private static final int LESS_COLORS = 0;
    private static final int MORE_COLORS = 1;
    private static final int GRADIENT_COLORS = 2;

    private final IntChoiceParam type = new IntChoiceParam("Colors", new Item[]{
        new Item("Less", LESS_COLORS),
        new Item("More", MORE_COLORS),
        new Item("Use Gradient", GRADIENT_COLORS),
    }, ALLOW_RANDOMIZE);

    private PlasmaFilter filter;

    private final float[] defaultThumbPositions = {0.0f, 0.3f, 0.7f, 1.0f};
    private final Color[] defaultValues = {BLACK, RED, ORANGE, YELLOW};
    private final GradientParam gradient = new GradientParam("Gradient", defaultThumbPositions, defaultValues);

    public JHPlasma() {
        super(false);

        type.setupEnableOtherIf(gradient, v -> v.getValue() == GRADIENT_COLORS);

        setParams(
            turbulence,
            type,
            gradient
        ).withAction(reseedByCalling(() -> filter.randomize()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PlasmaFilter(NAME);
        }

        filter.setLessColors(type.getValue() != MORE_COLORS);
        filter.setTurbulence((float) turbulence.getPercentage());
        filter.setUseColormap(type.getValue() == GRADIENT_COLORS);
        filter.setColormap(gradient.getValue());

        return filter.filter(src, dest);
    }
}