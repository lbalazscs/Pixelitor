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
import pixelitor.ThreadPool;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * An abstract superclass for filters that transform images through
 * pixel coordinate mapping.
 * Subclasses need to implement the transformInverse method to define
 * the mapping between source and destination pixel coordinates.
 */
public abstract class TransformFilter extends AbstractBufferedImageOp {
    // image dimensions
    protected int srcWidth;
    protected int srcHeight;

    // strategies for pixels that map outside the source image bounds
    public static final int TRANSPARENT = 0; // treat out-of-bounds pixels as transparent
    public static final int REPEAT_EDGE = 1; // use the nearest edge pixel
    public static final int WRAP_AROUND = 2; // wrap around to the opposite edge
    public static final int REFLECT = 4; // mirror the image at edges
    protected int edgeAction = REPEAT_EDGE;

    // Interpolation methods for sampling between pixel centers
    public static final int NEAREST_NEIGHBOUR = 0;
    public static final int BILINEAR = 1;
    protected int interpolation = BILINEAR;

    protected TransformFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the strategy for handling pixels that map outside the source image bounds.
     *
     * @param edgeAction one of TRANSPARENT, REPEAT_EDGE, REFLECT or WRAP_AROUND
     */
    public void setEdgeAction(int edgeAction) {
        this.edgeAction = edgeAction;
    }

    public int getEdgeAction() {
        return edgeAction;
    }

    /**
     * Sets the interpolation method used when sampling between pixel centers.
     *
     * @param interpolation one of NEAREST_NEIGHBOUR or BILINEAR
     */
    public void setInterpolation(int interpolation) {
        this.interpolation = interpolation;
    }

    public int getInterpolation() {
        return interpolation;
    }

    /**
     * Maps a destination pixel coordinate back to its source location.
     *
     * @param x   The X coordinate in the destination image
     * @param y   The Y coordinate in the destination image
     * @param out Output array to store the mapped source coordinates (x, y)
     */
    protected abstract void transformInverse(int x, int y, float[] out);

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        srcWidth = src.getWidth();
        srcHeight = src.getHeight();

        if (dst == null) {
            ColorModel dstCM = src.getColorModel();
            dst = new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(0, 0), dstCM
                    .isAlphaPremultiplied(), null);
        }

        int[] inPixels = getRGB(src, 0, 0, srcWidth, srcHeight, null);

        return switch (interpolation) {
            case BILINEAR -> filterPixelsBilinear(dst, srcWidth, srcHeight, inPixels);
            case NEAREST_NEIGHBOUR -> filterPixelsNN(dst, srcWidth, srcHeight, inPixels);
            default -> throw new IllegalStateException("should not get here");
        };
    }

    /**
     * Applies the transform using nearest-neighbor interpolation.
     */
    private BufferedImage filterPixelsNN(BufferedImage dst, int width, int height, int[] inPixels) {
        int outWidth = width;
        int outHeight = height;

        pt = createProgressTracker(outHeight);

        @SuppressWarnings("unchecked")
        Future<int[]>[] resultLines = new Future[outHeight];

        // process each output line in parallel
        for (int y = 0; y < outHeight; y++) {
            float[] out = new float[2];
            int finalY = y;

            Callable<int[]> calcLineTask = () -> {
                int[] outLine = new int[outWidth];

                for (int x = 0; x < outWidth; x++) {
                    transformInverse(x, finalY, out);
                    int srcX = (int) out[0];
                    int srcY = (int) out[1];
                    // int casting rounds towards zero, so we check out[0] < 0, not srcX < 0
                    outLine[x] = sampleNN(inPixels, srcX, srcY, srcWidth, srcHeight, out);
                }

                return outLine;
            };

            resultLines[finalY] = ThreadPool.submit2(calcLineTask);
        }

        ThreadPool.waitFor2(resultLines, dst, width, pt);
        finishProgressTracker();

        return dst;
    }

    /**
     * Applies the transform using bilinear interpolation.
     */
    private BufferedImage filterPixelsBilinear(BufferedImage dst, int width, int height, int[] inPixels) {
        int maxSrcX = width - 1;
        int maxSrcY = height - 1;
        int outWidth = width;
        int outHeight = height;

        pt = createProgressTracker(outHeight);
        @SuppressWarnings("unchecked")
        Future<int[]>[] resultLines = new Future[outHeight];

        // process each output line in parallel
        for (int y = 0; y < outHeight; y++) {
            float[] out = new float[2];
            int finalY = y;

            Callable<int[]> calcLineTask = () -> {
                int[] outLine = new int[outWidth];
                for (int x = 0; x < outWidth; x++) {
                    transformInverse(x, finalY, out);

                    int srcX = (int) FastMath.floor(out[0]);
                    int srcY = (int) FastMath.floor(out[1]);
                    float xWeight = out[0] - srcX;
                    float yWeight = out[1] - srcY;
                    int nw, ne, sw, se;

                    if ((srcX >= 0) && (srcX < maxSrcX) && (srcY >= 0) && (srcY < maxSrcY)) {
                        // fast path when all pixels are within bounds
                        int i = (srcWidth * srcY) + srcX;
                        nw = inPixels[i];
                        ne = inPixels[i + 1];
                        sw = inPixels[i + srcWidth];
                        se = inPixels[i + srcWidth + 1];
                    } else {
                        // some of the corners are off the image
                        nw = sampleBL(inPixels, srcX, srcY, srcWidth, srcHeight);
                        ne = sampleBL(inPixels, srcX + 1, srcY, srcWidth, srcHeight);
                        sw = sampleBL(inPixels, srcX, srcY + 1, srcWidth, srcHeight);
                        se = sampleBL(inPixels, srcX + 1, srcY + 1, srcWidth, srcHeight);
                    }
                    outLine[x] = ImageMath.bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se);
                }
                return outLine;
            };

            resultLines[finalY] = ThreadPool.submit2(calcLineTask);
        }

        ThreadPool.waitFor2(resultLines, dst, width, pt);
        finishProgressTracker();

        return dst;
    }

    /**
     * Samples a pixel for bilinear interpolation.
     */
    private int sampleBL(int[] pixels, int x, int y, int width, int height) {
        if ((x < 0) || (x >= width)) {
            if ((y < 0) || (y >= height)) {
                return sampleCorner(pixels, x, y, width, height);
            } else {
                return sampleVerEdge(pixels, x, y, width);
            }
        } else {
            if ((y < 0) || (y >= height)) {
                return sampleHorEdge(pixels, x, y, width, height);
            } else {
                return pixels[((y * width) + x)];
            }
        }
    }

    /**
     * Samples a pixel for nearest-neighbor interpolation.
     */
    private int sampleNN(int[] inPixels, int x, int y, int width, int height, float[] out) {
        if ((out[0] < 0) || (x >= width)) {
            if ((out[1] < 0) || (y >= height)) {
                return sampleCorner(inPixels, x, y, width, height);
            } else {
                return sampleVerEdge(inPixels, x, y, width);
            }
        } else {
            if ((out[1] < 0) || (y >= height)) {
                return sampleHorEdge(inPixels, x, y, width, height);
            } else {
                return inPixels[(width * y) + x];
            }
        }
    }

    private int sampleCorner(int[] pixels, int x, int y, int width, int height) {
        return switch (edgeAction) {
            case TRANSPARENT -> 0;
            case WRAP_AROUND -> pixels[(ImageMath.mod(y, height) * width) + ImageMath.mod(x, width)];
            case REPEAT_EDGE -> pixels[(ImageMath.clamp(y, 0, height - 1) * width) + ImageMath.clamp(x, 0, width - 1)];
            case REFLECT -> {
                int reflectedX = ImageMath.reflectTriangle(x, width);
                int reflectedY = ImageMath.reflectTriangle(y, height);
                yield pixels[(reflectedY * width) + reflectedX];
            }
            default -> 0;
        };
    }

    private int sampleVerEdge(int[] pixels, int x, int y, int width) {
        return switch (edgeAction) {
            case TRANSPARENT -> 0;
            case WRAP_AROUND -> pixels[(y * width) + ImageMath.mod(x, width)];
            case REPEAT_EDGE -> pixels[(y * width) + ImageMath.clamp(x, 0, width - 1)];
            case REFLECT -> {
                int reflectedX = ImageMath.reflectTriangle(x, width);
                yield pixels[(y * width) + reflectedX];
            }
            default -> 0;
        };
    }

    private int sampleHorEdge(int[] pixels, int x, int y, int width, int height) {
        return switch (edgeAction) {
            case TRANSPARENT -> 0;
            case WRAP_AROUND -> pixels[(ImageMath.mod(y, height) * width) + x];
            case REPEAT_EDGE -> pixels[(ImageMath.clamp(y, 0, height - 1) * width) + x];
            case REFLECT -> {
                int reflectedY = ImageMath.reflectTriangle(y, height);
                yield pixels[(reflectedY * width) + x];
            }
            default -> 0;
        };
    }
}
