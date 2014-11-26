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
import pixelitor.utils.ImageUtils;
import pixelitor.utils.SliderSpinner;

import java.awt.image.BufferedImage;

public class Threshold extends FilterWithParametrizedGUI {
    public static final int CRIT_LUMINOSITY = 1;
    public static final int CRIT_RED = 2;
    public static final int CRIT_GREEN = 3;
    public static final int CRIT_BLUE = 4;
    public static final int CRIT_SATURATION = 5;

    private final RangeParam threshold = new RangeParam("Threshold", 0,
            255, 128, false, SliderSpinner.TextPosition.BORDER);

    IntChoiceParam criterion = new IntChoiceParam("Based on",
            new IntChoiceParam.Value[]{
                new IntChoiceParam.Value("Luminosity", CRIT_LUMINOSITY),
                new IntChoiceParam.Value("Red Channel", CRIT_RED),
                new IntChoiceParam.Value("Green Channel", CRIT_GREEN),
                new IntChoiceParam.Value("Blue Channel", CRIT_BLUE),
                new IntChoiceParam.Value("Saturation", CRIT_SATURATION),
            });

    public Threshold() {
        super("Threshold", true, false);
        setParamSet(new ParamSet(threshold, criterion));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int basedOn = criterion.getValue();
        RGBPixelOp pixelOp = getRGBPixelOp(threshold.getValue(), basedOn);
        return FilterUtils.runRGBPixelOp(pixelOp, src, dest);
    }

    RGBPixelOp getRGBPixelOp(final int threshold, int basedOn) {
        switch (basedOn) {
            case CRIT_LUMINOSITY:
                return new RGBPixelOp() {
                    @Override
                    public int changeRGB(int a, int r, int g, int b) {
                        // TODO can be faster with the luminosity lookup?
                        int luminosity = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                        if (luminosity > threshold) {
                            r = 255;
                            g = 255;
                            b = 255;
                        } else {
                            r = 0;
                            g = 0;
                            b = 0;
                        }

                        return (a << 24) | (r << 16) | (g << 8) | b;
                    }
                };
            case CRIT_RED:
                return new RGBPixelOp() {
                    @Override
                    public int changeRGB(int a, int r, int g, int b) {
                        if (r > threshold) {
                            r = 255;
                            g = 255;
                            b = 255;
                        } else {
                            r = 0;
                            g = 0;
                            b = 0;
                        }

                        return (a << 24) | (r << 16) | (g << 8) | b;
                    }
                };
            case CRIT_GREEN:
                return new RGBPixelOp() {
                    @Override
                    public int changeRGB(int a, int r, int g, int b) {
                        if (g > threshold) {
                            r = 255;
                            g = 255;
                            b = 255;
                        } else {
                            r = 0;
                            g = 0;
                            b = 0;
                        }

                        return (a << 24) | (r << 16) | (g << 8) | b;
                    }
                };
            case CRIT_BLUE:
                return new RGBPixelOp() {
                    @Override
                    public int changeRGB(int a, int r, int g, int b) {
                        if (b > threshold) {
                            r = 255;
                            g = 255;
                            b = 255;
                        } else {
                            r = 0;
                            g = 0;
                            b = 0;
                        }

                        return (a << 24) | (r << 16) | (g << 8) | b;
                    }
                };
            case CRIT_SATURATION:
                return new RGBPixelOp() {
                    float satThreshold = threshold / 255.0f;
                    @Override
                    public int changeRGB(int a, int r, int g, int b) {
                        float sat = ImageUtils.calcSaturation(r, g, b);
                        if (sat > satThreshold) {
                            r = 255;
                            g = 255;
                            b = 255;
                        } else {
                            r = 0;
                            g = 0;
                            b = 0;
                        }

                        return (a << 24) | (r << 16) | (g << 8) | b;
                    }
                };

        }
        throw new IllegalStateException("basedOn = " + basedOn);
    }
}
