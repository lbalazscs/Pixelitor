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

import java.util.SplittableRandom;

import static com.jhlabs.image.ImageMath.average;

/**
 * Generates plasma-like textures using a recursive diamond-square algorithm.
 * See https://en.wikipedia.org/wiki/Diamond-square_algorithm
 */
public class PlasmaFilter extends WholeImageFilter {
    private static final int ITERATIONS_PER_UNIT = 200_000;
    private int iterationCount = 0;

    public float turbulence = 1.0f;
    private Colormap colormap;
    private SplittableRandom random;
    private boolean useColormap = false;

    private boolean lessColors = false;

    public PlasmaFilter(String filterName) {
        super(filterName);
    }

    public void setLessColors(boolean lessColors) {
        this.lessColors = lessColors;
    }

    public void setSeed(long newSeed) {
        this.random = new SplittableRandom(newSeed);
    }

    /**
     * Sets the turbulence of the texture.
     *
     * @param turbulence the turbulence of the texture.
     */
    public void setTurbulence(float turbulence) {
        this.turbulence = turbulence;
    }

    /**
     * Sets the colormap to be used for the filter.
     *
     * @param colormap the colormap
     */
    public void setColormap(Colormap colormap) {
        this.colormap = colormap;
    }

    public void setUseColormap(boolean useColormap) {
        this.useColormap = useColormap;
    }

    private int randomRGB() {
        return 0xFF_00_00_00 | random.nextInt(0x1_00_00_00);
    }

    private int changeColor(int rgb, float amount) {
        if (amount < 0.1f) {
            return rgb;
        }

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (lessColors) {
            // apply the same variation to all channels
            int d = (int) (amount * random.nextDouble(-0.5, 0.5));
            r = PixelUtils.clamp(r + d);
            g = PixelUtils.clamp(g + d);
            b = PixelUtils.clamp(b + d);
        } else {
            // apply independent variations to each channel
            int d1 = (int) (amount * random.nextDouble(-0.5, 0.5));
            int d2 = (int) (amount * random.nextDouble(-0.5, 0.5));
            int d3 = (int) (amount * random.nextDouble(-0.5, 0.5));
            r = PixelUtils.clamp(r + d1);
            g = PixelUtils.clamp(g + d2);
            b = PixelUtils.clamp(b + d3);
        }

        return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
    }

    private boolean plasmaStep(int x1, int y1, int x2, int y2, int[] pixels, int stride, int depth, float amount) {
        iterationCount++;
        if (iterationCount == ITERATIONS_PER_UNIT) {
            iterationCount = 0;
            pt.unitDone();
        }

        int midX = (x1 + x2) / 2;
        int midY = (y1 + y2) / 2;

        if (depth == 0) {
            if (x1 == x2 && y1 == y2) {
                return true;
            }

            int y1Stride = y1 * stride;
            int midYStride = midY * stride;
            int y2Stride = y2 * stride;

            int tl = pixels[y1Stride + x1];
            int bl = pixels[y2Stride + x1];
            int tr = pixels[y1Stride + x2];
            int br = pixels[y2Stride + x2];

            if (x1 != x2) {
                pixels[midYStride + x1] = changeColor(average(tl, bl), amount);
                pixels[midYStride + x2] = changeColor(average(tr, br), amount);
            }

            if (y1 != y2) {
                pixels[y2Stride + midX] = changeColor(average(bl, br), amount);
                pixels[y1Stride + midX] = changeColor(average(tl, tr), amount);
            }

            int mm = average(average(tl, br), average(bl, tr));
            pixels[midYStride + midX] = changeColor(mm, amount);

            return x2 - x1 >= 3 || y2 - y1 >= 3;
        }

        // recursively process quadrants
        // top left
        plasmaStep(x1, y1, midX, midY, pixels, stride, depth - 1, amount);
        // bottom left
        plasmaStep(x1, midY, midX, y2, pixels, stride, depth - 1, amount);
        // top right
        plasmaStep(midX, y1, x2, midY, pixels, stride, depth - 1, amount);
        // bottom right
        return plasmaStep(midX, midY, x2, y2, pixels, stride, depth - 1, amount);
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];

        if (width == 1 && height == 1) {
            // the algorithm doesn't work in this case, so just return the input
            outPixels[0] = inPixels[0];
            return outPixels;
        }

        int w1 = width - 1;
        int h1 = height - 1;

        // puts in the seed pixels - one in each corner, and one in the
        // center of each edge, plus one in the center of the image
        outPixels[0 * width + 0] = randomRGB();
        outPixels[0 * width + w1] = randomRGB();
        outPixels[h1 * width + 0] = randomRGB();
        outPixels[h1 * width + w1] = randomRGB();
        outPixels[h1 / 2 * width + w1 / 2] = randomRGB();
        outPixels[h1 / 2 * width + 0] = randomRGB();
        outPixels[h1 / 2 * width + w1] = randomRGB();
        outPixels[0 * width + w1 / 2] = randomRGB();
        outPixels[h1 * width + w1 / 2] = randomRGB();

        int estimatedIterations = calcEstimatedIterations(width, height);
        int workUnits = estimatedIterations / ITERATIONS_PER_UNIT;

        if (workUnits > 0) {
            pt = createProgressTracker(workUnits);
        } else {
            pt = ProgressTracker.NO_OP_TRACKER;
        }

        iterationCount = 0;

        // now we recurse through the image, going further each time
        int depth = 1;
        while (true) {
            float amount = (256.0f / (2.0f * depth)) * turbulence;
            if (!plasmaStep(0, 0, width - 1, height - 1, outPixels, width, depth, amount)) {
                break;
            }
            depth++;
        }

        if (useColormap && colormap != null) {
            for (int i = 0; i < outPixels.length; i++) {
                outPixels[i] = colormap.getColor((outPixels[i] & 0xff) / 255.0f);
            }
        }

        finishProgressTracker();
        return outPixels;
    }

    private static int calcEstimatedIterations(int width, int height) {
        int maxSize = Math.max(width, height);

        // empirically determined values
        if (maxSize <= 129) {
            // ignores smaller thresholds, they won't have a progress bar
            return 7_278;
        } else if (maxSize <= 257) {
            return 29_123;
        } else if (maxSize <= 513) {
            return 116_504;
        } else if (maxSize <= 1025) {
            return 466_029;
        } else if (maxSize <= 2049) {
            return 1_864_130;
        } else if (maxSize <= 4097) {
            return 7_456_535;
        } else if (maxSize <= 8193) {
            return 29_826_156;
        } else {
            // we don't expect images with a
            // max size of more than 16 000 pixels
            return 119_304_641;
        }
    }
}
