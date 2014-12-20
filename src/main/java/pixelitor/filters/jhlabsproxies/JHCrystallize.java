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

import com.jhlabs.image.CrystallizeFilter;
import com.jhlabs.math.Noise;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ActionParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;
import pixelitor.utils.CachedFloatRandom;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Crystallize based on the JHLabs CrystallizeFilter
 */
public class JHCrystallize extends FilterWithParametrizedGUI {
    private final RangeParam edgeThickness = new RangeParam("Edge Thickness", 0, 100, 40);
    private final RangeParam size = new RangeParam("Size", 1, 200, 20);
    private final ColorParam edgeColor = new ColorParam("Edge Color", Color.BLACK, true, true);
    private final BooleanParam fadeEdges = new BooleanParam("Fade Edges", false);

    private final RangeParam randomness = new RangeParam("Shape Randomness (%)", 0, 100, 0);
    private final IntChoiceParam gridType = IntChoiceParam.getGridTypeChoices("Shape", randomness);

    private final ActionParam reseedAction = new ReseedNoiseActionParam(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            CachedFloatRandom.reseedCache();
            Noise.reseed();
        }
    });

    private CrystallizeFilter filter;

    public JHCrystallize() {
        super("Crystallize", true, false);
        setParamSet(new ParamSet(
                size.adjustRangeToImageSize(0.2),
                edgeThickness,
                gridType,
                randomness,
                edgeColor,
                fadeEdges,
                reseedAction
//                rndGen
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CrystallizeFilter();
        }

        filter.setEdgeThickness(edgeThickness.getValueAsPercentage());
        filter.setScale(size.getValueAsFloat());
        filter.setRandomness(randomness.getValueAsPercentage());
        filter.setEdgeColor(edgeColor.getColor().getRGB());
        filter.setGridType(gridType.getValue());
        filter.setFadeEdges(fadeEdges.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}