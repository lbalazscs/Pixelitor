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
 * A filter which wraps an image around a circular arc.
 */
public class CircleFilter extends TransformFilter {
    private final float radius;
    private final float arcHeight;
    private final float angle;
    private final float spreadAngle;
    private final float cx;
    private final float cy;

    private float imgWidth;
    private float imgHeight;

    /**
     * Constructs a CircleFilter.
     *
     * @param filterName  the name of the filter.
     * @param edgeAction  the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param radius      the radius of the effect (must be >= 0).
     * @param arcHeight   the height of the arc.
     * @param angle       the angle of the arc.
     * @param spreadAngle the spread angle of the arc.
     * @param center      the center of the effect in image pixels.
     */
    public CircleFilter(String filterName,
                        int edgeAction, int interpolation,
                        float radius, float arcHeight,
                        float angle, float spreadAngle,
                        Point2D center) {
        super(filterName, edgeAction, interpolation);

        this.radius = radius;
        this.arcHeight = arcHeight;
        this.angle = angle;
        this.spreadAngle = spreadAngle;
        this.cx = (float) center.getX();
        this.cy = (float) center.getY();
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        imgWidth = src.getWidth();
        imgHeight = src.getHeight();
        imgWidth--;
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        float theta = (float) FastMath.atan2(-dy, -dx) + angle;
        float r = (float) Math.sqrt(dx * dx + dy * dy);

        theta = ImageMath.mod(theta, 2 * (float) Math.PI);

        out[0] = imgWidth * theta / (spreadAngle + 0.00001f);
        out[1] = imgHeight * (1 - (r - radius) / (arcHeight + 0.00001f));
    }
}
