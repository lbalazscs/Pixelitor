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
    private float height = 20;
    private float angle = 0;
    private float spreadAngle = (float) Math.PI;
    private float centerX = 0.5f;
    private float centerY = 0.5f;

    private float icenterX;
    private float icenterY;
    private float iWidth;
    private float iHeight;

    /**
     * Construct a CircleFilter.
     */
    public CircleFilter(String filterName) {
        super(filterName);
        setEdgeAction(TRANSPARENT);
    }

    /**
     * Set the height of the arc.
     *
     * @param height the height
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * Set the angle of the arc.
     *
     * @param angle the angle of the arc.
     * @angle
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Set the spread angle of the arc.
     *
     * @param spreadAngle the angle
     * @angle
     */
    public void setSpreadAngle(float spreadAngle) {
        this.spreadAngle = spreadAngle;
    }

    /**
     * Set the radius of the effect.
     *
     * @param radius the radius
     * @min-value 0
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Set the center of the effect in the Y direction as a proportion of the image size.
     *
     * @param centerX the center
     */
    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    /**
     * Set the center of the effect in the Y direction as a proportion of the image size.
     *
     * @param centerY the center
     */
    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    /**
     * Set the center of the effect as a proportion of the image size.
     *
     * @param center the center
     */
    public void setCenter(Point2D center) {
        centerX = (float) center.getX();
        centerY = (float) center.getY();
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        iWidth = src.getWidth();
        iHeight = src.getHeight();
        icenterX = iWidth * centerX;
        icenterY = iHeight * centerY;
        iWidth--;
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - icenterX;
        float dy = y - icenterY;
        float theta = (float) FastMath.atan2(-dy, -dx) + angle;
        float r = (float) Math.sqrt(dx * dx + dy * dy);

        theta = ImageMath.mod(theta, 2 * (float) Math.PI);

        out[0] = iWidth * theta / (spreadAngle + 0.00001f);
        out[1] = iHeight * (1 - (r - radius) / (height + 0.00001f));
    }

    @Override
    public String toString() {
        return "Distort/Circle...";
    }
}
