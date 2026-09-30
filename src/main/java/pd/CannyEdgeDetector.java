/*
 * Copyright 2006 - 2010 Tom Gibara.
 *
 * This file is in public domain.
 * See http://www.tomgibara.com/computer-vision/canny-edge-detector
 */

package pd;

import com.jhlabs.image.ImageMath;
import pixelitor.progress.ProgressTracker;
import pixelitor.progress.StatusBarProgressTracker;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * <p><em>This software has been released into the public domain.
 * <strong>Please read the notes in this source file for additional information.
 * </strong></em></p>
 *
 * <p>This class provides a configurable implementation of the Canny edge
 * detection algorithm. This classic algorithm has a number of shortcomings,
 * but remains an effective tool in many scenarios. <em>This class is designed
 * for single threaded use only.</em></p>
 *
 * <p>Sample usage:</p>
 *
 * <pre><code>
 * //create the detector
 * CannyEdgeDetector detector = new CannyEdgeDetector();
 * //adjust its parameters as desired
 * detector.setLowThreshold(0.5f);
 * detector.setHighThreshold(1f);
 * //apply it to an image
 * detector.setSourceImage(frame);
 * detector.process();
 * BufferedImage edges = detector.getEdgesImage();
 * </code></pre>
 *
 * <p>For a more complete understanding of this edge detector's parameters
 * consult an explanation of the algorithm.</p>
 *
 * @author Tom Gibara
 */

public class CannyEdgeDetector {
    // statics
    private static final float GAUSSIAN_CUT_OFF = 0.005f;
    private static final float MAGNITUDE_SCALE = 100.0F;
    private static final float MAGNITUDE_LIMIT = 1000.0F;
    private static final int MAGNITUDE_MAX = (int) (MAGNITUDE_SCALE * MAGNITUDE_LIMIT);

    private static final int EDGE_COLOR_WHITE = 0xFF_FF_FF_FF;
    private static final int BG_COLOR_BLACK = 0xFF_00_00_00;

    // fields

    private int height;
    private int width;
    private int pixelCount;
    private int[] luminance;
    private int[] magnitude;
    private BufferedImage sourceImage;
    private BufferedImage edgesImage;

    private float gaussianKernelRadius;
    private float lowThreshold;
    private float highThreshold;
    private int gaussianKernelWidth;
    private boolean contrastNormalized;

    private ProgressTracker pt;

    // constructors

    /**
     * Constructs a new detector with default parameters.
     */
    public CannyEdgeDetector() {
        lowThreshold = 2.5f;
        highThreshold = 7.5f;
        gaussianKernelRadius = 2.0f;
        gaussianKernelWidth = 16;
        contrastNormalized = false;
    }

    // accessors

    /**
     * Specifies the image that will provide the luminance data in which edges
     * will be detected. A source image must be set before the process method
     * is called.
     *
     * @param image a source of luminance data
     */
    public void setSourceImage(BufferedImage image) {
        sourceImage = image;
    }

    /**
     * Obtains an image containing the edges detected during the last call to
     * the process method. The buffered image is an opaque image of type
     * BufferedImage.TYPE_INT_ARGB in which edge pixels are white and all other
     * pixels are black.
     *
     * @return an image containing the detected edges, or null if the process
     * method has not yet been called.
     */
    public BufferedImage getEdgesImage() {
        return edgesImage;
    }

    /**
     * Sets the edges image. Calling this method will not change the operation
     * of the edge detector in any way. It is intended to provide a means by
     * which the memory referenced by the detector object may be reduced.
     *
     * @param edgesImage expected (though not required) to be null
     */
    public void setEdgesImage(BufferedImage edgesImage) {
        this.edgesImage = edgesImage;
    }

    /**
     * Sets the low threshold for hysteresis. Suitable values for this parameter
     * must be determined experimentally for each application. It is nonsensical
     * (though not prohibited) for this value to exceed the high threshold value.
     *
     * @param threshold a low hysteresis threshold
     */
    public void setLowThreshold(float threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException();
        }
        lowThreshold = threshold;
    }

    /**
     * Sets the high threshold for hysteresis. Suitable values for this
     * parameter must be determined experimentally for each application. It is
     * nonsensical (though not prohibited) for this value to be less than the
     * low threshold value.
     *
     * @param threshold a high hysteresis threshold
     */
    public void setHighThreshold(float threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException();
        }
        highThreshold = threshold;
    }

    /**
     * The number of pixels across which the Gaussian kernel is applied.
     * This implementation will reduce the radius if the contribution of pixel
     * values is deemed negligible, so this is actually a maximum radius.
     *
     * @param gaussianKernelWidth a radius for the convolution operation in
     *                            pixels, at least 2.
     */
    public void setGaussianKernelWidth(int gaussianKernelWidth) {
        if (gaussianKernelWidth < 2) {
            throw new IllegalArgumentException();
        }
        this.gaussianKernelWidth = gaussianKernelWidth;
    }

    /**
     * Sets the radius of the Gaussian convolution kernel used to smooth the
     * source image prior to gradient calculation.
     *
     * @param gaussianKernelRadius a Gaussian kernel radius in pixels, must exceed 0.1f.
     */
    public void setGaussianKernelRadius(float gaussianKernelRadius) {
        if (gaussianKernelRadius < 0.1f) {
            throw new IllegalArgumentException();
        }
        this.gaussianKernelRadius = gaussianKernelRadius;
    }

    /**
     * Sets whether the luminance data extracted from the source image is
     * normalized by linearizing its histogram prior to edge extraction.
     *
     * @param contrastNormalized true if the contrast should be normalized,
     *                           false otherwise
     */
    public void setContrastNormalized(boolean contrastNormalized) {
        this.contrastNormalized = contrastNormalized;
    }

    // methods

    public void process() {
        width = sourceImage.getWidth();
        height = sourceImage.getHeight();

        assert width > 0 && height > 0;
        assert lowThreshold <= highThreshold;
        assert width >= 2 * gaussianKernelWidth && height >= 2 * gaussianKernelWidth;

        // the number of computational units are experimental values
        // that seem to work pretty well
        pt = new StatusBarProgressTracker("Canny", height + 450);

        pixelCount = width * height;
        initArrays();

        pt.unitsDone(10);

        readLuminance();

        pt.unitsDone(50);

        if (contrastNormalized) {
            normalizeContrast();
            pt.unitsDone(10);
        }

        computeGradients(gaussianKernelRadius, gaussianKernelWidth);

        int low = Math.round(lowThreshold * MAGNITUDE_SCALE);
        int high = Math.round(highThreshold * MAGNITUDE_SCALE);
        performHysteresis(low, high);
        pt.unitDone();

        thresholdEdges();
        pt.unitDone();

        writeEdges(luminance);
        pt.finished();
    }

    // private utility methods

    private void initArrays() {
        if (luminance == null || pixelCount != luminance.length) {
            luminance = new int[pixelCount];
            magnitude = new int[pixelCount];
        }
    }

    //NOTE: The elements of the method below (specifically the technique for
    //non-maximal suppression and the technique for gradient computation)
    //are derived from an implementation posted in the following forum (with the
    //clear intent of others using the code):
    //  http://forum.java.sun.com/thread.jspa?threadID=546211&start=45&tstart=0
    //My code effectively mimics the algorithm exhibited above.
    //Since I don't know the providence of the code that was posted it is a
    //possibility (though I think a very remote one) that this code violates
    //someone's intellectual property rights. If this concerns you feel free to
    //contact me for an alternative, though less efficient, implementation.
    private void computeGradients(float kernelRadius, int kernelWidth) {
        float[] blurredX = new float[pixelCount];
        float[] blurredY = new float[pixelCount];
        float[] gradX = new float[pixelCount];
        float[] gradY = new float[pixelCount];

        //generate the gaussian convolution masks
        float[] kernel = new float[kernelWidth];
        float[] diffKernel = new float[kernelWidth];
        int kwidth;
        for (kwidth = 0; kwidth < kernelWidth; kwidth++) {
            float g1 = gaussian(kwidth, kernelRadius);
            if (g1 <= GAUSSIAN_CUT_OFF && kwidth >= 2) {
                break;
            }
            float g2 = gaussian(kwidth - 0.5f, kernelRadius);
            float g3 = gaussian(kwidth + 0.5f, kernelRadius);
            kernel[kwidth] = (g1 + g2 + g3) / 3.0f / ((float) Math.TAU * kernelRadius * kernelRadius);
            diffKernel[kwidth] = g3 - g2;
        }

        pt.unitDone();

        int initX = kwidth - 1;
        int maxX = width - (kwidth - 1);
        int initY = width * (kwidth - 1);
        int maxY = width * (height - (kwidth - 1));

        //perform convolution in x and y directions across rows sequentially
        for (int y = initY; y < maxY; y += width) {
            for (int x = initX; x < maxX; x++) {
                int index = x + y;
                float sumX = luminance[index] * kernel[0];
                float sumY = sumX;
                for (int i = 1; i < kwidth; i++) {
                    int yOffset = i * width;
                    sumY += kernel[i] * (luminance[index - yOffset] + luminance[index + yOffset]);
                    sumX += kernel[i] * (luminance[index - i] + luminance[index + i]);
                }

                blurredY[index] = sumY;
                blurredX[index] = sumX;
            }
        }

        pt.unitsDone(200);

        for (int y = initY; y < maxY; y += width) {
            for (int x = initX; x < maxX; x++) {
                float sum = 0.0f;
                int index = x + y;
                for (int i = 1; i < kwidth; i++) {
                    sum += diffKernel[i] * (blurredY[index - i] - blurredY[index + i]);
                }

                gradX[index] = sum;
            }
        }

        pt.unitsDone(100);

        for (int y = initY; y < maxY; y += width) {
            for (int x = kwidth; x < width - kwidth; x++) {
                float sum = 0.0f;
                int index = x + y;
                int yOffset = width;
                for (int i = 1; i < kwidth; i++) {
                    sum += diffKernel[i] * (blurredX[index - yOffset] - blurredX[index + yOffset]);
                    yOffset += width;
                }

                gradY[index] = sum;
            }
        }

        pt.unitsDone(100);

        // precompute gradient magnitudes
        float[] gradMags = blurredX; // reuse blurredX to save pixelCount * 4 bytes of heap allocation
        for (int i = 0; i < pixelCount; i++) {
            float gx = gradX[i];
            float gy = gradY[i];
            gradMags[i] = ImageMath.hypot(gx, gy);
        }

        initX = kwidth;
        maxX = width - kwidth;
        initY = width * kwidth;
        maxY = width * (height - kwidth);
        for (int y = initY; y < maxY; y += width) {
            pt.unitDone();
            for (int x = initX; x < maxX; x++) {
                int index = x + y;
                int indexN = index - width;
                int indexS = index + width;
                int indexW = index - 1;
                int indexE = index + 1;
                int indexNW = indexN - 1;
                int indexNE = indexN + 1;
                int indexSW = indexS - 1;
                int indexSE = indexS + 1;

                float xGrad = gradX[index];
                float yGrad = gradY[index];
                float gradMag = gradMags[index];

                // perform non-maximal suppression
                float nMag = gradMags[indexN];
                float sMag = gradMags[indexS];
                float wMag = gradMags[indexW];
                float eMag = gradMags[indexE];
                float neMag = gradMags[indexNE];
                float seMag = gradMags[indexSE];
                float swMag = gradMags[indexSW];
                float nwMag = gradMags[indexNW];

                /*
                 * An explanation of what's happening here, for those who want
                 * to understand the source: This performs the "non-maximal
                 * suppression" phase of the Canny edge detection in which we
                 * need to compare the gradient magnitude to that in the
                 * direction of the gradient; only if the value is a local
                 * maximum do we consider the point as an edge candidate.
                 *
                 * We need to break the comparison into a number of different
                 * cases depending on the gradient direction so that the
                 * appropriate values can be used. To avoid computing the
                 * gradient direction, we use two simple comparisons: first we
                 * check that the partial derivatives have the same sign (1)
                 * and then we check which is larger (2). As a consequence, we
                 * have reduced the problem to one of four identical cases that
                 * each test the central gradient magnitude against the values at
                 * two points with 'identical support'; what this means is that
                 * the geometry required to accurately interpolate the magnitude
                 * of gradient function at those points has an identical
                 * geometry (upto right-angled-rotation/reflection).
                 *
                 * When comparing the central gradient to the two interpolated
                 * values, we avoid performing any divisions by multiplying both
                 * sides of each inequality by the greater of the two partial
                 * derivatives.
                 */

                // Determine whether (xGrad, yGrad) share the same sign
                boolean sameSign = (xGrad * yGrad) > 0;
                float absX = Math.abs(xGrad);
                float absY = Math.abs(yGrad);

                boolean isLocalMax;
                float centerVal;

                if (absX >= absY) {
                    centerVal = absX * gradMag;
                    float d = absX - absY;
                    float m1 = sameSign ? seMag : neMag;
                    float m2 = sameSign ? nwMag : swMag;
                    isLocalMax = centerVal >= (absY * m1 + d * eMag)
                        && centerVal > (absY * m2 + d * wMag);
                } else {
                    centerVal = absY * gradMag;
                    float d = absY - absX;
                    float m1 = sameSign ? seMag : neMag;
                    float m2 = sameSign ? nwMag : swMag;
                    float n1 = sameSign ? sMag : nMag;
                    float n2 = sameSign ? nMag : sMag;
                    isLocalMax = centerVal >= (absX * m1 + d * n1)
                        && centerVal > (absX * m2 + d * n2);
                }

                if (isLocalMax) {
                    magnitude[index] = gradMag >= MAGNITUDE_LIMIT ? MAGNITUDE_MAX : (int) (MAGNITUDE_SCALE * gradMag);
                    //NOTE: The orientation of the edge is not employed by this
                    //implementation. It is a simple matter to compute it at
                    //this point as: Math.atan2(yGrad, xGrad);
                } else {
                    magnitude[index] = 0;
                }
            }
        }
    }

    private static float gaussian(float x, float sigma) {
        return (float) Math.exp(-(x * x) / (2.0f * sigma * sigma));
    }

    private void performHysteresis(int low, int high) {
        //NOTE: this implementation reuses the data array to store both
        //luminance data from the image, and edge intensity from the processing.
        //This is done for memory efficiency, other implementations may wish
        //to separate these functions.
        Arrays.fill(luminance, 0);
        int[] stack = new int[pixelCount];

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (luminance[offset] == 0 && magnitude[offset] >= high) {
                    follow(offset, low, stack);
                }
                offset++;
            }
        }
    }

    private void follow(int startIdx, int threshold, int[] stack) {
        int top = 0;
        stack[top++] = startIdx;
        luminance[startIdx] = magnitude[startIdx];

        while (top > 0) {
            int currIdx = stack[--top];
            int cx = currIdx % width;
            int cy = currIdx / width;

            int x0 = Math.max(0, cx - 1);
            int x2 = Math.min(width - 1, cx + 1);
            int y0 = Math.max(0, cy - 1);
            int y2 = Math.min(height - 1, cy + 1);

            for (int y = y0; y <= y2; y++) {
                int rowOffset = y * width;
                for (int x = x0; x <= x2; x++) {
                    int neighborIdx = x + rowOffset;
                    if ((x != cx || y != cy)
                        && luminance[neighborIdx] == 0
                        && magnitude[neighborIdx] >= threshold) {
                        luminance[neighborIdx] = magnitude[neighborIdx];
                        stack[top++] = neighborIdx;
                    }
                }
            }
        }
    }

    private void thresholdEdges() {
        for (int i = 0; i < pixelCount; i++) {
            luminance[i] = luminance[i] > 0 ? EDGE_COLOR_WHITE : BG_COLOR_BLACK;
        }
    }

    // Integer fixed-point approximation of Rec. 601: (0.299*R + 0.587*G + 0.114*B)
    private static int luminance(int r, int g, int b) {
        return (19595 * r + 38470 * g + 7471 * b + 32768) >> 16;
    }

    private void readLuminance() {
        int type = sourceImage.getType();
        switch (type) {
            case BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB -> readRGBLuminance();
            case BufferedImage.TYPE_INT_ARGB_PRE -> readArgbPremultipliedLuminance();
            case BufferedImage.TYPE_BYTE_GRAY -> readByteGrayLuminance();
            case BufferedImage.TYPE_USHORT_GRAY -> readShortGrayLuminance();
            case BufferedImage.TYPE_3BYTE_BGR -> readBGRLuminance();
            default -> readGenericLuminance();
        }
    }

    private void readRGBLuminance() {
        int[] pixels = (int[]) sourceImage.getRaster().getDataElements(0, 0, width, height, null);
        for (int i = 0; i < pixelCount; i++) {
            int p = pixels[i];
            int r = (p & 0xFF_00_00) >> 16;
            int g = (p & 0xFF_00) >> 8;
            int b = p & 0xFF;
            luminance[i] = luminance(r, g, b);
        }
    }

    private void readArgbPremultipliedLuminance() {
        int[] pixels = (int[]) sourceImage.getRaster().getDataElements(0, 0, width, height, null);
        for (int i = 0; i < pixelCount; i++) {
            int p = pixels[i];
            int a = p >>> 24;
            int r = (p & 0xFF_00_00) >> 16;
            int g = (p & 0xFF_00) >> 8;
            int b = p & 0xFF;
            int lum = luminance(r, g, b);
            if (a != 255) {
                lum = (a == 0) ? 0 : Math.clamp((lum * 255 + (a / 2)) / a, 0, 255);
            }
            luminance[i] = lum;
        }
    }

    private void readByteGrayLuminance() {
        byte[] pixels = (byte[]) sourceImage.getRaster().getDataElements(0, 0, width, height, null);
        for (int i = 0; i < pixelCount; i++) {
            luminance[i] = (pixels[i] & 0xFF);
        }
    }

    private void readShortGrayLuminance() {
        short[] pixels = (short[]) sourceImage.getRaster().getDataElements(0, 0, width, height, null);
        for (int i = 0; i < pixelCount; i++) {
            luminance[i] = (pixels[i] & 0xFF_FF) >>> 8;
        }
    }

    private void readBGRLuminance() {
        byte[] pixels = (byte[]) sourceImage.getRaster().getDataElements(0, 0, width, height, null);
        int offset = 0;
        for (int i = 0; i < pixelCount; i++) {
            int b = pixels[offset++] & 0xFF;
            int g = pixels[offset++] & 0xFF;
            int r = pixels[offset++] & 0xFF;
            luminance[i] = luminance(r, g, b);
        }
    }

    private void readGenericLuminance() {
        int[] rgb = sourceImage.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixelCount; i++) {
            int p = rgb[i];
            luminance[i] = luminance((p >>> 16) & 0xFF, (p >>> 8) & 0xFF, p & 0xFF);
        }
    }

    private void normalizeContrast() {
        if (pixelCount == 0) {
            return;
        }

        int[] histogram = new int[256];
        for (int datum : luminance) {
            histogram[datum]++;
        }

        int[] remap = new int[256];
        int sum = 0;
        long halfPixelCount = pixelCount / 2;
        for (int i = 0; i < 256; i++) {
            sum += histogram[i];
            // Use long to prevent 32-bit overflow on images > 8.4 MP, and add halfPixelCount for rounding
            remap[i] = (int) (((long) sum * 255 + halfPixelCount) / pixelCount);
        }

        for (int i = 0; i < pixelCount; i++) {
            luminance[i] = remap[luminance[i]];
        }
    }

    private void writeEdges(int[] pixels) {
        if (edgesImage == null || edgesImage.getWidth() != width || edgesImage.getHeight() != height) {
            edgesImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        edgesImage.getRaster().setDataElements(0, 0, width, height, pixels);
    }
}
