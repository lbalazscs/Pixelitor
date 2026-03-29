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
    private final double a;
    private final double b;
    private final double a2;
    private final double b2;
    private final double refractionIndex;

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
        this.a2 = a * a;
        this.b2 = b * b;
        this.refractionIndex = refractionIndex;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = y - cy;
        double x2 = dx * dx;
        double y2 = dy * dy;
        if (y2 >= (b2 - (b2 * x2) / a2)) {
            out[0] = x;
            out[1] = y;
        } else {
            double rRefraction = 1.0f / refractionIndex;

            double z2 = (1.0f - x2 / a2 - y2 / b2) * (a * b);
            double z = Math.sqrt(z2);

            double xAngle = FastMath.acos(dx / Math.sqrt(x2 + z2));
            double angle1 = Math.PI / 2.0 - xAngle;
            double angle2 = FastMath.asin(FastMath.sin(angle1) * rRefraction);
            angle2 = Math.PI / 2.0 - xAngle - angle2;
            out[0] = (float) (x - FastMath.tan(angle2) * z);

            double yAngle = FastMath.acos(dy / Math.sqrt(y2 + z2));
            angle1 = Math.PI / 2.0 - yAngle;
            angle2 = FastMath.asin(FastMath.sin(angle1) * rRefraction);
            angle2 = Math.PI / 2.0 - yAngle - angle2;
            out[1] = (float) (y - FastMath.tan(angle2) * z);
        }
    }

    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
            new Ellipse2D.Double(cx - a, cy - b, 2 * a, 2 * b)
        };
    }
}
