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
import com.jhlabs.image.ImageMath;

/**
 * A filter which draws a four color gradient using triangulation and
 * barycentric interpolation.
 * The midpoint splits the rectangular image into four triangular sections.
 * The colors are interpolated across the vertices of the corresponding triangle.
 */
public class FourColorTriangularFilter extends FourColorFilter {
    private static final double EPSILON = -1.0e-5;

    private Triangle[] triangles;

    /**
     * A helper record to hold the colors of each of the 4 triangles
     * and pre-calculate the affine coefficients for barycentric coordinates.
     */
    private record Triangle(
        double wa_x, double wa_y, double wa_c,
        double wb_x, double wb_y, double wb_c,
        float[] colorA, int alphaA,
        float[] colorB, int alphaB,
        float[] colorC, int alphaC
    ) {
        static Triangle create(
            double vAx, double vAy, float[] colorA, int alphaA,
            double vBx, double vBy, float[] colorB, int alphaB,
            double vCx, double vCy, float[] colorC, int alphaC) {

            double det = (vBy - vCy) * (vAx - vCx) + (vCx - vBx) * (vAy - vCy);

            // exclude degenerate triangles directly at setup
            if (Math.abs(det) < 1.0e-9) {
                return null;
            }

            double invDet = 1.0 / det;

            // precompute the affine coefficients for wa and wb
            double wa_x = (vBy - vCy) * invDet;
            double wa_y = (vCx - vBx) * invDet;
            double wa_c = (-(vBy - vCy) * vCx - (vCx - vBx) * vCy) * invDet;

            double wb_x = (vCy - vAy) * invDet;
            double wb_y = (vAx - vCx) * invDet;
            double wb_c = (-(vCy - vAy) * vCx - (vAx - vCx) * vCy) * invDet;

            return new Triangle(wa_x, wa_y, wa_c, wb_x, wb_y, wb_c,
                colorA, alphaA, colorB, alphaB, colorC, alphaC);
        }
    }

    public FourColorTriangularFilter(String filterName,
                                     int colorNW, int colorNE, int colorSW, int colorSE,
                                     InterpolationType interpolation, ColorSpaceType colorSpace,
                                     double relCx, double relCy, int width, int height) {
        super(filterName, colorNW, colorNE, colorSW, colorSE, interpolation, colorSpace, relCx, relCy, width, height);

        setupTriangles();
    }

    private void setupTriangles() {
        int aCenter = (int) (calcWeightedCenterAlpha() + 0.5f);
        float[] cCenter = calcWeightedCenterColor();

        triangles = new Triangle[4];

        // north triangle (center, NE, NW)
        triangles[0] = Triangle.create(cx, cy, cCenter, aCenter,
            width, 0, cNE, aNE,
            0, 0, cNW, aNW);
        // east triangle (center, SE, NE)
        triangles[1] = Triangle.create(cx, cy, cCenter, aCenter,
            width, height, cSE, aSE,
            width, 0, cNE, aNE);
        // south triangle (center, SW, SE)
        triangles[2] = Triangle.create(cx, cy, cCenter, aCenter,
            0, height, cSW, aSW,
            width, height, cSE, aSE);
        // west triangle (center, NW, SW)
        triangles[3] = Triangle.create(cx, cy, cCenter, aCenter,
            0, 0, cNW, aNW,
            0, height, cSW, aSW);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        Triangle bestTri = null;
        double maxMinWeight = -Double.MAX_VALUE;
        double bestWa = 0, bestWb = 0, bestWc = 0;

        // find which bounding triangle the pixel falls into
        for (int i = 0; i < 4; i++) {
            Triangle t = triangles[i];

            // skip degenerate triangles (when the center point is pulled to an edge line)
            if (t == null) {
                continue;
            }

            // calculate barycentric weights
            double wa = t.wa_x() * x + t.wa_y() * y + t.wa_c();
            double wb = t.wb_x() * x + t.wb_y() * y + t.wb_c();
            double wc = 1.0 - wa - wb;

            double minW = Math.min(wa, Math.min(wb, wc));

            if (minW >= EPSILON) {
                bestTri = t;
                bestWa = wa;
                bestWb = wb;
                bestWc = wc;
                break;
            }

            // keep track in case no bounds cleanly match due to extreme float rounding
            if (minW > maxMinWeight) {
                maxMinWeight = minW;
                bestTri = t;
                bestWa = wa;
                bestWb = wb;
                bestWc = wc;
            }
        }

        if (bestTri == null) {
            return rgb; // should only happen mathematically if every single bounding rect edge degenerates
        }

        float wa = (float) bestWa;
        float wb = (float) bestWb;
        float wc = (float) bestWc;

        // allow application of non-linear smooth interpolation over the barycentric coordinates
        if (interpolation != InterpolationType.LINEAR) {
            wa = Math.clamp(wa, 0.0f, 1.0f);
            wb = Math.clamp(wb, 0.0f, 1.0f);
            wc = Math.clamp(wc, 0.0f, 1.0f);

            wa = interpolation.calcInterpolatedWeight(wa);
            wb = interpolation.calcInterpolatedWeight(wb);
            wc = interpolation.calcInterpolatedWeight(wc);

            float sum = wa + wb + wc;
            if (sum > 0.0f) {
                wa /= sum;
                wb /= sum;
                wc /= sum;
            } else {
                wa = 1.0f / 3.0f;
                wb = 1.0f / 3.0f;
                wc = 1.0f / 3.0f;
            }
        }

        // apply determined weights to color vectors and alpha
        int a = ImageMath.clamp((int) (wa * bestTri.alphaA() + wb * bestTri.alphaB() + wc * bestTri.alphaC() + 0.5f), 0, 255);
        float c1 = wa * bestTri.colorA()[0] + wb * bestTri.colorB()[0] + wc * bestTri.colorC()[0];
        float c2 = wa * bestTri.colorA()[1] + wb * bestTri.colorB()[1] + wc * bestTri.colorC()[1];
        float c3 = wa * bestTri.colorA()[2] + wb * bestTri.colorB()[2] + wc * bestTri.colorC()[2];

        // format to standard packed sRGB representation
        return colorSpace.toSrgb(a, c1, c2, c3);
    }
}
