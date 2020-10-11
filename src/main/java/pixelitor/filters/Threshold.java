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

import pixelitor.colors.Colors;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.lookup.LuminanceLookup;
import pixelitor.filters.util.FilterUtils;

import java.awt.image.BufferedImage;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * The Threshold filter
 */
public class Threshold extends ParametrizedFilter {
    public static final String NAME = "Threshold";

    private static final int LUMINOSITY = 1;
    private static final int RED = 2;
    private static final int GREEN = 3;
    private static final int BLUE = 4;
    private static final int SATURATION = 5;

    private final RangeParam threshold = new RangeParam("Threshold", 0,
        128, 255, false, BORDER);

    private final IntChoiceParam criterion = new IntChoiceParam("Based on", new Item[]{
        new Item("Luminosity", LUMINOSITY),
        new Item("Red Channel", RED),
        new Item("Green Channel", GREEN),
        new Item("Blue Channel", BLUE),
        new Item("Saturation", SATURATION),
    });

    public Threshold() {
        super(ShowOriginal.YES);

        setParams(threshold, criterion);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int basedOn = criterion.getValue();
        RGBPixelOp pixelOp = rgbPixelOp(threshold.getValueAsDouble(), basedOn);
        return FilterUtils.runRGBPixelOp(pixelOp, src, dest);
    }

    private static RGBPixelOp rgbPixelOp(double threshold, int basedOn) {
        return switch (basedOn) {
            case LUMINOSITY -> luminosityPixelOp(threshold);
            case RED -> redPixelOp(threshold);
            case GREEN -> greenPixelOp(threshold);
            case BLUE -> bluePixelOp(threshold);
            case SATURATION -> saturationPixelOp(threshold);
            default -> throw new IllegalStateException("basedOn = " + basedOn);
        };
    }

    private static RGBPixelOp luminosityPixelOp(double threshold) {
        return (a, r, g, b) -> {
            double luminosity = LuminanceLookup.from(r, g, b);
            if (luminosity > threshold) {
                r = 255;
                g = 255;
                b = 255;
            } else {
                r = 0;
                g = 0;
                b = 0;
            }

            return a << 24 | r << 16 | g << 8 | b;
        };
    }

    private static RGBPixelOp redPixelOp(double threshold) {
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

            return a << 24 | r << 16 | g << 8 | b;
        };
    }

    private static RGBPixelOp greenPixelOp(double threshold) {
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

            return a << 24 | r << 16 | g << 8 | b;
        };
    }

    private static RGBPixelOp bluePixelOp(double threshold) {
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

            return a << 24 | r << 16 | g << 8 | b;
        };
    }

    private static RGBPixelOp saturationPixelOp(double threshold) {
        return new RGBPixelOp() {
            final float satThreshold = (float) (threshold / 255.0f);

            @Override
            public int changeRGB(int a, int r, int g, int b) {
                float sat = Colors.calcSaturation(r, g, b);
                if (sat > satThreshold) {
                    r = 255;
                    g = 255;
                    b = 255;
                } else {
                    r = 0;
                    g = 0;
                    b = 0;
                }

                return a << 24 | r << 16 | g << 8 | b;
            }
        };
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
