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

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.WholeImageFilter;
import org.jdesktop.swingx.graphics.ColorUtilities;
import pixelitor.utils.ColorSpaces;

import java.awt.Color;

/**
 * Performs histogram equalization on a single channel of a given color space.
 */
public class EqualizeFilter extends WholeImageFilter {

    private final int numBins;
    private final Equalizer equalizer;

    /**
     * Defines the color space/channel combination used for equalization.
     */
    public enum Equalizer {
        /**
         * Equalizes the Y (luma) channel of YCbCr.
         */
        YCBCR("YCbCr/Y", 0) {
            @Override
            public float[] toColorSpace(int[] inPixels, int numPixels) {
                float[] ycbcrPixels = new float[numPixels * 3];
                for (int i = 0, j = 0; i < numPixels; i++, j += 3) {
                    float[] ycbcr = ColorSpaces.srgbToYCbCr(inPixels[i]);
                    // normalize Y from [0, 255] to [0, 1]
                    ycbcrPixels[j] = ycbcr[0] / 255.0f;
                    ycbcrPixels[j + 1] = ycbcr[1];
                    ycbcrPixels[j + 2] = ycbcr[2];
                }
                return ycbcrPixels;
            }

            @Override
            public void toSrgb(float[] ycbcrPixels, int[] outPixels, int numPixels) {
                float[] ycbcr = new float[3];
                for (int i = 0, j = 0; i < numPixels; i++, j += 3) {
                    // de-normalize Y from [0, 1] back to [0, 255]
                    ycbcr[0] = ycbcrPixels[j] * 255.0f;
                    ycbcr[1] = ycbcrPixels[j + 1];
                    ycbcr[2] = ycbcrPixels[j + 2];
                    outPixels[i] = ColorSpaces.ycbcrToSrgb(ycbcr);
                }
            }
        },
        /**
         * Equalizes the V (value) channel of HSV.
         */
        HSV("HSV/V", 2) {
            @Override
            public float[] toColorSpace(int[] inPixels, int numPixels) {
                float[] hsvPixels = new float[numPixels * 3];
                float[] hsv = new float[3];
                for (int i = 0, j = 0; i < numPixels; i++, j += 3) {
                    int rgb = inPixels[i];
                    Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsv);
                    hsvPixels[j] = hsv[0];
                    hsvPixels[j + 1] = hsv[1];
                    hsvPixels[j + 2] = hsv[2];
                }
                return hsvPixels;
            }

            @Override
            public void toSrgb(float[] hsvPixels, int[] outPixels, int numPixels) {
                for (int i = 0, j = 0; i < numPixels; i++, j += 3) {
                    outPixels[i] = Color.HSBtoRGB(hsvPixels[j], hsvPixels[j + 1], hsvPixels[j + 2]);
                }
            }
        },
        /**
         * Equalizes the L (luminance) channel of HSL.
         */
        HSL("HSL/L", 2) {
            @Override
            public float[] toColorSpace(int[] inPixels, int numPixels) {
                float[] hslPixels = new float[numPixels * 3];
                float[] hsl = new float[3];
                for (int i = 0, j = 0; i < numPixels; i++, j += 3) {
                    int rgb = inPixels[i];
                    ColorUtilities.RGBtoHSL((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsl);
                    hslPixels[j] = hsl[0];
                    hslPixels[j + 1] = hsl[1];
                    hslPixels[j + 2] = hsl[2];
                }
                return hslPixels;
            }

            @Override
            public void toSrgb(float[] hslPixels, int[] outPixels, int numPixels) {
                for (int i = 0, j = 0; i < numPixels; i++, j += 3) {
                    outPixels[i] = ColorUtilities.HSLtoRGB(hslPixels[j], hslPixels[j + 1], hslPixels[j + 2]).getRGB();
                }
            }
        },
        /**
         * Equalizes the L (lightness) channel of Oklab.
         */
        OKLAB("Oklab/L", 0) {
            @Override
            public float[] toColorSpace(int[] inPixels, int numPixels) {
                float[] oklabPixels = new float[numPixels * 3];
                ColorSpaces.srgbToOklabBulk(inPixels, oklabPixels);
                return oklabPixels;
            }

            @Override
            public void toSrgb(float[] oklabPixels, int[] outPixels, int numPixels) {
                ColorSpaces.oklabToSrgbBulk(oklabPixels, outPixels);
            }
        };

        private final String displayName;

        // the index of the channel to be equalized (0, 1, or 2)
        private final int channelIndex;

        Equalizer(String displayName, int channelIndex) {
            this.displayName = displayName;
            this.channelIndex = channelIndex;
        }

        /**
         * Converts from sRGB pixels to the target color space.
         */
        public abstract float[] toColorSpace(int[] inPixels, int numPixels);

        /**
         * Converts from the target color space back to sRGB pixels.
         */
        public abstract void toSrgb(float[] colorSpacePixels, int[] outPixels, int numPixels);

        public int getChannelIndex() {
            return channelIndex;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public EqualizeFilter(String name, int numBins, Equalizer equalizer) {
        super(name);
        this.numBins = numBins;
        this.equalizer = equalizer;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int numPixels = width * height;
        int[] outPixels = new int[numPixels];
        int channelIndex = equalizer.getChannelIndex();
        float scaleToBin = numBins - 1.0f;

        pt = createProgressTracker(5);

        // convert to target color space
        float[] colorSpacePixels = equalizer.toColorSpace(inPixels, numPixels);
        pt.unitDone();

        // build histogram for the target channel
        int[] histogram = buildHistogram(colorSpacePixels, numPixels, channelIndex, scaleToBin);
        pt.unitDone();

        // build cumulative distribution function
        int[] cdf = buildCdf(histogram);

        // find smallest non-zero cdf value
        int cdfMin = findFirstNonZero(cdf);

        // if there's no variation in the target channel, nothing to equalize
        if (numPixels - cdfMin == 0) {
            finishProgressTracker();
            return inPixels;
        }

        // create equalization lookup table
        float[] equalizationLut = createEqualizationLut(cdf, cdfMin, numPixels);
        pt.unitDone();

        // apply equalization to the target channel
        applyLut(colorSpacePixels, numPixels, channelIndex, scaleToBin, equalizationLut);
        pt.unitDone();

        // convert back to sRGB
        equalizer.toSrgb(colorSpacePixels, outPixels, numPixels);
        pt.unitDone();

        finishProgressTracker();
        return outPixels;
    }

    private int[] buildHistogram(float[] pixels, int numPixels, int channelIndex, float scaleToBin) {
        int[] histogram = new int[numBins];
        for (int i = 0; i < numPixels; i++) {
            int bin = toBin(pixels[i * 3 + channelIndex], scaleToBin);
            histogram[bin]++;
        }
        return histogram;
    }

    private int[] buildCdf(int[] histogram) {
        int[] cdf = new int[numBins];
        cdf[0] = histogram[0];
        for (int i = 1; i < numBins; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }
        return cdf;
    }

    private static int findFirstNonZero(int[] array) {
        for (int value : array) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private float[] createEqualizationLut(int[] cdf, int cdfMin, int numPixels) {
        float[] lut = new float[numBins];
        float cdfScale = 1.0f / (numPixels - cdfMin);
        for (int i = 0; i < numBins; i++) {
            lut[i] = Math.max(0.0f, (cdf[i] - cdfMin) * cdfScale);
        }
        return lut;
    }

    private void applyLut(float[] pixels, int numPixels, int channelIndex, float scaleToBin, float[] lut) {
        for (int i = 0; i < numPixels; i++) {
            int bin = toBin(pixels[i * 3 + channelIndex], scaleToBin);
            pixels[i * 3 + channelIndex] = lut[bin];
        }
    }

    private int toBin(float channelValue, float scaleToBin) {
        return ImageMath.clamp((int) (channelValue * scaleToBin + 0.5f), 0, numBins - 1);
    }
}
