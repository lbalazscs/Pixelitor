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
    // image dimensions (the source and destination sizes always match)
    protected int width;
    protected int height;

    // strategies for pixels that map outside the source image bounds
    public static final int TRANSPARENT = 0; // treat out-of-bounds pixels as transparent
    public static final int REPEAT_EDGE = 1; // use the nearest edge pixel
    public static final int WRAP_AROUND = 2; // wrap around to the opposite edge
    public static final int REFLECT = 4; // mirror the image at edges
    protected int edgeAction = REPEAT_EDGE;

    // interpolation methods for sampling between pixel centers
    public static final int NEAREST_NEIGHBOR = 0;
    public static final int BILINEAR = 1;
    public static final int BICUBIC = 2;
    protected int interpolation = BILINEAR;

    protected TransformFilter(String filterName) {
        super(filterName);
    }

    protected TransformFilter(String filterName, int edgeAction, int interpolation) {
        super(filterName);

        this.edgeAction = edgeAction;
        this.interpolation = interpolation;
    }

    /**
     * Sets the strategy for handling pixels that map outside the source image bounds.
     *
     * @param edgeAction one of TRANSPARENT, REPEAT_EDGE, REFLECT or WRAP_AROUND
     */
    public void setEdgeAction(int edgeAction) {
        this.edgeAction = edgeAction;
    }

    /**
     * Sets the interpolation method used when sampling between pixel centers.
     *
     * @param interpolation one of NEAREST_NEIGHBOUR, BILINEAR, or BICUBIC
     */
    public void setInterpolation(int interpolation) {
        this.interpolation = interpolation;
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
        width = src.getWidth();
        height = src.getHeight();

        if (dst == null) {
            ColorModel dstCM = src.getColorModel();
            dst = new BufferedImage(dstCM,
                dstCM.createCompatibleWritableRaster(width, height),
                dstCM.isAlphaPremultiplied(), null);
        }

        int[] inPixels = getRGB(src, 0, 0, width, height, null);

        return switch (interpolation) {
            case BILINEAR -> filterPixelsBilinear(dst, inPixels);
            case BICUBIC -> filterPixelsBicubic(dst, inPixels);
            case NEAREST_NEIGHBOR -> filterPixelsNN(dst, inPixels);
            default -> throw new IllegalStateException("should not get here");
        };
    }

    /**
     * Applies the transform using nearest-neighbor interpolation.
     */
    private BufferedImage filterPixelsNN(BufferedImage dst, int[] inPixels) {
        pt = createProgressTracker(height);

        @SuppressWarnings("unchecked")
        Future<int[]>[] rowFutures = new Future[height];

        // process each output line in parallel
        for (int y = 0; y < height; y++) {
            int finalY = y;

            Callable<int[]> rowTask = () -> {
                float[] out = new float[2];
                int[] outLine = new int[width];

                for (int x = 0; x < width; x++) {
                    transformInverse(x, finalY, out);
                    int srcX = (int) out[0];
                    int srcY = (int) out[1];
                    // int casting rounds towards zero, so we check out[0] < 0, not srcX < 0
                    outLine[x] = sampleNN(inPixels, srcX, srcY, out);
                }

                return outLine;
            };

            rowFutures[finalY] = ThreadPool.submit2(rowTask);
        }

        ThreadPool.waitFor2(rowFutures, dst, width, pt);
        finishProgressTracker();

        return dst;
    }

    /**
     * Applies the transform using bilinear interpolation.
     */
    private BufferedImage filterPixelsBilinear(BufferedImage dst, int[] inPixels) {
        int maxSrcX = width - 1;
        int maxSrcY = height - 1;

        pt = createProgressTracker(height);
        @SuppressWarnings("unchecked")
        Future<int[]>[] rowFutures = new Future[height];

        // process each output line in parallel
        for (int y = 0; y < height; y++) {
            int finalY = y;

            Callable<int[]> rowTask = () -> {
                float[] out = new float[2];
                int[] outLine = new int[width];
                for (int x = 0; x < width; x++) {
                    transformInverse(x, finalY, out);

                    int srcX = (int) FastMath.floor(out[0]);
                    int srcY = (int) FastMath.floor(out[1]);
                    float xWeight = out[0] - srcX;
                    float yWeight = out[1] - srcY;
                    int nw, ne, sw, se;

                    if ((srcX >= 0) && (srcX < maxSrcX) && (srcY >= 0) && (srcY < maxSrcY)) {
                        // fast path when all pixels are within bounds
                        int i = (width * srcY) + srcX;
                        nw = inPixels[i];
                        ne = inPixels[i + 1];
                        sw = inPixels[i + width];
                        se = inPixels[i + width + 1];
                    } else {
                        // some of the sample points are outside the image bounds
                        nw = sampleBL(inPixels, srcX, srcY);
                        ne = sampleBL(inPixels, srcX + 1, srcY);
                        sw = sampleBL(inPixels, srcX, srcY + 1);
                        se = sampleBL(inPixels, srcX + 1, srcY + 1);
                    }
                    outLine[x] = ImageMath.bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se);
                }
                return outLine;
            };

            rowFutures[finalY] = ThreadPool.submit2(rowTask);
        }

        ThreadPool.waitFor2(rowFutures, dst, width, pt);
        finishProgressTracker();

        return dst;
    }

    /**
     * Applies the transform using bicubic interpolation.
     */
    private BufferedImage filterPixelsBicubic(BufferedImage dst, int[] inPixels) {
        pt = createProgressTracker(height);
        @SuppressWarnings("unchecked")
        Future<int[]>[] rowFutures = new Future[height];

        // process each output line in parallel
        for (int y = 0; y < height; y++) {
            int finalY = y;

            Callable<int[]> rowTask = () -> {
                float[] out = new float[2];
                int[] outLine = new int[width];
                int[][] p = new int[4][4];
                for (int x = 0; x < width; x++) {
                    transformInverse(x, finalY, out);

                    float srcX_f = out[0];
                    float srcY_f = out[1];
                    int srcX = (int) FastMath.floor(srcX_f);
                    int srcY = (int) FastMath.floor(srcY_f);
                    float xWeight = srcX_f - srcX;
                    float yWeight = srcY_f - srcY;

                    // check if the 4x4 neighborhood is completely within the image
                    if (srcX >= 1 && srcX < width - 2 && srcY >= 1 && srcY < height - 2) {
                        // fast path
                        int i = (width * (srcY - 1)) + srcX - 1;
                        for (int row = 0; row < 4; row++) {
                            p[row][0] = inPixels[i];
                            p[row][1] = inPixels[i + 1];
                            p[row][2] = inPixels[i + 2];
                            p[row][3] = inPixels[i + 3];
                            i += width;
                        }
                    } else {
                        // slow path with edge handling
                        for (int row = 0; row < 4; row++) {
                            for (int col = 0; col < 4; col++) {
                                p[row][col] = sampleBL(inPixels, srcX - 1 + col, srcY - 1 + row);
                            }
                        }
                    }
                    outLine[x] = ImageMath.bicubicInterpolate(xWeight, yWeight, p);
                }
                return outLine;
            };

            rowFutures[finalY] = ThreadPool.submit2(rowTask);
        }

        ThreadPool.waitFor2(rowFutures, dst, width, pt);
        finishProgressTracker();

        return dst;
    }

    /**
     * Samples a pixel for bilinear interpolation.
     */
    private int sampleBL(int[] pixels, int x, int y) {
        if ((x < 0) || (x >= width)) {
            if ((y < 0) || (y >= height)) {
                return sampleCorner(pixels, x, y);
            } else {
                return sampleVerEdge(pixels, x, y);
            }
        } else {
            if ((y < 0) || (y >= height)) {
                return sampleHorEdge(pixels, x, y);
            } else {
                return pixels[((y * width) + x)];
            }
        }
    }

    /**
     * Samples a pixel for nearest-neighbor interpolation.
     */
    private int sampleNN(int[] inPixels, int x, int y, float[] out) {
        if ((out[0] < 0) || (x >= width)) {
            if ((out[1] < 0) || (y >= height)) {
                return sampleCorner(inPixels, x, y);
            } else {
                return sampleVerEdge(inPixels, x, y);
            }
        } else {
            if ((out[1] < 0) || (y >= height)) {
                return sampleHorEdge(inPixels, x, y);
            } else {
                return inPixels[(width * y) + x];
            }
        }
    }

    private int sampleCorner(int[] pixels, int x, int y) {
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

    private int sampleVerEdge(int[] pixels, int x, int y) {
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

    private int sampleHorEdge(int[] pixels, int x, int y) {
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
