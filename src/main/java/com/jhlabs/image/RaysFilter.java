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

import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A filter which produces the effect of light rays shining out of an image.
 */
public class RaysFilter extends MotionBlurOp {
    //    private float opacity = 1.0f;
    private float threshold = 0.0f;
    private float strength = 0.5f;
    //    private boolean raysOnly = false;
    private Colormap colormap;

    public RaysFilter(String filterName) {
        super(filterName);
    }

    /**
     * Set the threshold value.
     *
     * @param threshold the threshold value
     */
    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    /**
     * Set the strength of the rays.
     *
     * @param strength the strength.
     */
    public void setStrength(float strength) {
        this.strength = strength;
    }

//    /**
//     * Set whether to render only the rays.
//     *
//     * @param raysOnly true to render rays only.
//     */
//    public void setRaysOnly(boolean raysOnly) {
//        this.raysOnly = raysOnly;
//    }

    /**
     * Set the colormap to be used for the filter.
     *
     * @param colormap the colormap
     */
    public void setColormap(Colormap colormap) {
        this.colormap = colormap;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] pixels = new int[width];
        int[] srcPixels = new int[width];

        pt = createProgressTracker(3);
        BufferedImage rays = new BufferedImage(width, height, TYPE_INT_ARGB);

        int threshold3 = (int) (threshold * 3 * 255);
        for (int y = 0; y < height; y++) {
            getRGB(src, 0, y, width, 1, pixels);
            for (int x = 0; x < width; x++) {
                int rgb = pixels[x];
                int a = rgb & 0xff000000;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                int l = r + g + b;
                if (l < threshold3) {
                    pixels[x] = 0xff000000;
                } else {
                    l /= 3;
                    pixels[x] = a | (l << 16) | (l << 8) | l;
                }
            }
            setRGB(rays, 0, y, width, 1, pixels);
        }
        pt.unitDone();
        ProgressTracker savedTracker = pt;
        // do not track the super call, it is fast
        pt = ProgressTracker.NULL_TRACKER;

        rays = super.filter(rays, null);

        pt = savedTracker;
        pt.unitDone();

        for (int y = 0; y < height; y++) {
            getRGB(rays, 0, y, width, 1, pixels);
            getRGB(src, 0, y, width, 1, srcPixels);
            for (int x = 0; x < width; x++) {
                int rgb = pixels[x];
                int a = rgb & 0xff000000;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                if (colormap != null) {
                    int l = r + g + b;
                    rgb = colormap.getColor(l * strength * (1 / 3.0f));
                } else {
                    r = PixelUtils.max255((int) (r * strength));
                    g = PixelUtils.max255((int) (g * strength));
                    b = PixelUtils.max255((int) (b * strength));
                    rgb = a | (r << 16) | (g << 8) | b;
                }

                pixels[x] = rgb;
            }
            setRGB(rays, 0, y, width, 1, pixels);
        }
        pt.unitDone();
        finishProgressTracker();

        return rays;
    }

    @Override
    public String toString() {
        return "Stylize/Rays...";
    }
}
