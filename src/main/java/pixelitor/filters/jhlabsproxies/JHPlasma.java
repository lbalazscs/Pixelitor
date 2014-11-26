/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.PlasmaFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ActionParam;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Plasma based on the JHLabs PlasmaFilter
 */
public class JHPlasma extends FilterWithParametrizedGUI {
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 0, 600, 100);

    private static final int LESS_COLORS = 0;
    private static final int MORE_COLORS = 1;
    private static final int GRADIENT_COLORS = 2;

    private final IntChoiceParam typeParam = new IntChoiceParam("Colors", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Less", LESS_COLORS),
            new IntChoiceParam.Value("More", MORE_COLORS),
            new IntChoiceParam.Value("Use Gradient", GRADIENT_COLORS),
    }, true);

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionParam reseedAction = new ReseedNoiseActionParam(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (filter != null) {
                filter.randomize();
            }
        }
    });

    private PlasmaFilter filter;

    private final float[] defaultThumbPositions = {0.0f, 0.3f, 0.7f, 1.0f};
    private final Color[] defaultValues = {Color.BLACK, Color.RED, Color.ORANGE, Color.YELLOW};
    private final GradientParam gradientParam = new GradientParam("Gradient", defaultThumbPositions, defaultValues);


    public JHPlasma() {
        super("Plasma", false, false);
        setParamSet(new ParamSet(
                turbulenceParam,
                typeParam,
                gradientParam,
                reseedAction
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float turbulence = turbulenceParam.getValueAsPercentage();

        if (filter == null) {
            filter = new PlasmaFilter();
        }

        filter.setLessColors(typeParam.getValue() != MORE_COLORS);
        filter.setTurbulence(turbulence);

        filter.setUseColormap(typeParam.getValue() == GRADIENT_COLORS);
        filter.setColormap(gradientParam.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}