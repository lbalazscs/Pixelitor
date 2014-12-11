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
    private RangeParam progressParam = new RangeParam("Progress (%)", 0, 100, 0);
    private IntChoiceParam type = new IntChoiceParam("Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Box IN", BricTransitionFilter.BOX_IN),
            new IntChoiceParam.Value("Box OUT", BricTransitionFilter.BOX_OUT),
            new IntChoiceParam.Value("Bars (Horizontal)", BricTransitionFilter.BARS_HORIZONTAL),
            new IntChoiceParam.Value("Bars (Vertical)", BricTransitionFilter.BARS_VERTICAL),
            new IntChoiceParam.Value("Checkerboard", BricTransitionFilter.CHECKERBOARD),
            new IntChoiceParam.Value("Circle IN", BricTransitionFilter.CIRCLE_IN),
            new IntChoiceParam.Value("Circle OUT", BricTransitionFilter.CIRCLE_OUT),
            new IntChoiceParam.Value("Collapse", BricTransitionFilter.COLLAPSE),
            new IntChoiceParam.Value("Curtain", BricTransitionFilter.CURTAIN),
            new IntChoiceParam.Value("Diamonds", BricTransitionFilter.DIAMONDS),
//            new IntChoiceParam.Value("Documentary", BricTransitionFilter.DOCUMENTARY),
            new IntChoiceParam.Value("Dots", BricTransitionFilter.DOTS),
            new IntChoiceParam.Value("Exploding Squares", BricTransitionFilter.SQUARES),
            new IntChoiceParam.Value("Fade", BricTransitionFilter.FADE),
            new IntChoiceParam.Value("Flurry", BricTransitionFilter.FLURRY),
            new IntChoiceParam.Value("Funky Wipe", BricTransitionFilter.FUNKY_WIPE),
            new IntChoiceParam.Value("Goo", BricTransitionFilter.GOO),
            new IntChoiceParam.Value("Halftone", BricTransitionFilter.HALFTONE),
            new IntChoiceParam.Value("Kaleidoscope", BricTransitionFilter.KALEIDOSCOPE),
            new IntChoiceParam.Value("Levitate", BricTransitionFilter.LEVITATE),
            new IntChoiceParam.Value("Microscope", BricTransitionFilter.MICROSCOPE),
            new IntChoiceParam.Value("Pivot", BricTransitionFilter.PIVOT),
            new IntChoiceParam.Value("Radial Wipe", BricTransitionFilter.RADIAL_WIPE),
            new IntChoiceParam.Value("Reveal", BricTransitionFilter.REVEAL),
            new IntChoiceParam.Value("Rotate", BricTransitionFilter.ROTATE),
            new IntChoiceParam.Value("Scale", BricTransitionFilter.SCALE),
            new IntChoiceParam.Value("Scribble", BricTransitionFilter.SCRIBBLE),
            new IntChoiceParam.Value("Scribble Twice", BricTransitionFilter.SCRIBBLE_TWICE),
            new IntChoiceParam.Value("Spiral", BricTransitionFilter.SPIRAL),
            new IntChoiceParam.Value("Spiral Sprawl", BricTransitionFilter.SPIRAL_SPRAWL),
            new IntChoiceParam.Value("Square Rain", BricTransitionFilter.SQUARE_RAIN),
            new IntChoiceParam.Value("Stars", BricTransitionFilter.STARS),
            new IntChoiceParam.Value("Toss In", BricTransitionFilter.TOSS_IN),
            new IntChoiceParam.Value("Venetian Blinds", BricTransitionFilter.BLINDS),
            new IntChoiceParam.Value("Wave", BricTransitionFilter.WAVE),
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