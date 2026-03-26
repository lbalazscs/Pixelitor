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

import java.awt.geom.Point2D;

/**
 * A filter which performs a perspective distortion on an image.
 * It maps an image onto an arbitrary convex quadrilateral.
 */
public class PerspectiveFilter extends TransformFilter {
    // the projective transformation matrix that maps normalized source
    // coordinates (u, v) in [0,1]² to a point in the destination quad
    private final float a11, a12, a13, a21, a22, a23, a31, a32, a33;

    // the inverse matrix that maps destination pixel
    // coordinates back to source pixel coordinates
    private final float A, B, C, D, E, F, G, H, I;

    /**
     * Constructs a PerspectiveFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param x0 the new X position of the top left corner
     * @param y0 the new Y position of the top left corner
     * @param x1 the new X position of the top right corner
     * @param y1 the new Y position of the top right corner
     * @param x2 the new X position of the bottom right corner
     * @param y2 the new Y position of the bottom right corner
     * @param x3 the new X position of the bottom left corner
     * @param y3 the new Y position of the bottom left corner
     */
    public PerspectiveFilter(String filterName,
                             int edgeAction, int interpolation,
                             float x0, float y0,
                             float x1, float y1,
                             float x2, float y2,
                             float x3, float y3,
                             int srcWidth, int srcHeight) {
        super(filterName, edgeAction, interpolation);

        float dx1 = x1 - x2;
        float dy1 = y1 - y2;
        float dx2 = x3 - x2;
        float dy2 = y3 - y2;
        float dx3 = x0 - x1 + x2 - x3;
        float dy3 = y0 - y1 + y2 - y3;

        if (dx3 == 0 && dy3 == 0) {
            // the quad is a parallelogram and the transform
            // simplifies to a pure affine mapping
            a11 = x1 - x0;
            a21 = x2 - x1;
            a31 = x0;
            a12 = y1 - y0;
            a22 = y2 - y1;
            a32 = y0;
            a13 = a23 = 0;
        } else {
            a13 = (dx3 * dy2 - dx2 * dy3) / (dx1 * dy2 - dy1 * dx2);
            a23 = (dx1 * dy3 - dy1 * dx3) / (dx1 * dy2 - dy1 * dx2);
            a11 = x1 - x0 + a13 * x1;
            a21 = x3 - x0 + a23 * x3;
            a31 = x0;
            a12 = y1 - y0 + a13 * y1;
            a22 = y3 - y0 + a23 * y3;
            a32 = y0;
        }
        a33 = 1; // a normalization convention in homogeneous coordinates

        float invWidth = 1.0f / srcWidth;
        float invHeight = 1.0f / srcHeight;

        // the adjugate (cofactor transpose) of the forward matrix,
        // with A–F pre-fused by srcWidth/srcHeight so that
        // transformInverse computes source pixel coordinates directly
        A = a22 * a33 - a32 * a23;
        B = (a31 * a23 - a21 * a33) * srcWidth * invHeight;
        C = (a21 * a32 - a31 * a22) * srcWidth;
        D = (a32 * a13 - a12 * a33) * srcHeight * invWidth;
        E = a11 * a33 - a31 * a13;
        F = (a31 * a12 - a11 * a32) * srcHeight;
        G = (a12 * a23 - a22 * a13) * invWidth;
        H = (a21 * a13 - a11 * a23) * invHeight;
        I = a11 * a22 - a21 * a12;
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Float();
        }
        float x = (float) srcPt.getX();
        float y = (float) srcPt.getY();
        float f = 1.0f / (x * a13 + y * a23 + a33);
        dstPt.setLocation((x * a11 + y * a21 + a31) * f, (x * a12 + y * a22 + a32) * f);
        return dstPt;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float invDen = 1.0f / (G * x + H * y + I);
        out[0] = (A * x + B * y + C) * invDen;
        out[1] = (D * x + E * y + F) * invDen;
    }
}
