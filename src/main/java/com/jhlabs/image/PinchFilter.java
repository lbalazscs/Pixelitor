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
 * A filter which performs the popular whirl-and-pinch distortion effect.
 */
public class PinchFilter extends TransformFilter {
    private final float angle;
    private final float radius;
    private final float radius2;
    private final float pinchBulgeAmount;

    private final float cx;
    private final float cy;

    private final float zoom;
    private final float rotateResultAngle;

    /**
     * Constructs a PinchFilter with all parameters specified.
     *
     * @param filterName        the name of the filter
     * @param edgeAction        the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT)
     * @param interpolation     the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC)
     * @param swirlAmount       the angle of twirl in radians. 0 means no distortion.
     *                          This is the angle by which pixels at the nearest edge of the image will move.
     * @param pinchBulgeAmount  the amount of pinch/bulge in the range [-1, 1]
     * @param radius            the radius of the effect (must be >= 0)
     * @param center            the center of the effect in pixels
     * @param zoom              the zoom factor (1 = no zoom)
     * @param rotateResultAngle additional rotation applied to the result in radians
     */
    public PinchFilter(String filterName, int edgeAction, int interpolation,
                       float swirlAmount, float pinchBulgeAmount, float radius,
                       Point2D center, float zoom, float rotateResultAngle) {
        super(filterName, edgeAction, interpolation);

        this.angle = -swirlAmount;
        this.pinchBulgeAmount = -pinchBulgeAmount;

        this.radius = radius;
        this.radius2 = radius * radius;

        this.cx = (float) center.getX();
        this.cy = (float) center.getY();

        this.zoom = zoom;
        this.rotateResultAngle = rotateResultAngle;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        float distance = dx * dx + dy * dy;

        if (distance > radius2 || distance == 0) {
            double theta = FastMath.atan2(dy, dx);
            theta += rotateResultAngle;
            double r = Math.sqrt(distance);
            double zoomedR = r / zoom;
            float u = (float) (zoomedR * FastMath.cos(theta));
            float v = (float) (zoomedR * FastMath.sin(theta));

            out[0] = (u + cx);
            out[1] = (v + cy);
        } else {
            float scaledDist = (float) Math.sqrt(distance / radius2);
            float pinchBulgeFactor = (float) FastMath.pow(
                FastMath.sin(Math.PI * 0.5 * scaledDist),
                -pinchBulgeAmount
            );

            // pinch-bulge
            dx *= pinchBulgeFactor;
            dy *= pinchBulgeFactor;

            // twirl
            float e = 1 - scaledDist;
            float a = angle * e * e;

            a += rotateResultAngle;

            float sin = (float) FastMath.sin(a);
            float cos = (float) FastMath.cos(a);

            float u = (cos * dx - sin * dy) / zoom;
            float v = (sin * dx + cos * dy) / zoom;

            out[0] = cx + u;
            out[1] = cy + v;
        }
    }

    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
            new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius)
        };
    }
}
