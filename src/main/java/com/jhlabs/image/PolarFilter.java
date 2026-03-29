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

import java.awt.image.BufferedImage;

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
     * Invert the image in a circle.
     */
    public static final int INVERT_IN_CIRCLE = 2;

    private final int type;
    private final float zoom;
    private final float angle;
    private final float relativeCenterX;
    private final float relativeCenterY;

    private float width, height;
    private float centerX, centerY;
    private float radius;

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
     * @param relativeCenterX the horizontal center of the transformation as a fraction of the image width (0.0–1.0).
     * @param relativeCenterY the vertical center of the transformation as a fraction of the image height (0.0–1.0).
     */
    public PolarFilter(String filterName, int edgeAction, int interpolation,
                       int type, float zoom, double angle,
                       float relativeCenterX, float relativeCenterY) {
        super(filterName, edgeAction, interpolation);

        this.type = type;
        this.zoom = zoom;
        this.angle = (float) angle;
        this.relativeCenterX = relativeCenterX;
        this.relativeCenterY = relativeCenterY;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        width = src.getWidth();
        height = src.getHeight();

        centerX = width * relativeCenterX;
        centerY = height * relativeCenterY;

        radius = Math.max(centerY, centerX);
        return super.filter(src, dst);
    }

    private static float sqr(float x) {
        return x * x;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float theta;
        float r = 0;

        switch (type) {
            case RECT_TO_POLAR:
                theta = 0;
                if (x >= centerX) {
                    if (y > centerY) {
                        theta = ImageMath.PI - (float) FastMath.atan((x - centerX) / (y - centerY));
                        r = (float) Math.sqrt(sqr(x - centerX) + sqr(y - centerY));
                    } else if (y < centerY) {
                        theta = (float) FastMath.atan((x - centerX) / (centerY - y));
                        r = (float) Math.sqrt(sqr(x - centerX) + sqr(centerY - y));
                    } else {
                        theta = ImageMath.HALF_PI;
                        r = x - centerX;
                    }
                } else if (x < centerX) {
                    if (y < centerY) {
                        theta = ImageMath.TWO_PI - (float) FastMath.atan((centerX - x) / (centerY - y));
                        r = (float) Math.sqrt(sqr(centerX - x) + sqr(centerY - y));
                    } else if (y > centerY) {
                        theta = ImageMath.PI + (float) FastMath.atan((centerX - x) / (y - centerY));
                        r = (float) Math.sqrt(sqr(centerX - x) + sqr(y - centerY));
                    } else {
                        theta = 1.5f * ImageMath.PI;
                        r = centerX - x;
                    }
                }
                theta += angle;
                r /= zoom;

                out[0] = (width - 1) - (((width - 1) / ImageMath.TWO_PI) * theta);
                out[1] = height * r / radius;

                break;
            case POLAR_TO_RECT:
                theta = x / width * ImageMath.TWO_PI;
                theta += angle;

                float theta2;

                if (theta >= 1.5f * ImageMath.PI) {
                    theta2 = ImageMath.TWO_PI - theta;
                } else if (theta >= ImageMath.PI) {
                    theta2 = theta - ImageMath.PI;
                } else if (theta >= 0.5f * ImageMath.PI) {
                    theta2 = ImageMath.PI - theta;
                } else {
                    theta2 = theta;
                }
                r = radius * y / height;
                r /= zoom;

                float nx = -r * (float) FastMath.sin(theta2);
                float ny = r * (float) FastMath.cos(theta2);

                if (theta >= 1.5f * ImageMath.PI) {
                    out[0] = centerX - nx;
                    out[1] = centerY - ny;
                } else if (theta >= Math.PI) {
                    out[0] = centerX - nx;
                    out[1] = centerY + ny;
                } else if (theta >= 0.5 * Math.PI) {
                    out[0] = centerX + nx;
                    out[1] = centerY + ny;
                } else {
                    out[0] = centerX + nx;
                    out[1] = centerY - ny;
                }
                break;
            case INVERT_IN_CIRCLE:
                float dx = x - centerX;
                float dy = y - centerY;
                float distance2 = dx * dx + dy * dy;

                float relX = centerX * centerX * dx / distance2;
                float relY = centerY * centerY * dy / distance2;

                relX *= zoom;
                relY *= zoom;

                out[0] = centerX + relX;
                out[1] = centerY + relY;

                out[0] -= (width - 1) / ImageMath.TWO_PI * angle;

                break;
        }
    }
}
