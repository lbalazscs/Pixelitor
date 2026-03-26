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
    private float radius = 10;
    private float arcHeight = 20;
    private float angle = 0;
    private float spreadAngle = (float) Math.PI;

    private float cx;
    private float cy;
    private float imgWidth;
    private float imgHeight;

    /**
     * Constructs a CircleFilter.
     */
    public CircleFilter(String filterName) {
        super(filterName);
        setEdgeAction(TRANSPARENT);
    }

    /**
     * Sets the height of the arc.
     *
     * @param arcHeight the height
     */
    public void setArcHeight(float arcHeight) {
        this.arcHeight = arcHeight;
    }

    /**
     * Sets the angle of the arc.
     *
     * @param angle the angle of the arc.
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Sets the spread angle of the arc.
     *
     * @param spreadAngle the angle
     */
    public void setSpreadAngle(float spreadAngle) {
        this.spreadAngle = spreadAngle;
    }

    /**
     * Sets the radius of the effect.
     *
     * @param radius the radius (must be >= 0)
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Sets the center of the effect in image pixels.
     *
     * @param center the center
     */
    public void setCenter(Point2D center) {
        cx = (float) center.getX();
        cy = (float) center.getY();
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
