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

    private boolean uniformChannelVariation = false;

    public PlasmaFilter(String filterName) {
        super(filterName);
    }

    public void setUniformChannelVariation(boolean uniformChannelVariation) {
        this.uniformChannelVariation = uniformChannelVariation;
    }

    public void setSeed(long newSeed) {
        this.random = new SplittableRandom(newSeed);
    }

    /**
     * Sets the roughness or amount of randomness applied during texture generation.
     */
    public void setTurbulence(float turbulence) {
        this.turbulence = turbulence;
    }

    /**
     * Sets the color mapping used to tint the generated procedural values.
     */
    public void setColormap(Colormap colormap) {
        this.colormap = colormap;
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

        if (uniformChannelVariation) {
            // apply the same variation to all channels
            int d = (int) (amount * random.nextDouble(-0.5, 0.5));
            r = PixelUtils.clamp(r + d);
            g = PixelUtils.clamp(g + d);
            b = PixelUtils.clamp(b + d);
        } else {
            // apply independent variations to each channel
            int dR = (int) (amount * random.nextDouble(-0.5, 0.5));
            int dG = (int) (amount * random.nextDouble(-0.5, 0.5));
            int dB = (int) (amount * random.nextDouble(-0.5, 0.5));
            r = PixelUtils.clamp(r + dR);
            g = PixelUtils.clamp(g + dG);
            b = PixelUtils.clamp(b + dB);
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

            int y1Offset = y1 * stride;
            int midYOffset = midY * stride;
            int y2Offset = y2 * stride;

            int tl = pixels[y1Offset + x1];
            int bl = pixels[y2Offset + x1];
            int tr = pixels[y1Offset + x2];
            int br = pixels[y2Offset + x2];

            if (x1 != x2) {
                pixels[midYOffset + x1] = changeColor(average(tl, bl), amount);
                pixels[midYOffset + x2] = changeColor(average(tr, br), amount);
            }

            if (y1 != y2) {
                pixels[y2Offset + midX] = changeColor(average(bl, br), amount);
                pixels[y1Offset + midX] = changeColor(average(tl, tr), amount);
            }

            int mm = average(average(tl, br), average(bl, tr));
            pixels[midYOffset + midX] = changeColor(mm, amount);

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

        int maxX = width - 1;
        int maxY = height - 1;

        // sets the seed pixels - one in each corner, and one in the
        // center of each edge, plus one in the center of the image
        outPixels[0] = randomRGB();
        outPixels[maxX] = randomRGB();
        outPixels[maxY * width] = randomRGB();
        outPixels[maxY * width + maxX] = randomRGB();
        outPixels[maxY / 2 * width + maxX / 2] = randomRGB();
        outPixels[maxY / 2 * width] = randomRGB();
        outPixels[maxY / 2 * width + maxX] = randomRGB();
        outPixels[maxX / 2] = randomRGB();
        outPixels[maxY * width + maxX / 2] = randomRGB();

        int numIterations = calcNumIterations(width, height);
        int workUnits = numIterations / ITERATIONS_PER_UNIT;

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

        if (colormap != null) {
            for (int i = 0; i < outPixels.length; i++) {
                outPixels[i] = colormap.getColor((outPixels[i] & 0xFF) / 255.0f);
            }
        }

        finishProgressTracker();
        return outPixels;
    }

    private static int calcNumIterations(int width, int height) {
        int maxSize = Math.max(width, height);

        // the maximum distance between start and end pixels (use
        // a minimum of 1 to safely handle 1x1 or 2x2 edge cases)
        int s = Math.max(1, maxSize - 1);

        // the highest depth recursive subdivisions (ceil(log2(s)) - 1)
        int d = Math.max(1, 31 - Integer.numberOfLeadingZeros(s - 1));

        // computes Sum_{i=1}^{d} (4^{i+1} - 1) / 3
        // using the geometric series formula: (16 * 4^d - 3d - 16) / 9
        return (int) (((1L << (2 * d + 4)) - 16 - 3 * d) / 9);
    }
}
