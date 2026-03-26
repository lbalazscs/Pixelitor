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

import com.jhlabs.math.FFT;
import net.jafama.FastMath;

import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A filter which uses FFTs to simulate lens blur on an image.
 */
public class LensBlurFilter extends AbstractBufferedImageOp {
    private final float radius;
    private final float bloom;
    private final float bloomThreshold;
    private final int sides;

    /**
     * Creates a new {@link LensBlurFilter} with the given parameters.
     *
     * @param filterName     the name of the filter
     * @param radius         the radius of the blur kernel in pixels; controls the amount of blur
     * @param sides          the number of sides of the aperture shape
     * @param bloom          the bloom factor applied to bright areas
     * @param bloomThreshold the threshold above which pixel values are considered for blooming
     */
    public LensBlurFilter(String filterName,
                          float radius,
                          int sides,
                          float bloom,
                          float bloomThreshold) {
        super(filterName);

        this.radius = radius;
        this.sides = sides;
        this.bloom = bloom;
        this.bloomThreshold = bloomThreshold;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();
        int rows = 1, cols = 1;
        int log2rows = 0, log2cols = 0;
        int iradius = (int) Math.ceil(radius);

        int tileWidth = iradius < 32 ? Math.min(128, width + 2 * iradius) : Math.min(256, width + 2 * iradius);
        int tileHeight = iradius < 32 ? Math.min(128, height + 2 * iradius) : Math.min(256, height + 2 * iradius);

        if (dst == null) {
            dst = new BufferedImage(width, height, TYPE_INT_ARGB);
        }

        while (rows < tileHeight) {
            rows *= 2;
            log2rows++;
        }
        while (cols < tileWidth) {
            cols *= 2;
            log2cols++;
        }

        // ensure that the tile dimensions are strictly greater than
        // 2 * iradius to prevent zero or negative loop step sizes
        while (rows <= 2 * iradius) {
            rows *= 2;
            log2rows++;
        }
        while (cols <= 2 * iradius) {
            cols *= 2;
            log2cols++;
        }

        int w = cols;
        int h = rows;

        tileWidth = w;
        tileHeight = h;//FIXME-tileWidth, w, and cols are always all the same

        FFT fft = new FFT(Math.max(log2rows, log2cols));

        int[] rgb = new int[w * h];
        float[][] gb = new float[2][w * h];
        float[][] ar = new float[2][w * h];

        float[][] mask = createKernel(w, h);

        // FFT-transform the the kernel upfront for efficiency 
        fft.transform2D(mask[0], mask[1], w, h, true);

        int stepY = tileHeight - 2 * iradius;
        int workUnits = (height + stepY - 1) / stepY;
        pt = createProgressTracker(workUnits);

        int[] tilePixels = new int[tileWidth * tileHeight];

        // the image is processed in overlapping tiles (each tile
        // extends iradius pixels beyond its useful region on all sides)
        for (int tileY = -iradius; tileY + iradius < height; tileY += tileHeight - 2 * iradius) {
            for (int tileX = -iradius; tileX + iradius < width; tileX += tileWidth - 2 * iradius) {
                // clip the tile to the image bounds
                int tx = tileX, ty = tileY, tw = tileWidth, th = tileHeight;
                int fx = 0, fy = 0;
                if (tx < 0) {
                    tw += tx;
                    fx -= tx;
                    tx = 0;
                }
                if (ty < 0) {
                    th += ty;
                    fy -= ty;
                    ty = 0;
                }
                if (tx + tw > width) {
                    tw = width - tx;
                }
                if (ty + th > height) {
                    th = height - ty;
                }

                getRGB(src, tx, ty, tw, th, tilePixels);
                for (int row = 0; row < th; row++) {
                    System.arraycopy(tilePixels, row * tw, rgb, (fy + row) * w + fx, tw);
                }

                // flatten 2D array lookups to avoid constant pointer dereferencing
                float[] ar0 = ar[0], ar1 = ar[1], gb0 = gb[0], gb1 = gb[1];

                // Create a float array from the pixels.
                // Any pixels off the edge of the source image get duplicated from the edge.
                int i = 0;
                for (int y = 0; y < h; y++) {
                    int imageY = y + tileY;
                    int j = (imageY < 0) ? fy : (imageY >= height ? fy + th - 1 : y);
                    j *= w;
                    for (int x = 0; x < w; x++) {
                        int imageX = x + tileX;
                        int k = (imageX < 0) ? fx : (imageX >= width ? fx + tw - 1 : x);
                        k += j;

                        // hoist color extraction & blooming logic
                        int pixel = rgb[k];
                        ar0[i] = pixel >>> 24;
                        float r = (pixel >> 16) & 0xFF;
                        float g = (pixel >> 8) & 0xFF;
                        float b = pixel & 0xFF;

                        if (r > bloomThreshold) {
                            r *= bloom;
                        }
                        if (g > bloomThreshold) {
                            g *= bloom;
                        }
                        if (b > bloomThreshold) {
                            b *= bloom;
                        }

                        ar1[i] = r;
                        gb0[i] = g;
                        gb1[i] = b;
                        i++;
                    }
                }

                // transform into frequency space
                fft.transform2D(ar[0], ar[1], cols, rows, true);
                fft.transform2D(gb[0], gb[1], cols, rows, true);

                // multiply the transformed pixels by the transformed kernel
                // (complex multiplication = convolution in spatial domain)
                for (int j = 0; j < ar[0].length; j++) {
                    float re = ar[0][j];
                    float im = ar[1][j];
                    float rem = mask[0][j];
                    float imm = mask[1][j];
                    ar[0][j] = re * rem - im * imm;
                    ar[1][j] = re * imm + im * rem;

                    re = gb[0][j];
                    im = gb[1][j];
                    gb[0][j] = re * rem - im * imm;
                    gb[1][j] = re * imm + im * rem;
                }

                // transform back
                fft.transform2D(ar[0], ar[1], cols, rows, false);
                fft.transform2D(gb[0], gb[1], cols, rows, false);

                // convert back to RGB pixels, with quadrant remapping
                int row_flip = h >> 1;
                int col_flip = w >> 1;
                int index = 0;

                //FIXME-don't bother converting pixels off image edges
                for (int y = 0; y < h; y++) {
                    int ym = y ^ row_flip;
                    int yi = ym * w;
                    for (int x = 0; x < w; x++) {
                        int xm = yi + (x ^ col_flip);

                        int a = (int) ar[0][xm];
                        int r = (int) ar[1][xm];
                        int g = (int) gb[0][xm];
                        int b = (int) gb[1][xm];

                        // clamp high pixels due to blooming
                        if (r > 255) {
                            r = 255;
                        }
                        if (g > 255) {
                            g = 255;
                        }
                        if (b > 255) {
                            b = 255;
                        }
                        int argb = (a << 24) | (r << 16) | (g << 8) | b;
                        rgb[index++] = argb;
                    }
                }

                // clip to the output image
                tx = tileX + iradius;
                ty = tileY + iradius;
                tw = tileWidth - 2 * iradius;
                th = tileHeight - 2 * iradius;
                if (tx + tw > width) {
                    tw = width - tx;
                }
                if (ty + th > height) {
                    th = height - ty;
                }

                for (int row = 0; row < th; row++) {
                    System.arraycopy(rgb, (iradius + row) * w + iradius, tilePixels, row * tw, tw);
                }
                setRGB(dst, tx, ty, tw, th, tilePixels);
            }
            pt.unitDone();
        }
        finishProgressTracker();

        return dst;
    }

    /**
     * Creates the blur kernel, a shape describing how each pixel spreads light onto its neighbors.
     */
    private float[][] createKernel(int w, int h) {
        float[][] kernel = new float[2][w * h];
        double polyAngle = Math.PI / sides;
        double polyScale = 1.0 / FastMath.cos(polyAngle);
        double r2 = radius * radius;
        float total = 0;
        int i = 0;
        for (int y = 0; y < h; y++) {
            double dy = y - h / 2.0;
            for (int x = 0; x < w; x++) {
                double dx = x - w / 2.0;
                double r2_current = dx * dx + dy * dy;
                double f = 0;
                if (r2_current < r2) {
                    double r = Math.sqrt(r2_current);
                    if (sides != 0) { // polygonal aperture
                        double a = FastMath.atan2(dy, dx);
                        a = ImageMath.mod(a, polyAngle * 2) - polyAngle;
                        f = FastMath.cos(a) * polyScale;
                    } else { // circular aperture
                        f = 1;
                    }
                    f = (f * r < radius) ? 1 : 0;
                }
                total += (float) f;
                kernel[0][i++] = (float) f;
            }
        }

        // normalize => the blur preserves overall image brightness
        for (int j = 0; j < kernel[0].length; j++) {
            kernel[0][j] /= total;
        }
        return kernel;
    }
}
