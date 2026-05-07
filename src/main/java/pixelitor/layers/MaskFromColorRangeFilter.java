/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import com.jhlabs.image.PointFilter;

import java.awt.Color;

/**
 * A filter that creates a grayscale mask based on color similarity
 * to a reference color. Used internally by the "Mask from Color Range".
 */
class MaskFromColorRangeFilter extends PointFilter {
    public static final int RGB = 1;
    public static final int HSB = 2;
    public static final int HUE = 3;
    public static final int SAT = 4;

    private final int distanceMetric;

    private static final int WHITE_PIXEL = 0xFF_FF_FF_FF;
    private static final int BLACK_PIXEL = 0xFF_00_00_00;

    private final double minTolerance;
    private final double maxTolerance;

    private final int refR, refG, refB; // the reference color in RGB
    private final float refHue, refSat, refBri; // the reference color in HSB

    private final boolean inverted;

    /**
     * Constructs a MaskFromColorRangeFilter.
     *
     * @param filterName     the name of the filter
     * @param distanceMetric the color distance calculation method
     * @param referenceColor the reference color against which other colors will be compared
     * @param tolerance      the tolerance parameter for the mask creation
     * @param softness       the softness parameter for the mask creation
     * @param inverted       whether the mask should be inverted (exclude matching colors instead of including them)
     */
    protected MaskFromColorRangeFilter(String filterName, int distanceMetric, Color referenceColor, double tolerance, double softness, boolean inverted) {
        super(filterName);

        this.distanceMetric = distanceMetric;
        this.inverted = inverted;

        this.refR = referenceColor.getRed();
        this.refG = referenceColor.getGreen();
        this.refB = referenceColor.getBlue();

        if (distanceMetric != RGB) {
            float[] hsb = Color.RGBtoHSB(refR, refG, refB, null);
            this.refHue = hsb[0];
            this.refSat = hsb[1];
            this.refBri = hsb[2];
        } else {
            this.refHue = 0;
            this.refSat = 0;
            this.refBri = 0;
        }

        this.minTolerance = tolerance * (1.0 - softness);
        this.maxTolerance = tolerance * (1.0 + softness);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        double dist = calcDistance(rgb);

        if (dist > maxTolerance) {
            return inverted ? WHITE_PIXEL : BLACK_PIXEL;
        } else if (dist <= minTolerance) {
            return inverted ? BLACK_PIXEL : WHITE_PIXEL;
        } else {
            // linear interpolation
            int v = (int) ((maxTolerance - dist) * 255 / (maxTolerance - minTolerance));
            if (inverted) {
                v = 255 - v;
            }
            return 0xFF_00_00_00 | v << 16 | v << 8 | v;
        }
    }

    private double calcDistance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return switch (distanceMetric) {
            case RGB -> calcRGBDistance(r, g, b);
            case HSB -> calcHSBDistance(r, g, b);
            case HUE -> calcHueDistance(r, g, b);
            case SAT -> calcSatDistance(r, g, b);
            default -> throw new IllegalStateException("distType = " + distanceMetric);
        };
    }

    private double calcRGBDistance(int r, int g, int b) {
        int deltaR = r - refR;
        int deltaG = g - refG;
        int deltaB = b - refB;

        return Math.sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB);
    }

    private double calcHSBDistance(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);

        float deltaHue = hsb[0] - refHue;
        float deltaSat = hsb[1] - refSat;
        float deltaBri = hsb[2] - refBri;

        // hue is an angle
        if (deltaHue > 0.5f) {
            deltaHue = 1.0f - deltaHue;
        } else if (deltaHue < -0.5f) {
            deltaHue = 1.0f + deltaHue;
        }

        return 150 * Math.sqrt(deltaHue * deltaHue + deltaSat * deltaSat + deltaBri * deltaBri);
    }

    private double calcHueDistance(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float deltaHue = hsb[0] - refHue;
        // hue is an angle
        if (deltaHue > 0.5f) {
            deltaHue = 1.0f - deltaHue;
        } else if (deltaHue < -0.5f) {
            deltaHue = 1.0f + deltaHue;
        }
        return Math.abs(1000 * deltaHue);
    }

    private double calcSatDistance(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float deltaSat = hsb[1] - refSat;
        return 150 * Math.abs(deltaSat);
    }
}
