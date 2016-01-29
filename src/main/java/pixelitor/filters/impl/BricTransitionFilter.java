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

package pixelitor.filters.impl;

import com.bric.image.transition.*;
import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.*;

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

    // these have to be cached
    private GooTransition2D gooTransition2D;
    private SquaresTransition2D squaresTransition2D;

    public BricTransitionFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage frameA = src;
        BufferedImage frameB = ImageUtils.createImageWithSameColorModel(src);

        Graphics2D g2 = dest.createGraphics();

        // TODO are they worth it?
        // they increase te processing time, but do not seem to have any effect
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        g2.setRenderingHint(KEY_DITHERING, VALUE_DITHER_ENABLE);
        g2.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);

        Transition transition;
        switch (type) {
            case BARS_HORIZONTAL:
                transition = new BarsTransition2D(BarsTransition2D.HORIZONTAL, false);
                break;
            case BARS_VERTICAL:
                transition = new BarsTransition2D(BarsTransition2D.VERTICAL, false);
                break;
            case FADE:
                transition = new BlendTransition2D();
                break;
            case BLINDS:
                transition = new BlindsTransition2D();
                break;
            case BOX_IN:
                transition = new BoxTransition2D(Transition.IN);
                break;
            case BOX_OUT:
                transition = new BoxTransition2D(Transition.OUT);
                break;
            case KALEIDOSCOPE:
                transition = new KaleidoscopeTransition2D();
                break;
            case CHECKERBOARD:
                transition = new CheckerboardTransition2D();
                break;
            case CIRCLE_IN:
                transition = new CircleTransition2D(Transition.IN);
                break;
            case CIRCLE_OUT:
                transition = new CircleTransition2D(Transition.OUT);
                break;
            case COLLAPSE:
                transition = new CollapseTransition2D();
                break;
            case CURTAIN:
                transition = new CurtainTransition2D();
                break;
            case DIAMONDS:
                transition = new DiamondsTransition2D(100);
                break;
//            case DOCUMENTARY:
//                transition = new DocumentaryTransition2D();
//                break;
            case DOTS:
                transition = new DotsTransition2D();
                break;
            case FLURRY:
                transition = new FlurryTransition2D(Transition.OUT);
                break;
            case FUNKY_WIPE:
                transition = new FunkyWipeTransition2D(true);
                break;
            case GOO:
                // must be cached, otherwise randomness is different in each frame
                if(gooTransition2D == null) {
                    gooTransition2D = new GooTransition2D();
                }
                transition = gooTransition2D;
                break;
            case HALFTONE:
                transition = new HalftoneTransition2D();
                break;
            case LEVITATE:
                transition = new LevitateTransition2D();
                break;
            case MICROSCOPE:
                transition = new MicroscopeTransition2D();
                break;
            case PIVOT:
                transition = new PivotTransition2D(Transition.BOTTOM_LEFT, false);
                break;
            case RADIAL_WIPE:
                transition = new RadialWipeTransition2D();
                break;
            case REVEAL:
                transition = new RevealTransition2D();
                break;
            case ROTATE:
                transition = new RotateTransition2D(Transition.OUT);
                break;
            case SCALE:
                transition = new ScaleTransition2D(Transition.OUT);
                break;
            case SCRIBBLE:
                transition = new ScribbleTransition2D(false);
                break;
            case SCRIBBLE_TWICE:
                transition = new ScribbleTransition2D(true);
                break;
            case SPIRAL:
                transition = new SpiralTransition2D(false);
                break;
            case SPIRAL_SPRAWL:
                transition = new SpiralTransition2D(true);
                break;
            case SQUARE_RAIN:
                transition = new SquareRainTransition2D();
                break;
            case SQUARES:
                // must be cached, otherwise randomness is different in each frame
                if(squaresTransition2D == null) {
                    squaresTransition2D = new SquaresTransition2D();
                }
                transition = squaresTransition2D;
                break;
            case STARS:
                transition = new StarsTransition2D();
                break;
            case TOSS_IN:
                transition = new ReversedTransition(new TossTransition2D());
                progress = 1.0f - progress;
                break;
            case WAVE:
//                transition = new WaveTransition2D();
                transition = new ReversedTransition(new WaveTransition2D(Transition.LEFT));
//                progress = 1.0f - progress;
                break;

            default:
                throw new IllegalStateException("Unexpected type = " + type);
        }

        if(progress < 0.0f) {
            progress = 0.0f;
        }
        if(progress > 1.0f) {
            progress = 1.0f;
        }

        transition.paint(g2, frameA, frameB, progress);
        g2.dispose();

        return dest;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setType(int type) {
        this.type = type;
    }
}
