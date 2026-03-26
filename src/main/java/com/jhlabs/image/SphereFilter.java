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
    private double a = 0;
    private double b = 0;
    private double a2 = 0;
    private double b2 = 0;
    private double refractionIndex = 1.5f;

    private double cx;
    private double cy;

    public SphereFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the index of refraction.
     *
     * @param refractionIndex the index of refraction
     */
    public void setRefractionIndex(double refractionIndex) {
        this.refractionIndex = refractionIndex;
    }

    /**
     * Sets the center of the effect in pixels
     *
     * @param center the center
     */
    public void setCenter(Point2D center) {
        cx = center.getX();
        cy = center.getY();
    }

    // sets the horizontal radius
    public void setA(double a) {
        this.a = a;
        this.a2 = a * a;
    }

    // sets the vertical radius
    public void setB(double b) {
        this.b = b;
        this.b2 = b * b;
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
