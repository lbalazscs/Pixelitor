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

import java.awt.geom.Point2D;

import static com.jhlabs.image.ImageMath.HALF_PI;

/**
 * A filter which performs a swirl-and-pinch distortion effect.
 */
public class PinchFilter extends TransformFilter {
    private final float angle;
    private final float radiusSq;
    private final float pinchBulgeAmount;

    private final float cx;
    private final float cy;

    private final float zoom;
    private final float rotation;

    private final float cosR;
    private final float sinR;
    private final boolean outsideIsIdentity;

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
     * @param rotation          additional rotation applied to the result in radians
     */
    public PinchFilter(String filterName, int edgeAction, int interpolation,
                       float swirlAmount, float pinchBulgeAmount, float radius,
                       Point2D center, float zoom, float rotation) {
        super(filterName, edgeAction, interpolation);

        this.angle = -swirlAmount;
        this.pinchBulgeAmount = -pinchBulgeAmount;

        this.radiusSq = radius * radius;

        this.cx = (float) center.getX();
        this.cy = (float) center.getY();

        this.zoom = zoom;
        this.rotation = rotation;

        this.cosR = (float) (Math.cos(rotation) / zoom);
        this.sinR = (float) (Math.sin(rotation) / zoom);
        this.outsideIsIdentity = zoom == 1.0f && rotation == 0.0f;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        float distanceSq = dx * dx + dy * dy;

        if (distanceSq > radiusSq || distanceSq == 0) { // outside the radius
            if (outsideIsIdentity) { // fast path for default settings
                out[0] = x;
                out[1] = y;
            } else {
                // only zoom and rotate via 2D rotation matrix
                out[0] = cx + (cosR * dx - sinR * dy);
                out[1] = cy + (sinR * dx + cosR * dy);
            }
        } else {
            float scaledDist = (float) Math.sqrt(distanceSq / radiusSq);

            if (pinchBulgeAmount != 0) {
                // pinch-bulge
                float factor = (float) FastMath.pow(
                    FastMath.sin(HALF_PI * scaledDist),
                    -pinchBulgeAmount
                );
                dx *= factor;
                dy *= factor;
            }

            // twirl
            float falloff = 1 - scaledDist;
            float a = angle * falloff * falloff;

            a += rotation;

            float sin = (float) FastMath.sin(a);
            float cos = (float) FastMath.cos(a);

            float srcDx = (cos * dx - sin * dy) / zoom;
            float srcDy = (sin * dx + cos * dy) / zoom;

            out[0] = cx + srcDx;
            out[1] = cy + srcDy;
        }
    }
}
