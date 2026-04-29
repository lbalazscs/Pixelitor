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

package pixelitor.filters.impl;

import com.jhlabs.image.FourColorFilter;

/**
 * A filter which draws a four color gradient using Inverse Distance Weighting
 * (Shepard's Method) to create an organic "Metaball" effect.
 *
 * It ignores angles and grid coordinates completely and relies purely on the
 * distance from the pixel to the 5 anchor points (the 4 corners, plus the
 * Center Point with an averaged color).
 *
 * The InterpolationType parameter is repurposed to dictate the power 'p'
 * of the inverse distance. Higher powers create flatter regions of solid color
 * with sharper boundaries, mirroring the visual effect of higher-order splines.
 */
public class FourColorMetaballFilter extends FourColorFilter {
    private static final double EPSILON = 1.0e-8;

    // center colors
    private float aC;
    private float cC1, cC2, cC3;

    // the power parameter for inverse distance weighting
    private int p;

    public FourColorMetaballFilter(String filterName,
                                   int colorNW, int colorNE, int colorSW, int colorSE,
                                   InterpolationType interpolation, ColorSpaceType colorSpace,
                                   double relCx, double relCy) {
        super(filterName, colorNW, colorNE, colorSW, colorSE, interpolation, colorSpace, relCx, relCy);
    }

    @Override
    public void setDimensions(int width, int height) {
        super.setDimensions(width, height);

        // precompute the color of the 5th anchor point (the center)
        // as the weighted average of the four corners
        this.aC = (int) (calcWeightedCenterAlpha() + 0.5f);
        float[] cCenter = calcWeightedCenterColor();
        this.cC1 = cCenter[0];
        this.cC2 = cCenter[1];
        this.cC3 = cCenter[2];

        // map the UI interpolation type to the inverse distance weighting power 'p'
        this.p = switch (interpolation) {
            case LINEAR -> 1;
            case CUBIC -> 2;
            case QUINTIC -> 3;
            case SEPTIC -> 4;
        };
    }

    /**
     * Calculates the 1 / (d^p) weight quickly without using Math.pow.
     */
    private double calculateWeight(double d2) {
        return switch (p) {
            case 1 -> 1.0 / Math.sqrt(d2);
            case 2 -> 1.0 / d2;
            case 3 -> 1.0 / (d2 * Math.sqrt(d2));
            case 4 -> 1.0 / (d2 * d2);
            default -> throw new IllegalStateException("p = " + p);
        };
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // calculate squared distances to all 5 anchor points
        double dxNW = x;
        double dyNW = y;
        double d2_NW = dxNW * dxNW + dyNW * dyNW;

        double dxNE = x - width;
        double dyNE = y;
        double d2_NE = dxNE * dxNE + dyNE * dyNE;

        double dxSW = x;
        double dySW = y - height;
        double d2_SW = dxSW * dxSW + dySW * dySW;

        double dxSE = x - width;
        double dySE = y - height;
        double d2_SE = dxSE * dxSE + dySE * dySE;

        double dxC = x - cx;
        double dyC = y - cy;
        double d2_C = dxC * dxC + dyC * dyC;

        // handle exact anchor point matches to prevent division by zero
        if (d2_NW < EPSILON) {
            return colorSpace.toSrgb(aNW, cNW[0], cNW[1], cNW[2]);
        }
        if (d2_NE < EPSILON) {
            return colorSpace.toSrgb(aNE, cNE[0], cNE[1], cNE[2]);
        }
        if (d2_SW < EPSILON) {
            return colorSpace.toSrgb(aSW, cSW[0], cSW[1], cSW[2]);
        }
        if (d2_SE < EPSILON) {
            return colorSpace.toSrgb(aSE, cSE[0], cSE[1], cSE[2]);
        }
        if (d2_C < EPSILON) {
            return colorSpace.toSrgb((int) (aC + 0.5f), cC1, cC2, cC3);
        }

        // calculate weights based on the selected interpolation (power p)
        double w_NW = calculateWeight(d2_NW);
        double w_NE = calculateWeight(d2_NE);
        double w_SW = calculateWeight(d2_SW);
        double w_SE = calculateWeight(d2_SE);
        double w_C = calculateWeight(d2_C);

        double sumWeights = w_NW + w_NE + w_SW + w_SE + w_C;

        // weighted averages for all components
        double aInterp = (w_NW * aNW + w_NE * aNE + w_SW * aSW + w_SE * aSE + w_C * aC) / sumWeights;
        double c1Interp = (w_NW * cNW[0] + w_NE * cNE[0] + w_SW * cSW[0] + w_SE * cSE[0] + w_C * cC1) / sumWeights;
        double c2Interp = (w_NW * cNW[1] + w_NE * cNE[1] + w_SW * cSW[1] + w_SE * cSE[1] + w_C * cC2) / sumWeights;
        double c3Interp = (w_NW * cNW[2] + w_NE * cNE[2] + w_SW * cSW[2] + w_SE * cSE[2] + w_C * cC3) / sumWeights;

        return colorSpace.toSrgb((int) (aInterp + 0.5), (float) c1Interp, (float) c2Interp, (float) c3Interp);
    }
}
