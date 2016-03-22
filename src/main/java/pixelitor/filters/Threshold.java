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

package pixelitor.filters;

import pixelitor.colors.ColorUtils;
import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class Threshold extends FilterWithParametrizedGUI {
    public static final String NAME = "Threshold";

    private static final int CRIT_LUMINOSITY = 1;
    private static final int CRIT_RED = 2;
    private static final int CRIT_GREEN = 3;
    private static final int CRIT_BLUE = 4;
    private static final int CRIT_SATURATION = 5;

    private final RangeParam threshold = new RangeParam("Threshold", 0,
            128, 255, AddDefaultButton.NO, BORDER);

    private final IntChoiceParam criterion = new IntChoiceParam("Based on",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("Luminosity", CRIT_LUMINOSITY),
                    new IntChoiceParam.Value("Red Channel", CRIT_RED),
                    new IntChoiceParam.Value("Green Channel", CRIT_GREEN),
                    new IntChoiceParam.Value("Blue Channel", CRIT_BLUE),
                    new IntChoiceParam.Value("Saturation", CRIT_SATURATION),
            });

    public Threshold() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(threshold, criterion));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int basedOn = criterion.getValue();
        RGBPixelOp pixelOp = getRGBPixelOp(threshold.getValueAsDouble(), basedOn);
        return FilterUtils.runRGBPixelOp(pixelOp, src, dest);
    }

    private static RGBPixelOp getRGBPixelOp(double threshold, int basedOn) {
        switch (basedOn) {
            case CRIT_LUMINOSITY:
                return (a, r, g, b) -> {
                    // TODO can be faster with the luminosity lookup?
                    double luminosity = 0.299 * r + 0.587 * g + 0.114 * b;
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
                };
            case CRIT_RED:
                return (a, r, g, b) -> {
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
                };
            case CRIT_GREEN:
                return (a, r, g, b) -> {
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
                };
            case CRIT_BLUE:
                return (a, r, g, b) -> {
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
                };
            case CRIT_SATURATION:
                return new RGBPixelOp() {
                    final float satThreshold = (float) (threshold / 255.0f);

                    @Override
                    public int changeRGB(int a, int r, int g, int b) {
                        float sat = ColorUtils.calcSaturation(r, g, b);
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

    @Override
    public boolean supportsGray() {
        return false;
    }
}
