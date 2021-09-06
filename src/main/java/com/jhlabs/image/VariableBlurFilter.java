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

import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;

/**
 * A filter which performs a box blur with a different blur radius at each pixel. The radius can either be specified by
 * providing a blur mask image or by overriding the blurRadiusAt method.
 */
public class VariableBlurFilter extends AbstractBufferedImageOp {
    private float hRadius = 1;
    private float vRadius = 1;
    private int iterations = 1;
    private BufferedImage blurMask;
    private boolean premultiplyAlpha = true;

    public VariableBlurFilter(String filterName) {
        super(filterName);
    }

    /**
     * Set whether to premultiply the alpha channel.
     *
     * @param premultiplyAlpha true to premultiply the alpha
     * @see #getPremultiplyAlpha
     */
    public void setPremultiplyAlpha(boolean premultiplyAlpha) {
        this.premultiplyAlpha = premultiplyAlpha;
    }

    /**
     * Get whether to premultiply the alpha channel.
     *
     * @return true to premultiply the alpha
     * @see #setPremultiplyAlpha
     */
    public boolean getPremultiplyAlpha() {
        return premultiplyAlpha;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(iterations * (width + height));

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inPixels);

        if (premultiplyAlpha) {
            ImageMath.premultiply(inPixels, 0, inPixels.length);
        }

        for (int i = 0; i < iterations; i++) {
            blur(inPixels, outPixels, width, height, hRadius, 1, pt);
            blur(outPixels, inPixels, height, width, vRadius, 2, pt);
        }

        if (premultiplyAlpha) {
            ImageMath.unpremultiply(inPixels, 0, inPixels.length);
        }

        setRGB(dst, 0, 0, width, height, inPixels);

        finishProgressTracker();

        return dst;
    }

    public void blur(int[] in, int[] out, int width, int height, float radius, int pass, ProgressTracker pt) {
        int widthMinus1 = width - 1;
        int[] r = new int[width];
        int[] g = new int[width];
        int[] b = new int[width];
        int[] a = new int[width];
        int[] mask = new int[width];

        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;

            if (blurMask != null) {
                if (pass == 1) {
                    getRGB(blurMask, 0, y, width, 1, mask);
                } else {
                    getRGB(blurMask, y, 0, 1, width, mask);
                }
            }

            for (int x = 0; x < width; x++) {
                int argb = in[inIndex + x];
                a[x] = (argb >> 24) & 0xff;
                r[x] = (argb >> 16) & 0xff;
                g[x] = (argb >> 8) & 0xff;
                b[x] = argb & 0xff;
                if (x != 0) {
                    a[x] += a[x - 1];
                    r[x] += r[x - 1];
                    g[x] += g[x - 1];
                    b[x] += b[x - 1];
                }
            }

            for (int x = 0; x < width; x++) {
                // Get the blur radius at x, y
                int ra;
                if (blurMask != null) {
                    if (pass == 1) {
                        ra = (int) ((mask[x] & 0xff) * hRadius / 255.0f);
                    } else {
                        ra = (int) ((mask[x] & 0xff) * vRadius / 255.0f);
                    }
                } else {
                    if (pass == 1) {
                        ra = (int) (blurRadiusAt(x, y) * hRadius);
                    } else {
                        ra = (int) (blurRadiusAt(y, x) * vRadius);
                    }
                }

                int divisor = 2 * ra + 1;
                int ta = 0, tr = 0, tg = 0, tb = 0;
                int i1 = x + ra;
                if (i1 > widthMinus1) {
                    int f = i1 - widthMinus1;
                    int l = widthMinus1;
                    ta += (a[l] - a[l - 1]) * f;
                    tr += (r[l] - r[l - 1]) * f;
                    tg += (g[l] - g[l - 1]) * f;
                    tb += (b[l] - b[l - 1]) * f;
                    i1 = widthMinus1;
                }
                int i2 = x - ra - 1;
                if (i2 < 0) {
                    ta -= a[0] * i2;
                    tr -= r[0] * i2;
                    tg -= g[0] * i2;
                    tb -= b[0] * i2;
                    i2 = 0;
                }

                ta += a[i1] - a[i2];
                tr += r[i1] - r[i2];
                tg += g[i1] - g[i2];
                tb += b[i1] - b[i2];
                out[outIndex] = ((ta / divisor) << 24) | ((tr / divisor) << 16) | ((tg / divisor) << 8) | (tb / divisor);

                outIndex += height;
            }
            inIndex += width;

            pt.unitDone();
        }
    }

    /**
     * Override this to get a different blur radius at each point.
     *
     * @param width  the width of the image
     * @param height the height of the image
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @return the blur radius
     */
    protected float blurRadiusAt(int x, int y) {
        return 0.0f;
    }

    /**
     * Set the horizontal size of the blur.
     *
     * @param hRadius the radius of the blur in the horizontal direction
     * @min-value 0
     * @see #getHRadius
     */
    public void setHRadius(float hRadius) {
        this.hRadius = hRadius;
    }

    /**
     * Get the horizontal size of the blur.
     *
     * @return the radius of the blur in the horizontal direction
     * @see #setHRadius
     */
    public float getHRadius() {
        return hRadius;
    }

    /**
     * Set the vertical size of the blur.
     *
     * @param vRadius the radius of the blur in the vertical direction
     * @min-value 0
     * @see #getVRadius
     */
    public void setVRadius(float vRadius) {
        this.vRadius = vRadius;
    }

    /**
     * Get the vertical size of the blur.
     *
     * @return the radius of the blur in the vertical direction
     * @see #setVRadius
     */
    public float getVRadius() {
        return vRadius;
    }

    /**
     * Set the radius of the effect.
     *
     * @param radius the radius
     * @min-value 0
     * @see #getRadius
     */
    public void setRadius(float radius) {
        hRadius = vRadius = radius;
    }

    /**
     * Get the radius of the effect.
     *
     * @return the radius
     * @see #setRadius
     */
    public float getRadius() {
        return hRadius;
    }

    /**
     * Set the number of iterations the blur is performed.
     *
     * @param iterations the number of iterations
     * @min-value 0
     * @see #getIterations
     */
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    /**
     * Get the number of iterations the blur is performed.
     *
     * @return the number of iterations
     * @see #setIterations
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Set the mask used to give the amount of blur at each point.
     *
     * @param blurMask the mask
     * @see #getBlurMask
     */
    public void setBlurMask(BufferedImage blurMask) {
        this.blurMask = blurMask;
    }

    /**
     * Get the mask used to give the amount of blur at each point.
     *
     * @return the mask
     * @see #setBlurMask
     */
    public BufferedImage getBlurMask() {
        return blurMask;
    }

    @Override
    public String toString() {
        return "Blur/Variable Blur...";
    }
}
