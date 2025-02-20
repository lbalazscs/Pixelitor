/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import net.jafama.FastMath;

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
    private int distanceMetric = HSB;

    private static final int WHITE_PIXEL = 0xFF_FF_FF_FF;
    private static final int BLACK_PIXEL = 0xFF_00_00_00;

    private double maxTolerance;
    private double minTolerance;

    private int refR, refG, refB; // the reference color in RGB
    private float refHue, refSat, refBri; // the reference color in HSB

    private boolean inverted;

    protected MaskFromColorRangeFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the reference color against which other colors will be compared.
     */
    public void setReferenceColor(Color c) {
        refR = c.getRed();
        refG = c.getGreen();
        refB = c.getBlue();

        if (distanceMetric != RGB) {
            float[] hsb = Color.RGBtoHSB(refR, refG, refB, null);
            refHue = hsb[0];
            refSat = hsb[1];
            refBri = hsb[2];
        }
    }

    /**
     * Sets the color distance calculation method.
     */
    public void setDistanceMetric(int distanceMetric) {
        this.distanceMetric = distanceMetric;
    }

    /**
     * Sets the tolerance and softness parameters for the mask creation.
     */
    public void setTolerance(double tolerance, double softness) {
        // otherwise tolerance = 0 does not select exact matches - why?
        double adjustedTolerance = tolerance + 0.1;

        maxTolerance = adjustedTolerance * (1.0 - softness);
        minTolerance = adjustedTolerance * (1.0 + softness);
    }

    /**
     * Sets whether the mask should be inverted
     * (exclude matching colors instead of including them).
     */
    public void setInvertMask(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        double dist = calcDistance(rgb);

        if (dist > minTolerance) {
            if (inverted) {
                return WHITE_PIXEL;
            } else {
                return BLACK_PIXEL;
            }
        } else if (dist < maxTolerance) {
            if (inverted) {
                return BLACK_PIXEL;
            } else {
                return WHITE_PIXEL;
            }
        } else {
            // linear interpolation
            int v = (int) ((minTolerance - dist) * 255 / (minTolerance - maxTolerance));
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

        return FastMath.sqrtQuick(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB);
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

        return 150 * FastMath.sqrtQuick(deltaHue * deltaHue + deltaSat * deltaSat + deltaBri * deltaBri);
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
