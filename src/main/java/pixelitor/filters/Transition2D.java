/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
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
package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.BricTransitionFilter;

import java.awt.image.BufferedImage;


/**
 *
 */
public class Transition2D extends FilterWithParametrizedGUI {
    private RangeParam progressParam = new RangeParam("Progress", 0, 100, 0);
    private IntChoiceParam type = new IntChoiceParam("Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Bars (Horizontal)", BricTransitionFilter.BARS_HORIZONTAL),
            new IntChoiceParam.Value("Bars (Horizontal, Random)", BricTransitionFilter.BARS_HORIZONTAL_RANDOM),
            new IntChoiceParam.Value("Bars (Vertical)", BricTransitionFilter.BARS_VERTICAL),
            new IntChoiceParam.Value("Bars (Vertical, Random)", BricTransitionFilter.BARS_VERTICAL_RANDOM),
            new IntChoiceParam.Value("Kaleidoscope", BricTransitionFilter.KALEIDOSCOPE),
    });

    private BricTransitionFilter filter;

    public Transition2D() {
        super("2D Transitions", true, false);
        setParamSet(new ParamSet(type, progressParam));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new BricTransitionFilter();
        }

        filter.setType(type.getValue());
        filter.setProgress(progressParam.getValueAsPercentage());

        dest = filter.filter(src, dest);

        return dest;
    }
}