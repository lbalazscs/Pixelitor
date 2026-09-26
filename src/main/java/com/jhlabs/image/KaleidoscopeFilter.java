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

import static com.jhlabs.image.ImageMath.INV_TAU;

/**
 * A filter that produces the effect of looking into a kaleidoscope:
 * a narrow angular wedge of the source image is repeatedly mirrored and
 * rotated around a center point to build a symmetric, tiled pattern.
 */
public class KaleidoscopeFilter extends TransformFilter {
    private final float angle;
    private final float rotation;

    private final double invSectorAngle; // sides / 2π

    // the center in pixel coordinates
    private final double cx;
    private final double cy;

    private final float zoom;

    /**
     * Constructs a KaleidoscopeFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param angle         the angle of the kaleidoscope.
     * @param rotation      the secondary angle of the kaleidoscope (rotates the result).
     * @param sides         the number of sides of the kaleidoscope.
     * @param center        the center of the effect in pixels.
     * @param zoom          the zoom factor applied to the kaleidoscope effect.
     */
    public KaleidoscopeFilter(String filterName, int edgeAction, int interpolation,
                              float angle, float rotation, int sides,
                              Point2D center, float zoom) {
        super(filterName, edgeAction, interpolation);

        this.angle = angle;
        this.rotation = rotation;
        this.invSectorAngle = sides * INV_TAU;
        this.cx = center.getX();
        this.cy = center.getY();
        this.zoom = zoom;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // polar coordinates
        double dx = x - cx;
        double dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double rawTheta = FastMath.atan2(dy, dx) - angle - rotation;

        // create kaleidoscope effect by repeating angular segments
        double foldedTheta = ImageMath.triangle(rawTheta * invSectorAngle);

        double finalTheta = foldedTheta + angle; // apply final rotation
        double zoomedR = r / zoom; // apply final zooming

        // convert back to Cartesian coordinates
        out[0] = (float) (cx + zoomedR * FastMath.cos(finalTheta));
        out[1] = (float) (cy + zoomedR * FastMath.sin(finalTheta));
    }
}
