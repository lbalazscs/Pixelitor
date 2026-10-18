/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

/**
 * A piecewise spline curve defined by sorted control
 * points (knots) used to generate tone-mapping lookup tables.
 */
public class Curve {
    // knot coordinates, within [0, 1], kept sorted
    // in ascending order by their x coordinate
    public float[] x;
    public float[] y;

    public Curve() {
        reset();
    }

    public Curve(Curve curve) {
        setKnots(curve.x.clone(), curve.y.clone());
    }

    /**
     * Resets the curve to an identity transfer function defined by (0, 0) and (1, 1).
     */
    public void reset() {
        setKnots(new float[]{0, 1}, new float[]{0, 1});
    }

    /**
     * Sets the knot coordinate arrays.
     */
    public void setKnots(float[] newX, float[] newY) {
        assert newX.length == newY.length;
        x = newX;
        y = newY;
    }

    /**
     * Inserts a knot at the given coordinates while preserving
     * ascending x-coordinate order and returns its index.
     */
    public int addKnot(float kx, float ky) {
        int index = findInsertionIndex(kx);
        int numKnots = x.length;
        float[] newX = new float[numKnots + 1];
        float[] newY = new float[numKnots + 1];

        System.arraycopy(x, 0, newX, 0, index);
        System.arraycopy(y, 0, newY, 0, index);

        newX[index] = kx;
        newY[index] = ky;

        System.arraycopy(x, index, newX, index + 1, numKnots - index);
        System.arraycopy(y, index, newY, index + 1, numKnots - index);

        setKnots(newX, newY);

        return index;
    }

    /**
     * Finds the index at which a knot with the given x
     * should be inserted to maintain sorted order.
     */
    public int findInsertionIndex(float kx) {
        int numKnots = x.length;
        for (int i = 0; i < numKnots; i++) {
            if (x[i] > kx) {
                return i;
            }
        }

        return numKnots;
    }

    /**
     * Removes the knot at the given index and resizes the arrays, ensuring
     * that at least two knots remain to define the curve endpoints.
     */
    public void removeKnot(int index) {
        int numKnots = x.length;
        if (numKnots <= 2 || index < 0 || index >= numKnots) {
            return;
        }
        float[] newX = new float[numKnots - 1];
        float[] newY = new float[numKnots - 1];

        System.arraycopy(x, 0, newX, 0, index);
        System.arraycopy(y, 0, newY, 0, index);

        System.arraycopy(x, index + 1, newX, index, numKnots - index - 1);
        System.arraycopy(y, index + 1, newY, index, numKnots - index - 1);

        setKnots(newX, newY);
    }

    /**
     * Generates a 256-element integer LUT by interpolating
     * along the curve using a Catmull-Rom spline.
     */
    public int[] makeTable() {
        int numKnots = x.length;

        // prepends and appends duplicate endpoints to ensure that the spline
        // algorithm has boundary tangents to evaluate across the full range
        float[] paddedX = new float[numKnots + 2];
        float[] paddedY = new float[numKnots + 2];
        System.arraycopy(x, 0, paddedX, 1, numKnots);
        System.arraycopy(y, 0, paddedY, 1, numKnots);
        paddedX[0] = paddedX[1];
        paddedY[0] = paddedY[1];
        paddedX[numKnots + 1] = paddedX[numKnots];
        paddedY[numKnots + 1] = paddedY[numKnots];

        int[] lut = new int[256];

        int prevLutX = -1;
        int prevLutY = -1;

        // samples 2048 discrete steps along the parametric curve and maps to [0, 255],
        // linearly interpolating between samples to prevent gaps in steep sections
        for (int i = 0; i <= 2048; i++) {
            float f = i / 2048.0f;
            int lutX = (int) (255 * ImageMath.spline(f, paddedX.length, paddedX, true) + 0.5f);
            int lutY = (int) (255 * ImageMath.spline(f, paddedX.length, paddedY, false) + 0.5f);
            lutX = ImageMath.clamp(lutX, 0, 255);
            lutY = ImageMath.clamp(lutY, 0, 255);

            if (prevLutX >= 0) {
                if (lutX > prevLutX) {
                    for (int currX = prevLutX + 1; currX <= lutX; currX++) {
                        lut[currX] = ImageMath.clamp(
                            Math.round(prevLutY + (float) (lutY - prevLutY) * (currX - prevLutX) / (lutX - prevLutX)),
                            0, 255);
                    }
                } else {
                    lut[lutX] = lutY;
                }
            } else {
                // flat extrapolation: extend the first knot horizontally to x=0
                for (int currX = 0; currX <= lutX; currX++) {
                    lut[currX] = lutY;
                }
            }

            prevLutX = lutX;
            prevLutY = lutY;
        }

        // flat extrapolation: extend the last knot horizontally to x=255
        for (int currX = prevLutX + 1; currX <= 255; currX++) {
            lut[currX] = prevLutY;
        }

        return lut;
    }
}
