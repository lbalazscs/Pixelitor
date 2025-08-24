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

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * A filter which uses a another image as a mask to produce a halftoning effect.
 */
public class HalftoneFilter extends AbstractBufferedImageOp {

    public static final int GRID_TRIANGLE = 0;
    public static final int GRID_SQUARE = 1;
    public static final int GRID_RINGS = 2;

    private float softness = 0.1f;
    private boolean invert;
    private boolean monochrome;
    private BufferedImage mask;
    private int gridType = GRID_TRIANGLE;
    private double cx, cy;

    // cached data for GRID_RINGS mode
    private int[] maskPixelCache;
    private float firstRingRadius;

    public HalftoneFilter(String name) {
        super(name);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        if (mask == null) {
            return dst;
        }

        int maskWidth = mask.getWidth();
        int maskHeight = mask.getHeight();

        float softnessRange = 255 * softness;

        int[] inPixels = new int[width];
        float[] thetaScales = null;

        if (gridType == GRID_RINGS) {
            thetaScales = precomputeRingData(width, height, maskWidth, maskHeight);
        }

        int[] maskRowPixels = new int[maskWidth];

        pt = createProgressTracker(height);
        for (int y = 0; y < height; y++) {
            getRGB(src, 0, y, width, 1, inPixels);

            // for grid modes, get the current mask row based on y
            if (gridType == GRID_SQUARE || gridType == GRID_TRIANGLE) {
                getRGB(mask, 0, y % maskHeight, maskWidth, 1, maskRowPixels);
            }

            boolean offset = (gridType == GRID_TRIANGLE) && (y / maskHeight) % 2 != 0;

            for (int x = 0; x < width; x++) {
                int maskRGB;
                if (gridType == GRID_RINGS) {
                    maskRGB = getRingsMaskValue(x, y, maskWidth, maskHeight, thetaScales);
                } else { // GRID_SQUARE or GRID_TRIANGLE
                    maskRGB = getGridMaskValue(x, offset, maskWidth, maskRowPixels);
                }

                inPixels[x] = halftonePixel(inPixels[x], maskRGB, softnessRange);
            }

            setRGB(dst, 0, y, width, 1, inPixels);
            pt.unitDone();
        }
        finishProgressTracker();

        // release memory used by the cache
        this.maskPixelCache = null;

        return dst;
    }

    /**
     * Pre-calculates data needed for the rings grid mode.
     */
    private float[] precomputeRingData(int width, int height, int maskWidth, int maskHeight) {
        // the first ring is defined with half the standard radius and has
        // a direct mapping that puts a single, undistorted dot at the center
        this.firstRingRadius = maskHeight / 2.0f;

        // pre-load the entire mask into an array for efficient access
        this.maskPixelCache = new int[maskWidth * maskHeight];
        getRGB(mask, 0, 0, maskWidth, maskHeight, this.maskPixelCache);

        // pre-calculate scaling factors for theta for each ring
        // the maximum radius is the distance from the center to the farthest corner of the image
        double r00 = Math.hypot(cx, cy); // distance to corner (0, 0)
        double r10 = Math.hypot(width - cx, cy); // distance to corner (width, 0)
        double r01 = Math.hypot(cx, height - cy); // distance to corner (0, height)
        double r11 = Math.hypot(width - cx, height - cy); // distance to corner (width, height)
        int maxRadius = (int) Math.ceil(Math.max(Math.max(r00, r10), Math.max(r01, r11)));

        double maxAdjustedRadius = maxRadius - firstRingRadius;
        int numRings = (maxAdjustedRadius <= 0) ? 0 : (int) (maxAdjustedRadius / maskHeight) + 1;

        float[] thetaScales = new float[numRings];
        for (int i = 0; i < numRings; i++) {
            float ringRadius = firstRingRadius + (i + 0.5f) * maskHeight;
            float circumference = (float) (2.0 * Math.PI * ringRadius);
            // calculate the integer number of dots that best fits on this ring's circumference
            int numDots = Math.max(1, Math.round(circumference / maskWidth));
            // this scale factor will convert an angle (theta) into a mask x-coordinate
            thetaScales[i] = (float) (numDots * maskWidth / (2.0 * Math.PI));
        }
        return thetaScales;
    }

    /**
     * Gets the mask pixel value for a given coordinate in rings mode.
     */
    private int getRingsMaskValue(int x, int y, int maskWidth, int maskHeight, float[] thetaScales) {
        // calculate polar coordinates relative to the image center
        double dx = x - cx;
        double dy = y - cy;
        double r = FastMath.hypot(dx, dy);

        if (r < firstRingRadius) {
            // innermost ring: a single dot mapped concentrically
            int maskX = (int) (dx + maskWidth / 2.0);
            int maskY = (int) (dy + maskHeight / 2.0);

            if (maskX >= 0 && maskX < maskWidth && maskY >= 0 && maskY < maskHeight) {
                return maskPixelCache[maskY * maskWidth + maskX];
            }
            // fallback for coordinates outside the mask, though this should not happen
            return 0;
        }

        // outer rings: map polar coordinates to the mask
        double theta = FastMath.atan2(dy, dx);
        if (theta < 0) {
            theta += 2 * Math.PI;
        }

        double adjustedR = r - firstRingRadius;

        // determine which ring this pixel belongs to
        int ring = (int) (adjustedR / maskHeight);
        if (ring >= thetaScales.length) {
            ring = thetaScales.length - 1;
        }
        // handle case where there are no outer rings
        if (ring < 0) {
            return 0;
        }
        float thetaScale = thetaScales[ring];

        // map radius to the mask's y-coordinate (radial tiling)
        int maskY = ((int) adjustedR) % maskHeight;
        // map angle to the mask's x-coordinate using the scale for the current ring
        int maskX = ((int) (theta * thetaScale)) % maskWidth;

        return maskPixelCache[maskY * maskWidth + maskX];
    }

    /**
     * Gets the mask pixel value for a given coordinate in grid mode.
     */
    private static int getGridMaskValue(int x, boolean offset, int maskWidth, int[] maskRowPixels) {
        int maskX = x % maskWidth;
        if (offset) {
            maskX = (maskX + maskWidth / 2) % maskWidth;
        }
        return maskRowPixels[maskX];
    }

    /**
     * Calculates the final halftone pixel color based on the input and mask values.
     */
    private int halftonePixel(int inRGB, int maskRGB, float softnessRange) {
        int processedMaskRGB = invert ? (maskRGB ^ 0xffffff) : maskRGB;

        if (monochrome) {
            int v = PixelUtils.brightness(processedMaskRGB);
            int iv = PixelUtils.brightness(inRGB);

            // the mask image is used as a threshold map
            float f = 1 - ImageMath.smoothStep(iv - softnessRange, iv + softnessRange, v);
            int a = (int) (255 * f);
            return (inRGB & 0xff000000) | (a << 16) | (a << 8) | a;
        }

        int ir = (inRGB >> 16) & 0xff;
        int ig = (inRGB >> 8) & 0xff;
        int ib = inRGB & 0xff;
        int mr = (processedMaskRGB >> 16) & 0xff;
        int mg = (processedMaskRGB >> 8) & 0xff;
        int mb = processedMaskRGB & 0xff;
        int r = (int) (255 * (1 - ImageMath.smoothStep(ir - softnessRange, ir + softnessRange, mr)));
        int g = (int) (255 * (1 - ImageMath.smoothStep(ig - softnessRange, ig + softnessRange, mg)));
        int b = (int) (255 * (1 - ImageMath.smoothStep(ib - softnessRange, ib + softnessRange, mb)));
        return (inRGB & 0xff000000) | (r << 16) | (g << 8) | b;
    }

    /**
     * Set the softness of the effect in the range 0..1.
     */
    public void setSoftness(float softness) {
        this.softness = softness;
    }

    /**
     * Set the halftone mask.
     */
    public void setMask(BufferedImage mask) {
        this.mask = mask;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    /**
     * Set whether to do monochrome halftoning.
     */
    public void setMonochrome(boolean monochrome) {
        this.monochrome = monochrome;
    }

    /**
     * Sets the grid type for the halftoning effect.
     */
    public void setGridType(int gridType) {
        this.gridType = gridType;
    }

    public void setCenter(Point2D center) {
        cx = center.getX();
        cy = center.getY();
    }

    @Override
    public String toString() {
        return "Stylize/Halftone...";
    }
}
