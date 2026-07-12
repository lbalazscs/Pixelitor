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

import java.awt.image.BufferedImage;

/**
 * A filter that reproduces the dot-pattern look of CMY halftone-printed images.
 */
public class ColorHalftoneFilter extends AbstractBufferedImageOp {
    // grid-unit offsets to the current cell and its four orthogonal neighbors: {center, left, right, up, down}.
    public static final float[] NEIGHBOR_OFFSETS_X = new float[]{0, -1, 1, 0, 0};
    public static final float[] NEIGHBOR_OFFSETS_Y = new float[]{0, 0, 0, -1, 1};

    // pixels of anti-aliasing at a dot's edge
    public static final int DOT_EDGE_FEATHER = 1;

    private final float dotRadius;
    private final float[] screenAngles;

    /**
     * Constructs a ColorHalftoneFilter with the specified filter name and parameters.
     *
     * @param filterName         the name of the filter
     * @param dotRadius          the base radius, in pixels, used to size the halftone dots
     * @param cyanScreenAngle    the cyan screen angle (in radians)
     * @param magentaScreenAngle the magenta screen angle (in radians)
     * @param yellowScreenAngle  the yellow screen angle (in radians)
     */
    public ColorHalftoneFilter(String filterName, float dotRadius, float cyanScreenAngle, float magentaScreenAngle, float yellowScreenAngle) {
        super(filterName);

        assert dotRadius > 0;
        this.dotRadius = dotRadius;

        // channel 0 (red) drives the cyan screen, channel 1 (green)
        // the magenta screen, and channel 2 (blue) the yellow screen:
        // ink amount is the complement of the RGB primary
        this.screenAngles = new float[]{cyanScreenAngle, magentaScreenAngle, yellowScreenAngle};
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(height);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        float gridSize = (float) (2 * dotRadius * ImageMath.SQRT_2);
        float halfGridSize = gridSize / 2;
        int[] outPixels = new int[width];
        int[] inPixels = getRGB(src, 0, 0, width, height, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0, ix = y * width; x < width; x++, ix++) {
                outPixels[x] = (inPixels[ix] & 0xFF_00_00_00) | 0xFF_FF_FF;
            }
            // For each ink, resample the image onto a grid rotated to that ink's screen angle.
            // Each grid cell is one halftone dot whose radius grows with local ink coverage.
            for (int channel = 0; channel < 3; channel++) {
                int shift = 16 - 8 * channel;
                int mask = 0x00_00_00_FF << shift;
                float angle = screenAngles[channel];
                float sin = (float) Math.sin(angle);
                float cos = (float) Math.cos(angle);

                for (int x = 0; x < width; x++) {
                    // transform x,y into halftone screen coordinate space
                    float tx = x * cos + y * sin;
                    float ty = -x * sin + y * cos;

                    // find the nearest grid point
                    tx = tx - ImageMath.mod(tx - halfGridSize, gridSize) + halfGridSize;
                    ty = ty - ImageMath.mod(ty - halfGridSize, gridSize) + halfGridSize;

                    float coverage = 1;

                    // TODO: Efficiency warning: Because the dots overlap, we need to check neighboring grid squares.
                    // We check all four neighbors, but in practice only one can ever overlap any given point.
                    for (int i = 0; i < 5; i++) {
                        // find neighboring grid point
                        float ttx = tx + NEIGHBOR_OFFSETS_X[i] * gridSize;
                        float tty = ty + NEIGHBOR_OFFSETS_Y[i] * gridSize;
                        // transform back into image space
                        float ntx = ttx * cos - tty * sin;
                        float nty = ttx * sin + tty * cos;
                        // clamp to the image
                        int nx = ImageMath.clamp((int) ntx, 0, width - 1);
                        int ny = ImageMath.clamp((int) nty, 0, height - 1);
                        int argb = inPixels[ny * width + nx];
                        int channelValue = (argb >> shift) & 0xFF;
                        float normalizedChannel = channelValue / 255.0f;
                        float inkAmount = 1 - normalizedChannel * normalizedChannel;
                        float cellDotRadius = (float) (inkAmount * halfGridSize * ImageMath.SQRT_2);
                        float dx = x - ntx;
                        float dy = y - nty;
                        float dx2 = dx * dx;
                        float dy2 = dy * dy;
                        float dist = (float) Math.sqrt(dx2 + dy2);
                        float nCoverage = 1 - ImageMath.smoothStep(dist, dist + DOT_EDGE_FEATHER, cellDotRadius);

                        // a dot can be large enough to spill into an adjacent cell, so a pixel's final
                        // coverage is the minimum coverage from the nearest cell and its four neighbors
                        coverage = Math.min(coverage, nCoverage);
                    }

                    int channelByte = (int) (255 * coverage);
                    outPixels[x] = (outPixels[x] & ~mask) | (channelByte << shift);
                }
            }
            setRGB(dst, 0, y, width, 1, outPixels);

            pt.unitDone();
        }
        finishProgressTracker();

        return dst;
    }
}
