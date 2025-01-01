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
    private float angle = 0;
    private float angle2 = 0;
    private int sides = 3;

    // the center in relative coordinates
    private double relCx = 0.5f;
    private double relCy = 0.5f;

    // the center in pixel coordinates
    private double cx;
    private double cy;

    private float zoom;

    /**
     * Construct a KaleidoscopeFilter with no distortion.
     */
    public KaleidoscopeFilter(String filterName) {
        super(filterName);
        setEdgeAction(REPEAT_EDGE);
    }

    /**
     * Set the number of sides of the kaleidoscope.
     *
     * @param sides the number of sides
     * @min-value 2
     */
    public void setSides(int sides) {
        this.sides = sides;
    }

    /**
     * Set the angle of the kaleidoscope.
     *
     * @param angle the angle of the kaleidoscope.
     * @angle
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Set the secondary angle of the kaleidoscope.
     *
     * @param angle2 the angle
     * @angle
     */
    public void setAngle2(float angle2) {
        this.angle2 = angle2;
    }

    /**
     * Set the center of the effect as a proportion of the image size.
     *
     * @param center the center
     */
    public void setCenter(Point2D center) {
        relCx = center.getX();
        relCy = center.getY();
    }

    public void setZoom(float zoom) {
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

    @Override
    public String toString() {
        return "Distort/Kaleidoscope...";
    }
}
