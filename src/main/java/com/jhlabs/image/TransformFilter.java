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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * An abstract superclass for filters which distort images in some way. The subclass only needs to override
 * two methods to provide the mapping between source and destination pixels.
 */
public abstract class TransformFilter extends AbstractBufferedImageOp {
    // image dimensions
    protected int srcWidth;
    protected int srcHeight;

    /**
     * Treat pixels off the edge as zero.
     */
    public final static int TRANSPARENT = 0;

    /**
     * Clamp pixels to the image edges.
     */
    public final static int REPEAT_EDGE_PIXELS = 1;

    /**
     * Wrap pixels off the edge onto the opposite edge.
     */
    public final static int WRAP_AROUND = 2;

    /**
     * Clamp pixels RGB to the image edges, but zero the alpha. This prevents gray borders on your image.
     */
    public final static int RGB_CLAMP = 3;

    public final static int REFLECT = 4;

    // Horizontally reflect, vertically repeat
    public final static int MIXED = 5;


    /**
     * Use nearest-neighbour interpolation.
     */
    public final static int NEAREST_NEIGHBOUR = 0;
    public final static int NEAREST_NEIGHBOUR_OLD = 2;

    /**
     * Use bilinear interpolation.
     */
    public final static int BILINEAR = 1;
    public final static int BILINEAR_OLD = 3;

    /**
     * The action to take for pixels off the image edge.
     */
    protected int edgeAction = RGB_CLAMP;

    /**
     * The type of interpolation to use.
     */
    protected int interpolation = BILINEAR;

    /**
     * The output image rectangle.
     */
//    protected Rectangle transformedSpace;

    /**
     * The input image rectangle.
     */
//    protected Rectangle originalSpace;

    protected TransformFilter(String filterName) {
        super(filterName);
    }

    /**
     * Set the action to perform for pixels off the edge of the image.
     *
     * @param edgeAction one of TRANSPARENT, REPEAT_EDGE_PIXELS or WRAP_AROUND
     * @see #getEdgeAction
     */
    public void setEdgeAction(int edgeAction) {
        this.edgeAction = edgeAction;
    }

    /**
     * Get the action to perform for pixels off the edge of the image.
     *
     * @return one of TRANSPARENT, REPEAT_EDGE_PIXELS or WRAP_AROUND
     * @see #setEdgeAction
     */
    public int getEdgeAction() {
        return edgeAction;
    }

    /**
     * Set the type of interpolation to perform.
     *
     * @param interpolation one of NEAREST_NEIGHBOUR or BILINEAR
     * @see #getInterpolation
     */
    public void setInterpolation(int interpolation) {
        this.interpolation = interpolation;
    }

    /**
     * Get the type of interpolation to perform.
     *
     * @return one of NEAREST_NEIGHBOUR or BILINEAR
     * @see #setInterpolation
     */
    public int getInterpolation() {
        return interpolation;
    }

    /**
     * Inverse transform a point. This method needs to be overriden by all subclasses.
     *
     * @param x   the X position of the pixel in the output image
     * @param y   the Y position of the pixel in the output image
     * @param out the position of the pixel in the input image
     */
    protected abstract void transformInverse(int x, int y, float[] out);

    /**
     * Forward transform a rectangle. Used to determine the size of the output image.
     *
     * @param rect the rectangle to transform
     */
    protected void transformSpace(Rectangle rect) {
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        srcWidth = src.getWidth();
        srcHeight = src.getHeight();
//		int type = src.getType();
//		WritableRaster srcRaster = src.getRaster();

// lbalazscs: commented out space transformation because it is not used anyway
// (in the Filters used by pixelitor)
//        originalSpace = new Rectangle(0, 0, width, height);
//        transformedSpace = new Rectangle(0, 0, width, height);
//        transformSpace(transformedSpace);

        if (dst == null) {
            ColorModel dstCM = src.getColorModel();
//            dst = new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(transformedSpace.width, transformedSpace.height), dstCM.isAlphaPremultiplied(), null);
            dst = new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(0, 0), dstCM.isAlphaPremultiplied(), null);
        }
//		WritableRaster dstRaster = dst.getRaster();

        int[] inPixels = getRGB(src, 0, 0, srcWidth, srcHeight, null);

        if (interpolation == BILINEAR) {
            return filterPixelsBilinear(dst, srcWidth, srcHeight, inPixels);
        } else if (interpolation == NEAREST_NEIGHBOUR) {
            return filterPixelsNN(dst, srcWidth, srcHeight, inPixels);
        } else if (interpolation == BILINEAR_OLD) {
            return filterPixelsBilinearOLD(dst, srcWidth, srcHeight, inPixels);
        } else if (interpolation == NEAREST_NEIGHBOUR_OLD) {
            return filterPixelsNNOLD(dst, srcWidth, srcHeight, inPixels);
        }

        throw new IllegalStateException("should not get here");
    }

    private BufferedImage filterPixelsBilinearOLD(BufferedImage dst, int width, int height, int[] inPixels) {
        int srcWidth = width;
        int srcHeight = height;
        int srcWidth1 = width - 1;
        int srcHeight1 = height - 1;
        int outWidth = width;
        int outHeight = height;
//        int outX, outY;
//		int index = 0;
        int[] outPixels = new int[outWidth];

        float[] out = new float[2];

        for (int y = 0; y < outHeight; y++) {
            for (int x = 0; x < outWidth; x++) {
                transformInverse(x, y, out);
                int srcX = (int) FastMath.floor(out[0]);
                int srcY = (int) FastMath.floor(out[1]);
                float xWeight = out[0] - srcX;
                float yWeight = out[1] - srcY;
                int nw, ne, sw, se;

                if ((srcX >= 0) && (srcX < srcWidth1) && (srcY >= 0) && (srcY < srcHeight1)) {
                    // Easy case, all corners are in the image
                    int i = (srcWidth * srcY) + srcX;
                    nw = inPixels[i];
                    ne = inPixels[i + 1];
                    sw = inPixels[i + srcWidth];
                    se = inPixels[i + srcWidth + 1];
                } else {
                    // Some of the corners are off the image
                    nw = getPixelBL(inPixels, srcX, srcY, srcWidth, srcHeight);
                    ne = getPixelBL(inPixels, srcX + 1, srcY, srcWidth, srcHeight);
                    sw = getPixelBL(inPixels, srcX, srcY + 1, srcWidth, srcHeight);
                    se = getPixelBL(inPixels, srcX + 1, srcY + 1, srcWidth, srcHeight);
                }
                outPixels[x] = ImageMath.bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se);
            }
            setRGB(dst, 0, y, width, 1, outPixels);
        }
        return dst;
    }

    protected BufferedImage filterPixelsNNOLD(BufferedImage dst, int width, int height, int[] inPixels) {
        int srcWidth = width;
        int srcHeight = height;
        int outWidth = width;
        int outHeight = height;

        int srcX, srcY;
        int[] outPixels = new int[outWidth];

//		int[] rgb = new int[4];
        float[] out = new float[2];

        for (int y = 0; y < outHeight; y++) {
            for (int x = 0; x < outWidth; x++) {
                transformInverse(x, y, out);
                srcX = (int) out[0];
                srcY = (int) out[1];
                // int casting rounds towards zero, so we check out[0] < 0, not srcX < 0
                outPixels[x] = getPixelNN(inPixels, srcWidth, srcHeight, srcX, srcY, out);
            }
            setRGB(dst, 0, y, width, 1, outPixels);
        }
        return dst;
    }

    protected BufferedImage filterPixelsNN(BufferedImage dst, int width, int height, int[] inPixels) {
        int srcWidth = width;
        int srcHeight = height;
        int outWidth = width;
        int outHeight = height;

        pt = createProgressTracker(outHeight);
        Future<int[]>[] resultLines = new Future[outHeight];

        for (int y = 0; y < outHeight; y++) {
            float[] out = new float[2];
            int finalY = y;
            Callable<int[]> calculateLineTask = () -> {
                int srcX, srcY;
                int[] outPixels = new int[outWidth];

                for (int x = 0; x < outWidth; x++) {
                    transformInverse(x, finalY, out);
                    srcX = (int) out[0];
                    srcY = (int) out[1];
                    // int casting rounds towards zero, so we check out[0] < 0, not srcX < 0
                    outPixels[x] = getPixelNN(inPixels, srcWidth, srcHeight, srcX, srcY, out);
                }

                return outPixels;

            };
            resultLines[finalY] = ThreadPool.submit(calculateLineTask);
        }
        ThreadPool.waitForFutures2(dst, width, resultLines, pt);
        finishProgressTracker();

        return dst;
    }

    private BufferedImage filterPixelsBilinear(BufferedImage dst, int width, int height, int[] inPixels) {
        int srcWidth = width;
        int srcHeight = height;
        int srcWidth1 = width - 1;
        int srcHeight1 = height - 1;
        int outWidth = width;
        int outHeight = height;
//        int outX, outY;
//		int index = 0;

        pt = createProgressTracker(outHeight);
        Future<int[]>[] resultLines = new Future[outHeight];

        for (int y = 0; y < outHeight; y++) {
            float[] out = new float[2];
            int finalY = y;
            Callable<int[]> calculateLineTask = () -> {
                int[] outPixels = new int[outWidth];
                for (int x = 0; x < outWidth; x++) {
                    transformInverse(x, finalY, out);
                    int srcX = (int) FastMath.floor(out[0]);
                    int srcY = (int) FastMath.floor(out[1]);
                    float xWeight = out[0] - srcX;
                    float yWeight = out[1] - srcY;
                    int nw, ne, sw, se;

                    if ((srcX >= 0) && (srcX < srcWidth1) && (srcY >= 0) && (srcY < srcHeight1)) {
                        // Easy case, all corners are in the image
                        int i = (srcWidth * srcY) + srcX;
                        nw = inPixels[i];
                        ne = inPixels[i + 1];
                        sw = inPixels[i + srcWidth];
                        se = inPixels[i + srcWidth + 1];
                    } else {
                        // Some of the corners are off the image
                        nw = getPixelBL(inPixels, srcX, srcY, srcWidth, srcHeight);
                        ne = getPixelBL(inPixels, srcX + 1, srcY, srcWidth, srcHeight);
                        sw = getPixelBL(inPixels, srcX, srcY + 1, srcWidth, srcHeight);
                        se = getPixelBL(inPixels, srcX + 1, srcY + 1, srcWidth, srcHeight);
                    }
                    outPixels[x] = ImageMath.bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se);
                }
                return outPixels;
            };

            resultLines[finalY] = ThreadPool.submit(calculateLineTask);
        }
        ThreadPool.waitForFutures2(dst, width, resultLines, pt);
        finishProgressTracker();

        return dst;
    }

    private int getPixelBL(int[] pixels, int x, int y, int width, int height) {
        if ((x < 0) || (x >= width)) {  // x out of range
            if ((y < 0) || (y >= height)) { // y also out of range {
                switch (edgeAction) {
                    case TRANSPARENT:
                    default:
                        return 0;
                    case WRAP_AROUND:
                        return pixels[(ImageMath.mod(y, height) * width) + ImageMath.mod(x, width)];
                    case REPEAT_EDGE_PIXELS:
                        return pixels[(ImageMath.clamp(y, 0, height - 1) * width) + ImageMath.clamp(x, 0, width - 1)];
                    case REFLECT:
                        int reflectedX = ImageMath.reflectTriangle(x, width);
                        int reflectedY = ImageMath.reflectTriangle(y, height);
                        return pixels[(reflectedY * width) + reflectedX];
                }
            } else { // x out of range, but y is OK
                switch (edgeAction) {
                    case TRANSPARENT:
                    default:
                        return 0;
                    case WRAP_AROUND:
                        return pixels[(y * width) + ImageMath.mod(x, width)];
                    case REPEAT_EDGE_PIXELS:
                        return pixels[(y * width) + ImageMath.clamp(x, 0, width - 1)];
                    case REFLECT:
                        int reflectedX = ImageMath.reflectTriangle(x, width);
                        return pixels[(y * width) + reflectedX];
                }
            }
        } else { // x in range
            if ((y < 0) || (y >= height)) { // ... but y is out
                switch (edgeAction) {
                    case TRANSPARENT:
                    default:
                        return 0;
                    case WRAP_AROUND:
                        return pixels[(ImageMath.mod(y, height) * width) + x];
                    case REPEAT_EDGE_PIXELS:
                        return pixels[(ImageMath.clamp(y, 0, height - 1) * width) + x];
                    case REFLECT:
                        int reflectedY = ImageMath.reflectTriangle(y, height);
                        return pixels[(reflectedY * width) + x];
                }
            } else { // both x and y are OK
                return pixels[((y * width) + x)];
            }
        }
    }

    private int getPixelNN(int[] inPixels, int srcWidth, int srcHeight, int srcX, int srcY, float[] out) {
        if ((out[0] < 0) || (srcX >= srcWidth)) { // x out of range
            if ((out[1] < 0) || (srcY >= srcHeight)) {  // y also out of range
                switch (edgeAction) {
                    case TRANSPARENT:
                    default:
                        return 0;
                    case WRAP_AROUND:
                        return inPixels[(ImageMath.mod(srcY, srcHeight) * srcWidth) + ImageMath.mod(srcX, srcWidth)];
                    case REPEAT_EDGE_PIXELS:
                        return inPixels[(ImageMath.clamp(srcY, 0, srcHeight - 1) * srcWidth) + ImageMath.clamp(srcX, 0, srcWidth - 1)];
                    case REFLECT:
                        int reflectedX = ImageMath.reflectTriangle(srcX, srcWidth);
                        int reflectedY = ImageMath.reflectTriangle(srcY, srcHeight);
                        return inPixels[(reflectedY * srcWidth) + reflectedX];
                }
            } else { // x out of range, but y is OK
                switch (edgeAction) {
                    case TRANSPARENT:
                    default:
                        return 0;
                    case WRAP_AROUND:
                        return inPixels[(srcY * srcWidth) + ImageMath.mod(srcX, srcWidth)];
                    case REPEAT_EDGE_PIXELS:
                        return inPixels[(srcY * srcWidth) + ImageMath.clamp(srcX, 0, srcWidth - 1)];
                    case REFLECT:
                        int reflectedX = ImageMath.reflectTriangle(srcX, srcWidth);
                        return inPixels[(srcY * srcWidth) + reflectedX];
                }

            }
        } else {  // x in range
            if ((out[1] < 0) || (srcY >= srcHeight)) { // ... but y isn't
                switch (edgeAction) {
                    case TRANSPARENT:
                    default:
                        return 0;
                    case WRAP_AROUND:
                        return inPixels[(ImageMath.mod(srcY, srcHeight) * srcWidth) + srcX];
                    case REPEAT_EDGE_PIXELS:
                        return inPixels[(ImageMath.clamp(srcY, 0, srcHeight - 1) * srcWidth) + srcX];
                    case REFLECT:
//                        int wrappedY = ImageMath.mod(srcY, srcHeight);
//                        int reflectedY = srcHeight - wrappedY - 1;
                        int reflectedY = ImageMath.reflectTriangle(srcY, srcHeight);
                        return inPixels[(reflectedY * srcWidth) + srcX];
                }
            } else { // both x and y are OK
                int i = (srcWidth * srcY) + srcX;
                return inPixels[i];
            }
        }
    }

}
