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
 * A filter which distorts and image by performing coordinate conversions between rectangular and polar coordinates.
 */
public class PolarFilter extends TransformFilter {

    private float zoom;
    private float angle;

    /**
     * Convert from rectangular to polar coordinates.
     */
    public final static int RECT_TO_POLAR = 0;

    /**
     * Convert from polar to rectangular coordinates.
     */
    public final static int POLAR_TO_RECT = 1;

    /**
     * Invert the image in a circle.
     */
    public final static int INVERT_IN_CIRCLE = 2;

    private int type;
    private float width, height;
    private float centreX, centreY;
    private float radius;

    private float relativeCentreX = 0.5f;
    private float relativeCentreY = 0.5f;


    /**
     * Construct a PolarFilter.
     */
    public PolarFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        this.width = src.getWidth();
        this.height = src.getHeight();

        centreX = width * relativeCentreX;
        centreY = height * relativeCentreY;

        radius = Math.max(centreY, centreX);
        return super.filter(src, dst);
    }

    /**
     * Set the distortion type.
     *
     * @param type the distortion type
     * @see #getType
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Get the distortion type.
     *
     * @return the distortion type
     * @see #setType
     */
    public int getType() {
        return type;
    }

    private static float sqr(float x) {
        return x * x;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float theta, t;
        float m, xmax, ymax;
        float r = 0;

        switch (type) {
            case RECT_TO_POLAR:
                theta = 0;
                if (x >= centreX) {
                    if (y > centreY) {
                        theta = ImageMath.PI - (float) FastMath.atan((x - centreX) / (y - centreY));
                        r = (float) Math.sqrt(sqr(x - centreX) + sqr(y - centreY));
                    } else if (y < centreY) {
                        theta = (float) FastMath.atan((x - centreX) / (centreY - y));
                        r = (float) Math.sqrt(sqr(x - centreX) + sqr(centreY - y));
                    } else {
                        theta = ImageMath.HALF_PI;
                        r = x - centreX;
                    }
                } else if (x < centreX) {
                    if (y < centreY) {
                        theta = ImageMath.TWO_PI - (float) FastMath.atan((centreX - x) / (centreY - y));
                        r = (float) Math.sqrt(sqr(centreX - x) + sqr(centreY - y));
                    } else if (y > centreY) {
                        theta = ImageMath.PI + (float) FastMath.atan((centreX - x) / (y - centreY));
                        r = (float) Math.sqrt(sqr(centreX - x) + sqr(y - centreY));
                    } else {
                        theta = 1.5f * ImageMath.PI;
                        r = centreX - x;
                    }
                }
/* lbalazscs: commented out because not used
                if (x != centreX)
                    m = Math.abs(((float) (y - centreY)) / ((float) (x - centreX)));
                else
                    m = 0;

                if (m <= ((float) height / (float) width)) {
                    if (x == centreX) {
                        xmax = 0;
                        ymax = centreY;
                    } else {
                        xmax = centreX;
                        ymax = m * xmax;
                    }
                } else {
                    ymax = centreY;
                    xmax = ymax / m;
                }
*/
                theta += angle;

                r /= zoom;

//                out[0] = (width - 1) - (width - 1) / ImageMath.TWO_PI * theta;
//                out[1] = height * r / radius;

                out[0] = (width - 1) - (((width - 1) / (ImageMath.TWO_PI)) * theta);
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
 /*
 lbalazscs: commented out because not used
                t = (float) Math.tan(theta2);
                if (t != 0)
                    m = 1.0f / t;
                else
                    m = 0;

                if (m <= ((float) (height) / (float) (width))) {
                    if (theta2 == 0) {
                        xmax = 0;
                        ymax = centreY;
                    } else {
                        xmax = centreX;
                        ymax = m * xmax;
                    }
                } else {
                    ymax = centreY;
                    xmax = ymax / m;
                }
  */
                r = radius * y / height;
                r /= zoom;

                float nx = -r * (float) FastMath.sin(theta2) ;
                float ny = r * (float) FastMath.cos(theta2);

                if (theta >= 1.5f * ImageMath.PI) {
                    out[0] = centreX - nx;
                    out[1] = centreY - ny;
                } else if (theta >= Math.PI) {
                    out[0] = centreX - nx;
                    out[1] = centreY + ny;
                } else if (theta >= 0.5 * Math.PI) {
                    out[0] = centreX + nx;
                    out[1] = centreY + ny;
                } else {
                    out[0] = centreX + nx;
                    out[1] = centreY - ny;
                }
                break;
            case INVERT_IN_CIRCLE:
                float dx = x - centreX;
                float dy = y - centreY;
                float distance2 = dx * dx + dy * dy;

                float relX = centreX * centreX * dx / distance2;
                float relY = centreY * centreY * dy / distance2;

                relX *= zoom;
                relY *= zoom;

                out[0] = centreX + relX;
                out[1] = centreY + relY;

                // lbalazscs: interesting effect...
                out[0] -= (width - 1) / ImageMath.TWO_PI * angle;

                break;
        }

//        out[0] *= divideFactor;
//        out[1] *= divideFactor;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setRelativeCentreX(float relativeCentreX) {
        this.relativeCentreX = relativeCentreX;
    }

    public void setRelativeCentreY(float relativeCentreY) {
        this.relativeCentreY = relativeCentreY;
    }

    public float getRelativeCentreX() {
        return relativeCentreX;
    }

    public float getRelativeCentreY() {
        return relativeCentreY;
    }

    public String toString() {
		return "Distort/Polar Coordinates...";
	}

    public void setAngle(double angle) {
        this.angle = (float) angle;
    }
}
