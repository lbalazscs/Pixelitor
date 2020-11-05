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
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.BricTransitionFilter;
import pixelitor.gui.GUIText;

import java.awt.image.BufferedImage;

import static pixelitor.filters.impl.BricTransitionFilter.*;


/**
 * 2D transitions
 */
public class Transition2D extends ParametrizedFilter {
    public static final String NAME = "2D Transitions";

    private final RangeParam progress = new RangeParam("Progress (%)", 0, 0, 100);
    private final IntChoiceParam type = new IntChoiceParam(GUIText.TYPE, new Item[]{
        new Item("Box In", BOX_IN),
        new Item("Box Out", BOX_OUT),
        new Item("Bars (Horizontal)", BARS_HORIZONTAL),
        new Item("Bars (Vertical)", BARS_VERTICAL),
        new Item("Checkerboard", CHECKERBOARD),
        new Item("Circle In", CIRCLE_IN),
        new Item("Circle Out", CIRCLE_OUT),
        new Item("Collapse", COLLAPSE),
        new Item("Curtain", CURTAIN),
        new Item("Diamonds", DIAMONDS),
//            new IntChoiceParam.Value("Documentary", DOCUMENTARY),
        new Item("Dots", DOTS),
        new Item("Exploding Squares", SQUARES),
        new Item("Fade", FADE),
        new Item("Flurry", FLURRY),
        new Item("Funky Wipe", FUNKY_WIPE),
        new Item("Goo", GOO),
        new Item("Halftone", HALFTONE),
        new Item("Kaleidoscope", KALEIDOSCOPE),
        new Item("Levitate", LEVITATE),
        new Item("Microscope", MICROSCOPE),
        new Item("Pivot", PIVOT),
        new Item("Radial Wipe", RADIAL_WIPE),
        new Item("Reveal", REVEAL),
        new Item("Rotate", ROTATE),
        new Item("Scale", SCALE),
        new Item("Scribble", SCRIBBLE),
        new Item("Scribble Twice", SCRIBBLE_TWICE),
        new Item("Spiral", SPIRAL),
        new Item("Spiral Sprawl", SPIRAL_SPRAWL),
        new Item("Square Rain", SQUARE_RAIN),
        new Item("Stars", STARS),
        new Item("Toss In", TOSS_IN),
        new Item("Venetian Blinds", BLINDS),
        new Item("Wave", WAVE),
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