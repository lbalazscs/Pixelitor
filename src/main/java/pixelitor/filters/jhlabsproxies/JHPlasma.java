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

import com.jhlabs.image.PlasmaFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ActionSetting;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Plasma based on the JHLabs PlasmaFilter
 */
public class JHPlasma extends FilterWithParametrizedGUI {
    public static final String NAME = "Plasma";

    private final RangeParam turbulence = new RangeParam("Turbulence", 0, 100, 600);

    private static final int LESS_COLORS = 0;
    private static final int MORE_COLORS = 1;
    private static final int GRADIENT_COLORS = 2;

    private final IntChoiceParam type = new IntChoiceParam("Colors", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Less", LESS_COLORS),
            new IntChoiceParam.Value("More", MORE_COLORS),
            new IntChoiceParam.Value("Use Gradient", GRADIENT_COLORS),
    }, IGNORE_RANDOMIZE);

    private final ActionSetting reseedAction = new ReseedNoiseActionSetting(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (filter != null) {
                filter.randomize();
            }
        }
    });

    private PlasmaFilter filter;

    private final float[] defaultThumbPositions = {0.0f, 0.3f, 0.7f, 1.0f};
    private final Color[] defaultValues = {BLACK, RED, ORANGE, YELLOW};
    private final GradientParam gradient = new GradientParam("Gradient", defaultThumbPositions, defaultValues);


    public JHPlasma() {
        super(ShowOriginal.NO);

        setParamSet(new ParamSet(
                turbulence,
                type,
                gradient
        ).withAction(reseedAction));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        if (filter == null) {
            filter = new PlasmaFilter(NAME);
        }

        filter.setLessColors(type.getValue() != MORE_COLORS);
        filter.setTurbulence(turbulence.getValueAsPercentage());
        filter.setUseColormap(type.getValue() == GRADIENT_COLORS);
        filter.setColormap(gradient.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}