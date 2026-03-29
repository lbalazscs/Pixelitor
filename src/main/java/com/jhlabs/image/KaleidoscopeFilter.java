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
import java.awt.image.BufferedImage;

/**
 * A Filter which produces the effect of looking into a kaleidoscope.
 */
public class KaleidoscopeFilter extends TransformFilter {
    private final float angle;
    private final float angle2;
    private final int sides;

    // the center in relative coordinates
    private final double relCx;
    private final double relCy;

    // the center in pixel coordinates
    private double cx;
    private double cy;

    private final float zoom;

    /**
     * Constructs a KaleidoscopeFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param angle         the angle of the kaleidoscope.
     * @param angle2        the secondary angle of the kaleidoscope (rotates the result).
     * @param sides         the number of sides of the kaleidoscope (must be >= 2).
     * @param center        the center of the effect as a proportion of the image size.
     * @param zoom          the zoom factor applied to the kaleidoscope effect.
     */
    public KaleidoscopeFilter(String filterName, int edgeAction, int interpolation,
                              float angle, float angle2, int sides,
                              Point2D center, float zoom) {
        super(filterName, edgeAction, interpolation);

        this.angle = angle;
        this.angle2 = angle2;
        this.sides = sides;
        this.relCx = center.getX();
        this.relCy = center.getY();
        this.zoom = zoom;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        cx = src.getWidth() * relCx;
        cy = src.getHeight() * relCy;
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // polar coordinates
        double dx = x - cx;
        double dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double theta = FastMath.atan2(dy, dx) - angle - angle2;

        // create kaleidoscope effect by repeating angular segments
        theta = ImageMath.triangle((float) (theta / Math.PI * sides * 0.5));

        theta += angle; // apply final rotation
        double zoomedR = r / zoom; // apply final zooming

        // convert back to cartesian coordinates
        out[0] = (float) (cx + zoomedR * FastMath.cos(theta));
        out[1] = (float) (cy + zoomedR * FastMath.sin(theta));
    }
}
