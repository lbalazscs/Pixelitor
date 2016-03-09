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

package pixelitor.layers;

import com.jhlabs.image.PointFilter;
import net.jafama.FastMath;

import java.awt.Color;

class MaskFromColorRangeFilter extends PointFilter {
    public static final int MODE_RGB = 1;
    public static final int MODE_HSB = 2;
    private int mode = MODE_HSB;

    public static final int WHITE_PIXEL = 0xFF_FF_FF_FF;
    public static final int BLACK_PIXEL = 0xFF_00_00_00;
    private double toleranceMin;
    private double toleranceMax;

    private int refR, refG, refB;
    private float refHue, refSat, refBri;

    private boolean invert;

    protected MaskFromColorRangeFilter(String filterName) {
        super(filterName);
    }

    public void setColor(Color c) {
        refR = c.getRed();
        refG = c.getGreen();
        refB = c.getBlue();

        if (mode == MODE_HSB) {
            float[] hsb = Color.RGBtoHSB(refR, refG, refB, null);
            refHue = hsb[0];
            refSat = hsb[1];
            refBri = hsb[2];
        }
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setTolerance(double tolerance, double fuzziness) {
        // otherwise tolerance = 0 does not select exact matches - why?
        double adjustedTolerance = tolerance + 0.1;

        this.toleranceMin = adjustedTolerance * (1.0 - fuzziness);
        this.toleranceMax = adjustedTolerance * (1.0 + fuzziness);
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

        if (mode == MODE_RGB) {
            int deltaR = r - refR;
            int deltaG = g - refG;
            int deltaB = b - refB;

            dist = FastMath.sqrtQuick(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB);
        } else if (mode == MODE_HSB) {
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
        } else {
            throw new IllegalStateException("mode = " + mode);
        }

        if (dist > toleranceMax) {
            if (invert) {
                return WHITE_PIXEL;
            } else {
                return BLACK_PIXEL;
            }
        } else if (dist < toleranceMin) {
            if (invert) {
                return BLACK_PIXEL;
            } else {
                return WHITE_PIXEL;
            }
        } else {
            // linear interpolation
            int v = (int) ((toleranceMax - dist) * 255 / (toleranceMax - toleranceMin));
            if (invert) {
                v = 255 - v;
            }
            return 0xFF_00_00_00 | (v << 16) | (v << 8) | v;
        }
    }
}
