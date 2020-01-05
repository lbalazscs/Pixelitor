/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.BricTransitionFilter;

import java.awt.image.BufferedImage;

import static pixelitor.filters.impl.BricTransitionFilter.*;


/**
 * 2D transitions
 */
public class Transition2D extends ParametrizedFilter {
    public static final String NAME = "2D Transitions";

    private final RangeParam progress = new RangeParam("Progress (%)", 0, 0, 100);
    private final IntChoiceParam type = new IntChoiceParam("Type", new Value[]{
            new Value("Box In", BOX_IN),
            new Value("Box Out", BOX_OUT),
            new Value("Bars (Horizontal)", BARS_HORIZONTAL),
            new Value("Bars (Vertical)", BARS_VERTICAL),
            new Value("Checkerboard", CHECKERBOARD),
            new Value("Circle In", CIRCLE_IN),
            new Value("Circle Out", CIRCLE_OUT),
            new Value("Collapse", COLLAPSE),
            new Value("Curtain", CURTAIN),
            new Value("Diamonds", DIAMONDS),
//            new IntChoiceParam.Value("Documentary", DOCUMENTARY),
            new Value("Dots", DOTS),
            new Value("Exploding Squares", SQUARES),
            new Value("Fade", FADE),
            new Value("Flurry", FLURRY),
            new Value("Funky Wipe", FUNKY_WIPE),
            new Value("Goo", GOO),
            new Value("Halftone", HALFTONE),
            new Value("Kaleidoscope", KALEIDOSCOPE),
            new Value("Levitate", LEVITATE),
            new Value("Microscope", MICROSCOPE),
            new Value("Pivot", PIVOT),
            new Value("Radial Wipe", RADIAL_WIPE),
            new Value("Reveal", REVEAL),
            new Value("Rotate", ROTATE),
            new Value("Scale", SCALE),
            new Value("Scribble", SCRIBBLE),
            new Value("Scribble Twice", SCRIBBLE_TWICE),
            new Value("Spiral", SPIRAL),
            new Value("Spiral Sprawl", SPIRAL_SPRAWL),
            new Value("Square Rain", SQUARE_RAIN),
            new Value("Stars", STARS),
            new Value("Toss In", TOSS_IN),
            new Value("Venetian Blinds", BLINDS),
            new Value("Wave", WAVE),
    });

    private BricTransitionFilter filter;

    public Transition2D() {
        super(ShowOriginal.YES);

        setParams(type, progress);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new BricTransitionFilter(NAME);
        }

        filter.setType(type.getValue());
        filter.setProgress(progress.getPercentageValF());

        dest = filter.filter(src, dest);

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}