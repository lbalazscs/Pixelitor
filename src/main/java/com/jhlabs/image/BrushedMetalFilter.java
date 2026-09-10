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
import java.util.random.RandomGenerator;

import static com.jhlabs.image.PixelUtils.clamp;

/**
 * A filter which produces an image simulating brushed metal.
 */
public class BrushedMetalFilter extends AbstractBufferedImageOp {
    private final int radius;
    private final float amount;
    private final int color;
    private final float shine;
    private final float angle;
    private final RandomGenerator random;

    /**
     * Constructs a {@link BrushedMetalFilter}.
     *
     * @param filterName the name of the filter
     * @param color      the color of the metal
     * @param radius     the radius of the blur in the brushing direction
     * @param amount     the amount of texture/noise to add (in the range [0, 1])
     * @param shine      the amount of shine to add (in the range [0, 1])
     * @param angle      the brush direction in radians
     * @param random     the random number generator
     */
    public BrushedMetalFilter(String filterName, int color, int radius, float amount, float shine, float angle, RandomGenerator random) {
        super(filterName);

        this.color = color;
        this.radius = radius;
        this.amount = 255 * amount;
        this.shine = shine;
        this.angle = angle;
        this.random = random;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(height);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int a = color & 0xFF_00_00_00;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float normalizedAngle = (float) Math.IEEEremainder(angle, 2 * Math.PI);

        return (normalizedAngle == 0.0f)
            ? fastHorizontalBrushing(dst, width, height, r, g, b, a)
            : arbitraryAngleBrushing(dst, width, height, r, g, b, a);
    }

    // fast path for horizontal brushing (angle == 0)
    private BufferedImage fastHorizontalBrushing(BufferedImage dst, int width, int height, int r, int g, int b, int a) {
        int[] inPixels = new int[width];
        int[] outPixels = radius != 0 ? new int[width] : null;

        int[] rBase = new int[width];
        int[] gBase = new int[width];
        int[] bBase = new int[width];

        double piOverWidth = Math.PI / width;
        for (int x = 0; x < width; x++) {
            int f = shine != 0 ? (int) (255 * shine * FastMath.sin(x * piOverWidth)) : 0;
            rBase[x] = r + f;
            gBase[x] = g + f;
            bBase[x] = b + f;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int n = (int) ((2 * random.nextFloat() - 1) * amount);
                inPixels[x] = a | (clamp(rBase[x] + n) << 16) | (clamp(gBase[x] + n) << 8) | clamp(bBase[x] + n);
            }
            if (radius != 0) {
                blur(inPixels, outPixels, width, radius);
                setRGB(dst, 0, y, width, 1, outPixels);
            } else {
                setRGB(dst, 0, y, width, 1, inPixels);
            }

            pt.unitDone();
        }

        finishProgressTracker();
        return dst;
    }

    public static void blur(int[] in, int[] out, int width, int radius) {
        int widthMinus1 = width - 1;
        int r2 = 2 * radius + 1;
        int tr = 0, tg = 0, tb = 0;

        for (int i = -radius; i <= radius; i++) {
            int rgb = in[Math.floorMod(i, width)];
            tr += (rgb >> 16) & 0xFF;
            tg += (rgb >> 8) & 0xFF;
            tb += rgb & 0xFF;
        }

        for (int x = 0; x < width; x++) {
            out[x] = 0xFF_00_00_00 | ((tr / r2) << 16) | ((tg / r2) << 8) | (tb / r2);

            int i1 = x + radius + 1;
            if (i1 > widthMinus1) {
                i1 = Math.floorMod(i1, width);
            }
            int i2 = x - radius;
            if (i2 < 0) {
                i2 = Math.floorMod(i2, width);
            }
            int rgb1 = in[i1];
            int rgb2 = in[i2];

            tr += ((rgb1 & 0xFF_00_00) - (rgb2 & 0xFF_00_00)) >> 16;
            tg += ((rgb1 & 0xFF_00) - (rgb2 & 0xFF_00)) >> 8;
            tb += (rgb1 & 0xFF) - (rgb2 & 0xFF);
        }
    }

    // arbitrary angle brushing: rotate coordinate space before & after 1D blur
    private BufferedImage arbitraryAngleBrushing(BufferedImage dst, int width, int height, int r, int g, int b, int a) {
        double cos = FastMath.cos(angle);
        double sin = FastMath.sin(angle);

        double absCos = Math.abs(cos);
        double absSin = Math.abs(sin);

        // dimensions of the rotated intermediate buffer covering the destination image
        int wRot = Math.max(1, (int) Math.ceil(width * absCos + height * absSin));
        int hRot = Math.max(1, (int) Math.ceil(width * absSin + height * absCos));

        int[] rotatedPixels = new int[wRot * hRot];
        int[] inRow = new int[wRot];
        int[] outRow = radius != 0 ? new int[wRot] : null;

        // precompute base colors (with shine) along the brush direction in rotated space
        int[] rBase = new int[wRot];
        int[] gBase = new int[wRot];
        int[] bBase = new int[wRot];

        double piOverWRot = Math.PI / wRot;
        for (int u = 0; u < wRot; u++) {
            int f = shine != 0 ? (int) (255 * shine * FastMath.sin(u * piOverWRot)) : 0;
            rBase[u] = r + f;
            gBase[u] = g + f;
            bBase[u] = b + f;
        }

        // generate noise and apply O(1) 1D box blur row-by-row in rotated space
        for (int v = 0; v < hRot; v++) {
            for (int u = 0; u < wRot; u++) {
                int n = (int) ((2 * random.nextFloat() - 1) * amount);
                inRow[u] = a | (clamp(rBase[u] + n) << 16) | (clamp(gBase[u] + n) << 8) | clamp(bBase[u] + n);
            }
            if (radius != 0) {
                blur(inRow, outRow, wRot, radius);
                System.arraycopy(outRow, 0, rotatedPixels, v * wRot, wRot);
            } else {
                System.arraycopy(inRow, 0, rotatedPixels, v * wRot, wRot);
            }
        }

        // resample rotated blurred buffer into destination image using bilinear interpolation
        double cx = width / 2.0;
        double cy = height / 2.0;
        double cu = wRot / 2.0;
        double cv = hRot / 2.0;

        int[] dstRow = new int[width];

        for (int y = 0; y < height; y++) {
            double dy = y - cy + 0.5;
            double dySin = dy * sin;
            double dyCos = dy * cos;

            for (int x = 0; x < width; x++) {
                double dx = x - cx + 0.5;
                double uCenter = dx * cos + dySin + cu;
                double vCenter = -dx * sin + dyCos + cv;

                double u = ImageMath.clamp(uCenter - 0.5, 0, wRot - 1);
                double v = ImageMath.clamp(vCenter - 0.5, 0, hRot - 1);

                int x0 = (int) u;
                int y0 = (int) v;
                int x1 = Math.min(x0 + 1, wRot - 1);
                int y1 = Math.min(y0 + 1, hRot - 1);

                float fx = (float) (u - x0);
                float fy = (float) (v - y0);

                int row0 = y0 * wRot;
                int row1 = y1 * wRot;

                int p00 = rotatedPixels[row0 + x0];
                int p10 = rotatedPixels[row0 + x1];
                int p01 = rotatedPixels[row1 + x0];
                int p11 = rotatedPixels[row1 + x1];

                dstRow[x] = ImageMath.bilinearInterpolate(fx, fy, p00, p10, p01, p11);
            }
            setRGB(dst, 0, y, width, 1, dstRow);
            pt.unitDone();
        }

        finishProgressTracker();
        return dst;
    }
}
