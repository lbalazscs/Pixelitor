/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.impl;

import com.bric.image.transition.*;
import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static com.bric.image.transition.Transition.*;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A transition filter based on the com.bric.image.transition classes
 */
public class BricTransitionFilter extends AbstractBufferedImageOp {
    public static final int BARS_HORIZONTAL = 1;
    public static final int BARS_VERTICAL = 2;
    public static final int FADE = 3;
    public static final int BLINDS = 4;
    public static final int BOX_IN = 5;
    public static final int BOX_OUT = 6;
    public static final int CHECKERBOARD = 7;
    public static final int CIRCLE_IN = 8;
    public static final int CIRCLE_OUT = 9;
    public static final int COLLAPSE = 10;
    public static final int CURTAIN = 11;
    public static final int DIAMONDS = 12;
    //    public static final int DOCUMENTARY = 13;
    public static final int DOTS = 14;
    public static final int FLURRY = 15;
    public static final int FUNKY_WIPE = 16;
    public static final int GOO = 17;
    public static final int HALFTONE = 18;
    public static final int KALEIDOSCOPE = 19;
    public static final int LEVITATE = 20;
    public static final int MICROSCOPE = 21;
    public static final int PIVOT = 22;
    public static final int RADIAL_WIPE = 23;
    public static final int REVEAL = 24;
    public static final int ROTATE = 25;
    public static final int SCALE = 26;
    public static final int SCRIBBLE = 27;
    public static final int SCRIBBLE_TWICE = 28;
    public static final int SPIRAL = 29;
    public static final int SPIRAL_SPRAWL = 30;
    public static final int SQUARE_RAIN = 31;
    public static final int SQUARES = 32;
    public static final int STARS = 33;
    public static final int TOSS_IN = 34;
    public static final int WAVE = 35;

    private int type;
    private float progress;
    private boolean invert;

    // these have to be cached
    private GooTransition2D gooTransition2D;
    private SquaresTransition2D squaresTransition2D;

    public BricTransitionFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        var frameA = src;
        var frameB = ImageUtils.createImageWithSameCM(src);

        Graphics2D g2 = dest.createGraphics();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Transition transition = switch (type) {
            case BARS_HORIZONTAL -> new BarsTransition2D(HORIZONTAL, false);
            case BARS_VERTICAL -> new BarsTransition2D(VERTICAL, false);
            case FADE -> new BlendTransition2D();
            case BLINDS -> new BlindsTransition2D();
            case BOX_IN -> new BoxTransition2D(IN);
            case BOX_OUT -> new BoxTransition2D(OUT);
            case KALEIDOSCOPE -> new KaleidoscopeTransition2D();
            case CHECKERBOARD -> new CheckerboardTransition2D();
            case CIRCLE_IN -> new CircleTransition2D(IN);
            case CIRCLE_OUT -> new CircleTransition2D(OUT);
            case COLLAPSE -> new CollapseTransition2D();
            case CURTAIN -> new CurtainTransition2D();
            case DIAMONDS -> new DiamondsTransition2D(100, DiamondsTransition2D.TYPE_DIAMOND, 0);
            case DOTS -> new DotsTransition2D();
            case FLURRY -> new FlurryTransition2D(OUT);
            case FUNKY_WIPE -> new FunkyWipeTransition2D(true);
            case GOO -> createGooTransition();
            case HALFTONE -> new HalftoneTransition2D();
            case LEVITATE -> new LevitateTransition2D();
            case MICROSCOPE -> new MicroscopeTransition2D();
            case PIVOT -> new PivotTransition2D(BOTTOM_LEFT, false);
            case RADIAL_WIPE -> new RadialWipeTransition2D();
            case REVEAL -> new RevealTransition2D();
            case ROTATE -> new RotateTransition2D(OUT);
            case SCALE -> new ScaleTransition2D(OUT);
            case SCRIBBLE -> new ScribbleTransition2D(false);
            case SCRIBBLE_TWICE -> new ScribbleTransition2D(true);
            case SPIRAL -> new SpiralTransition2D(false);
            case SPIRAL_SPRAWL -> new SpiralTransition2D(true);
            case SQUARE_RAIN -> new SquareRainTransition2D();
            case SQUARES -> createSquareTransition();
            case STARS -> new StarsTransition2D();
            case TOSS_IN -> createTossInTransition();
            case WAVE -> new WaveTransition2D(RIGHT);
            default -> throw new IllegalStateException("Unexpected type = " + type);
        };

        if (progress < 0.0f) {
            progress = 0.0f;
        }
        if (progress > 1.0f) {
            progress = 1.0f;
        }

        transition.paint(g2, frameA, frameB, progress, invert);
        g2.dispose();

        return dest;
    }

    private Transition createTossInTransition() {
        Transition transition = new ReversedTransition(new TossTransition2D());
        progress = 1.0f - progress;
        return transition;
    }

    private Transition createGooTransition() {
        if (gooTransition2D == null) {
            gooTransition2D = new GooTransition2D();
        }
        return gooTransition2D;
    }

    private Transition createSquareTransition() {
        if (squaresTransition2D == null) {
            squaresTransition2D = new SquaresTransition2D();
        }
        return squaresTransition2D;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public void setType(int type) {
        this.type = type;
    }
}
