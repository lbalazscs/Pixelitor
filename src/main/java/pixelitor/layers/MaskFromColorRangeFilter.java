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

package pixelitor.layers;

import com.jhlabs.image.PointFilter;
import net.jafama.FastMath;

import java.awt.Color;

/**
 * A filter used internally by the "Mask from Color Range".
 * It creates a grayscale mask.
 */
class MaskFromColorRangeFilter extends PointFilter {
    public static final int RGB = 1;
    public static final int HSB = 2;
    public static final int HUE = 3;
    public static final int SAT = 4;
    private int distType = HSB;

    private static final int WHITE_PIXEL = 0xFF_FF_FF_FF;
    private static final int BLACK_PIXEL = 0xFF_00_00_00;

    private double maxTolerance;
    private double minTolerance;

    private int refR, refG, refB; // the reference color in RGB
    private float refHue, refSat, refBri; // the reference color in HSB

    private boolean invert;

    protected MaskFromColorRangeFilter(String filterName) {
        super(filterName);
    }

    public void setColor(Color c) {
        refR = c.getRed();
        refG = c.getGreen();
        refB = c.getBlue();

        if (distType != RGB) {
            float[] hsb = Color.RGBtoHSB(refR, refG, refB, null);
            refHue = hsb[0];
            refSat = hsb[1];
            refBri = hsb[2];
        }
    }

    public void setDistType(int distType) {
        this.distType = distType;
    }

    public void setTolerance(double tolerance, double fuzziness) {
        // otherwise tolerance = 0 does not select exact matches - why?
        double adjustedTolerance = tolerance + 0.1;

        maxTolerance = adjustedTolerance * (1.0 - fuzziness);
        minTolerance = adjustedTolerance * (1.0 + fuzziness);
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        double dist;

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (distType == RGB) {
            int deltaR = r - refR;
            int deltaG = g - refG;
            int deltaB = b - refB;

            dist = FastMath.sqrtQuick(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB);
        } else if (distType == HSB) {
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

//            int v = Math.abs((int) (deltaHue * 255 * 2));
//            return 0xFF_00_00_00 | (v << 16) | (v << 8) | v;

            dist = 150 * FastMath.sqrtQuick(deltaHue * deltaHue + deltaSat * deltaSat + deltaBri * deltaBri);
        } else if (distType == HUE) {
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            float deltaHue = hsb[0] - refHue;
            // hue is an angle
            if (deltaHue > 0.5f) {
                deltaHue = 1.0f - deltaHue;
            } else if (deltaHue < -0.5f) {
                deltaHue = 1.0f + deltaHue;
            }
            dist =  Math.abs(1000 * deltaHue);
        } else if (distType == SAT) {
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            float deltaSat = hsb[1] - refSat;
            dist = 150 * Math.abs(deltaSat);
        } else {
            throw new IllegalStateException("interpolation = " + distType);
        }

        if (dist > minTolerance) {
            if (invert) {
                return WHITE_PIXEL;
            } else {
                return BLACK_PIXEL;
            }
        } else if (dist < maxTolerance) {
            if (invert) {
                return BLACK_PIXEL;
            } else {
                return WHITE_PIXEL;
            }
        } else {
            // linear interpolation
            int v = (int) ((minTolerance - dist) * 255 / (minTolerance - maxTolerance));
            if (invert) {
                v = 255 - v;
            }
            return 0xFF_00_00_00 | v << 16 | v << 8 | v;
        }
    }
}
