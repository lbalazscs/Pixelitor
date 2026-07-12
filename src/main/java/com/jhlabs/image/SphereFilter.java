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

import net.jafama.FastMath;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 * A filter which simulates a lens placed over an image.
 */
public class SphereFilter extends TransformFilter {
    // ellipse parameters
    private final double a;
    private final double b;
    private final double invA2; // cached 1/a²
    private final double invB2; // cached 1/b²
    private final double ab;    // cached a*b

    private final double invRefractionIndex;

    private final double cx;
    private final double cy;

    /**
     * Constructs a SphereFilter.
     *
     * @param filterName      the name of the filter.
     * @param edgeAction      the edge handling strategy (TRANSPARENT, REPEAT_EDGE,
     *                        WRAP_AROUND, REFLECT).
     * @param interpolation   the interpolation method (NEAREST_NEIGHBOR, BILINEAR,
     *                        BICUBIC).
     * @param center          the center of the lens effect in pixels.
     * @param a               the horizontal radius of the lens.
     * @param b               the vertical radius of the lens.
     * @param refractionIndex the index of refraction; controls how strongly the lens
     *                        bends light — higher values produce more distortion.
     */
    public SphereFilter(String filterName, int edgeAction, int interpolation,
                        Point2D center, double a, double b, double refractionIndex) {
        super(filterName, edgeAction, interpolation);

        this.cx = center.getX();
        this.cy = center.getY();
        this.a = a;
        this.b = b;
        this.invA2 = 1.0 / (a * a);
        this.invB2 = 1.0 / (b * b);
        this.ab = a * b;
        this.invRefractionIndex = 1.0 / refractionIndex;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = y - cy;
        double x2 = dx * dx;
        double y2 = dy * dy;

        double r2 = x2 * invA2 + y2 * invB2; // x²/a² + y²/b²
        if (r2 >= 1.0) {
            // the pixel is outside the ellipse => leave it undistorted
            out[0] = x;
            out[1] = y;
            return;
        }

        // height of the lens surface above the image plane
        double z2 = (1.0 - r2) * ab;
        double z = Math.sqrt(z2);

        out[0] = (float) (x - refractionTangent(dx, x2, z2, invRefractionIndex) * z);
        out[1] = (float) (y - refractionTangent(dy, y2, z2, invRefractionIndex) * z);
    }

    /**
     * Returns tan of the angular deflection a ray undergoes when refracting from
     * air (n = 1) into the lens material (n = refractionIndex), per Snell's law.
     */
    private static double refractionTangent(double coord, double coord2, double z2, double invRefractionIndex) {
        double sinIncidence = coord / Math.sqrt(coord2 + z2);
        double incidenceAngle = FastMath.asin(sinIncidence);
        double refractionAngle = FastMath.asin(sinIncidence * invRefractionIndex);
        return FastMath.tan(incidenceAngle - refractionAngle);
    }

    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
            new Ellipse2D.Double(cx - a, cy - b, 2 * a, 2 * b)
        };
    }
}
