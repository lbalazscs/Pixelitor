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

/**
 * A filter which distorts an image by performing coordinate conversions between rectangular and polar coordinates.
 */
public class PolarFilter extends TransformFilter {
    /**
     * Convert from rectangular to polar coordinates.
     */
    public static final int RECT_TO_POLAR = 0;

    /**
     * Convert from polar to rectangular coordinates.
     */
    public static final int POLAR_TO_RECT = 1;

    /**
     * Invert the image in a circle/ellipse.
     */
    public static final int INVERT_IN_CIRCLE = 2;

    private final int type;
    private final float zoom;
    private final float angle;

    private final float cx, cy;
    private final float radius;

    /**
     * Constructs a PolarFilter.
     *
     * @param filterName     the name of the filter.
     * @param edgeAction     the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation  the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param type           the distortion type: {@link #RECT_TO_POLAR}, {@link #POLAR_TO_RECT},
     *                       or {@link #INVERT_IN_CIRCLE}.
     * @param zoom           the zoom factor applied during the coordinate transformation.
     * @param angle          the rotation angle (in radians) applied during the transformation.
     * @param center         the center of the transformation in pixels.
     */
    public PolarFilter(String filterName, int edgeAction, int interpolation,
                       int type, float zoom, double angle, Point2D center) {
        super(filterName, edgeAction, interpolation);

        this.type = type;
        this.zoom = zoom;
        this.angle = (float) angle;

        this.cx = (float) center.getX();
        this.cy = (float) center.getY();
        this.radius = Math.max(cy, cx);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        switch (type) {
            case RECT_TO_POLAR:
                rectToPolar(x, y, out);
                break;
            case POLAR_TO_RECT:
                polarToRect(x, y, out);
                break;
            case INVERT_IN_CIRCLE:
                invertInCircle(x, y, out);
                break;
        }
    }

    private void rectToPolar(int x, int y, float[] out) {
        float theta = 0;
        float r = 0;
        if (x >= cx) {
            if (y > cy) {
                theta = ImageMath.PI - (float) FastMath.atan((x - cx) / (y - cy));
                r = fastHypot(x - cx, y - cy);
            } else if (y < cy) {
                theta = (float) FastMath.atan((x - cx) / (cy - y));
                r = fastHypot(x - cx, y - cy);
            } else {
                theta = ImageMath.HALF_PI;
                r = x - cx;
            }
        } else if (x < cx) {
            if (y < cy) {
                theta = ImageMath.TWO_PI - (float) FastMath.atan((cx - x) / (cy - y));
                r = fastHypot(x - cx, y - cy);
            } else if (y > cy) {
                theta = ImageMath.PI + (float) FastMath.atan((cx - x) / (y - cy));
                r = fastHypot(x - cx, y - cy);
            } else {
                theta = 1.5f * ImageMath.PI;
                r = cx - x;
            }
        }
        theta += angle;
        r /= zoom;

        out[0] = (width - 1) - (((width - 1) / ImageMath.TWO_PI) * theta);
        out[1] = height * r / radius;
    }

    private void polarToRect(int x, int y, float[] out) {
        float theta;
        float r;
        theta = (float) x / width * ImageMath.TWO_PI;
        theta += angle;

        float refAngle;

        if (theta >= 1.5f * ImageMath.PI) {
            refAngle = ImageMath.TWO_PI - theta;
        } else if (theta >= ImageMath.PI) {
            refAngle = theta - ImageMath.PI;
        } else if (theta >= 0.5f * ImageMath.PI) {
            refAngle = ImageMath.PI - theta;
        } else {
            refAngle = theta;
        }
        r = radius * y / height;
        r /= zoom;

        float dx = -r * (float) FastMath.sin(refAngle);
        float dy = r * (float) FastMath.cos(refAngle);

        if (theta >= 1.5f * ImageMath.PI) {
            out[0] = cx - dx;
            out[1] = cy - dy;
        } else if (theta >= Math.PI) {
            out[0] = cx - dx;
            out[1] = cy + dy;
        } else if (theta >= 0.5 * Math.PI) {
            out[0] = cx + dx;
            out[1] = cy + dy;
        } else {
            out[0] = cx + dx;
            out[1] = cy - dy;
        }
    }

    private void invertInCircle(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        float distSq = dx * dx + dy * dy;

        float relX = cx * cx * dx / distSq;
        float relY = cy * cy * dy / distSq;

        relX *= zoom;
        relY *= zoom;

        out[0] = cx + relX;
        out[1] = cy + relY;

        out[0] -= (width - 1) / ImageMath.TWO_PI * angle;
    }

    private static float fastHypot(float dx, float dy) {
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
