/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.impl;

import com.jhlabs.image.WholeImageFilter;
import pixelitor.filters.Morphology;
import pixelitor.utils.ColorSpaces;

import java.awt.Color;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * The implementation of the {@link Morphology} filter.
 */
public class MorphologyFilter extends WholeImageFilter {
    private int iterations = 1;

    public static final int OP_ERODE = 1;  // reduces bright areas
    public static final int OP_DILATE = 2; // expands bright areas
    private int op;

    public static final int KERNEL_DIAMOND = 3; // includes cross-shaped neighboring pixels
    public static final int KERNEL_SQUARE = 4; // includes all 8 surrounding pixels
    private int kernel;

    // apply erosion or dilation independently to each R, G, B color channel
    public static final int CHANNEL_RGB = 0;
    // apply erosion or dilation to the value (V) channel while preserving hue and saturation
    public static final int CHANNEL_HSV = 1;
    // apply erosion or dilation to the luma (Y) channel while preserving chroma
    public static final int CHANNEL_YCBCR = 2;
    private int channel = CHANNEL_RGB;

    public MorphologyFilter(String filterName) {
        super(filterName);
    }

    public void setKernel(int kernel) {
        this.kernel = kernel;
    }

    public void setOp(int op) {
        this.op = op;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        return switch (channel) {
            case CHANNEL_RGB -> filterRgb(width, height, inPixels);
            case CHANNEL_HSV -> filterHsv(width, height, inPixels);
            case CHANNEL_YCBCR -> filterYCbCr(width, height, inPixels);
            default -> inPixels;
        };
    }

    private int[] filterRgb(int width, int height, int[] inPixels) {
        // uses one array as the source and the other as the destination
        // for the first iteration, then swap them so the previous destination
        // becomes the new source for the second iteration, and so on
        int[] outPixels = new int[width * height];
        int[] srcPixels = inPixels;
        int[] dstPixels = outPixels;

        pt = createProgressTracker(iterations);
        for (int it = 0; it < iterations; it++) {
            int index = 0; // the index of the processed pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // the final alpha, red, green, blue values
                    int a, r, g, b;

                    if (op == OP_DILATE) {
                        // initialize with the minimum value for the max operation
                        a = 0;
                        r = 0;
                        g = 0;
                        b = 0;
                    } else { // OP_ERODE
                        // initialize with the maximum value for the min operation
                        a = 0xFF;
                        r = 0xFF;
                        g = 0xFF;
                        b = 0xFF;
                    }

                    // examine neighboring pixels
                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = y + dy;
                        if (0 <= ny && ny < height) {
                            int xOffset = ny * width;
                            for (int dx = -1; dx <= 1; dx++) {
                                if (kernel == KERNEL_DIAMOND) {
                                    // ignore the corner neighbours
                                    if (dx == dy && dx != 0) {
                                        continue;
                                    }
                                    if (dx == -dy && dx != 0) {
                                        continue;
                                    }
                                }

                                int nx = x + dx;
                                if (0 <= nx && nx < width) {
                                    int neighborIndex = xOffset + nx;

                                    // read from the source array for the current iteration
                                    int neighborRgb = srcPixels[neighborIndex];
                                    int neighborA = (neighborRgb >> 24) & 0xFF;
                                    int neighborR = (neighborRgb >> 16) & 0xFF;
                                    int neighborG = (neighborRgb >> 8) & 0xFF;
                                    int neighborB = neighborRgb & 0xFF;

                                    if (op == OP_ERODE) {
                                        a = min(a, neighborA);
                                        r = min(r, neighborR);
                                        g = min(g, neighborG);
                                        b = min(b, neighborB);
                                    } else { // OP_DILATE
                                        a = max(a, neighborA);
                                        r = max(r, neighborR);
                                        g = max(g, neighborG);
                                        b = max(b, neighborB);
                                    }
                                }
                            }
                        }
                    }
                    // write to the destination array for the current iteration
                    dstPixels[index++] = a << 24 | r << 16 | g << 8 | b;
                }
            }
            pt.unitDone();

            // swap the source and destination arrays for the next iteration
            int[] temp = srcPixels;
            srcPixels = dstPixels;
            dstPixels = temp;
        }
        finishProgressTracker();

        return srcPixels; // this always contains the final, correct pixel data
    }

    private int[] filterYCbCr(int width, int height, int[] inPixels) {
        int numPixels = width * height;

        // convert the whole image to YCbCr, preserving alpha
        float[] yChannel = new float[numPixels];
        float[] cbChannel = new float[numPixels];
        float[] crChannel = new float[numPixels];
        int[] aChannel = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
            int p = inPixels[i];
            aChannel[i] = (p >> 24) & 0xFF;

            float[] ycbcr = ColorSpaces.srgbToYCbCr(p);
            yChannel[i] = ycbcr[0];
            cbChannel[i] = ycbcr[1];
            crChannel[i] = ycbcr[2];
        }

        // apply erosion or dilation to the Y channel
        float[] srcY = yChannel;
        float[] dstY = new float[numPixels];

        pt = createProgressTracker(iterations);
        for (int it = 0; it < iterations; it++) {
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float luma; // the final Y value for this pixel

                    if (op == OP_DILATE) {
                        luma = 0.0f; // min value for max operation
                    } else { // OP_ERODE
                        luma = 255.0f; // max value for min operation
                    }

                    // examine neighboring pixels
                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = y + dy;
                        if (0 <= ny && ny < height) {
                            int xOffset = ny * width;
                            for (int dx = -1; dx <= 1; dx++) {
                                if (kernel == KERNEL_DIAMOND) {
                                    if (dx == dy && dx != 0) {
                                        continue;
                                    }
                                    if (dx == -dy && dx != 0) {
                                        continue;
                                    }
                                }

                                int nx = x + dx;
                                if (0 <= nx && nx < width) {
                                    int neighborIndex = xOffset + nx;
                                    float neighborY = srcY[neighborIndex];

                                    if (op == OP_ERODE) {
                                        luma = min(luma, neighborY);
                                    } else { // OP_DILATE
                                        luma = max(luma, neighborY);
                                    }
                                }
                            }
                        }
                    }
                    dstY[index++] = luma;
                }
            }
            pt.unitDone();

            // swap source and destination for next iteration
            float[] temp = srcY;
            srcY = dstY;
            dstY = temp;
        }
        finishProgressTracker();

        // srcY now holds the final Y channel data.

        // recombine and convert back to RGB
        int[] outPixels = new int[numPixels];
        float[] ycbcr = new float[3];
        for (int i = 0; i < numPixels; i++) {
            ycbcr[0] = srcY[i]; // use the modified Y value
            ycbcr[1] = cbChannel[i];
            ycbcr[2] = crChannel[i];

            int rgb = ColorSpaces.ycbcrToSrgb(ycbcr); // alpha=0xFF
            // combine with the original alpha
            outPixels[i] = (aChannel[i] << 24) | (rgb & 0x00FFFFFF);
        }

        return outPixels;
    }

    private int[] filterHsv(int width, int height, int[] inPixels) {
        int numPixels = width * height;

        // convert the whole image to HSV, preserving alpha
        float[] hChannel = new float[numPixels];
        float[] sChannel = new float[numPixels];
        float[] vChannel = new float[numPixels];
        int[] aChannel = new int[numPixels];

        float[] hsbvals = new float[3];
        for (int i = 0; i < numPixels; i++) {
            int p = inPixels[i];
            aChannel[i] = (p >> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;

            Color.RGBtoHSB(r, g, b, hsbvals);
            hChannel[i] = hsbvals[0];
            sChannel[i] = hsbvals[1];
            vChannel[i] = hsbvals[2];
        }

        // apply erosion or dilation to the V channel
        float[] srcV = vChannel;
        float[] dstV = new float[numPixels];

        pt = createProgressTracker(iterations);
        for (int it = 0; it < iterations; it++) {
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float v; // the final V value for this pixel

                    if (op == OP_DILATE) {
                        v = 0.0f; // min value for max operation
                    } else { // OP_ERODE
                        v = 1.0f; // max value for min operation
                    }

                    // examine neighboring pixels
                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = y + dy;
                        if (0 <= ny && ny < height) {
                            int xOffset = ny * width;
                            for (int dx = -1; dx <= 1; dx++) {
                                if (kernel == KERNEL_DIAMOND) {
                                    if (dx == dy && dx != 0) {
                                        continue;
                                    }
                                    if (dx == -dy && dx != 0) {
                                        continue;
                                    }
                                }

                                int nx = x + dx;
                                if (0 <= nx && nx < width) {
                                    int neighborIndex = xOffset + nx;
                                    float neighborV = srcV[neighborIndex];

                                    if (op == OP_ERODE) {
                                        v = min(v, neighborV);
                                    } else { // OP_DILATE
                                        v = max(v, neighborV);
                                    }
                                }
                            }
                        }
                    }
                    dstV[index++] = v;
                }
            }
            pt.unitDone();

            // swap source and destination for next iteration
            float[] temp = srcV;
            srcV = dstV;
            dstV = temp;
        }
        finishProgressTracker();

        // srcV now holds the final V channel data.

        // recombine and convert back to RGB
        int[] outPixels = new int[numPixels];
        for (int i = 0; i < numPixels; i++) {
            float h = hChannel[i];
            float s = sChannel[i];
            float v = srcV[i]; // use the modified V value

            int rgb = Color.HSBtoRGB(h, s, v); // alpha=0xFF
            // combine with the original alpha
            outPixels[i] = (aChannel[i] << 24) | (rgb & 0x00FFFFFF);
        }

        return outPixels;
    }
}
