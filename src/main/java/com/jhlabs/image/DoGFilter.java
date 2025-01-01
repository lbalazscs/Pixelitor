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

import com.jhlabs.composite.SubtractComposite;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Edge detection by difference of Gaussians.
 *
 * @author Jerry Huxtable
 */
public class DoGFilter extends AbstractBufferedImageOp {
    private float radius1 = 1;
    private float radius2 = 2;
    private boolean normalize = true;

    public DoGFilter(String filterName) {
        super(filterName);
    }

    /**
     * Set the radius of the first kernel, and hence the amount of blur. The bigger the radius, the longer this filter will take.
     *
     * @param radius1 the radius of the blur in pixels.
     * @min-value 0
     * @max-value 100+
     */
    public void setRadius1(float radius1) {
        this.radius1 = radius1;
    }

    /**
     * Set the radius of the second kernel, and hence the amount of blur. The bigger the radius, the longer this filter will take.
     *
     * @param radius2 the radius of the blur in pixels.
     * @min-value 0
     * @max-value 100+
     */
    public void setRadius2(float radius2) {
        this.radius2 = radius2;
    }

    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage image1;

        int singleBlurUnit = 3 * (width + height);
        int workUnits = 0;
        if (radius1 > 0.0f) {
            workUnits += singleBlurUnit;
        }
        if (radius2 > 0.0f) {
            workUnits += singleBlurUnit;
        }
        workUnits += (singleBlurUnit / 2); // subtract
        if (doNormalize()) {
            workUnits = (int) (workUnits + singleBlurUnit * 0.16); // normalize
        }
        pt = createProgressTracker(workUnits);

        if (radius1 > 0.0f) {
            BoxBlurFilter blur = new BoxBlurFilter(radius1, radius1, 3, filterName);
            blur.setProgressTracker(pt);
            image1 = blur.filter(src, null);
        } else {
            image1 = src;
        }

        if (radius2 > 0.0f) {
            BoxBlurFilter blur = new BoxBlurFilter(radius2, radius2, 3, filterName);
            blur.setProgressTracker(pt);
            dst = blur.filter(src, null);
        } else {
            dst = ImageUtils.copyImage(src);
        }

        Graphics2D g2d = dst.createGraphics();
        g2d.setComposite(new SubtractComposite(1.0f));
        g2d.drawImage(image1, 0, 0, null);
        g2d.dispose();

        pt.unitsDone(singleBlurUnit / 2);

        if (doNormalize()) {
            int[] pixels = null;
            int max = 0;
            for (int y = 0; y < height; y++) {
                pixels = getRGB(dst, 0, y, width, 1, pixels);
                for (int x = 0; x < width; x++) {
                    int rgb = pixels[x];
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (r > max) {
                        max = r;
                    }
                    if (g > max) {
                        max = g;
                    }
                    if (b > max) {
                        max = b;
                    }
                }
            }

            if (max != 0) { // all-black images cannot be normalized
                for (int y = 0; y < height; y++) {
                    pixels = getRGB(dst, 0, y, width, 1, pixels);
                    for (int x = 0; x < width; x++) {
                        int rgb = pixels[x];
                        int r = (rgb >> 16) & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int b = rgb & 0xff;
                        r = r * 255 / max;
                        g = g * 255 / max;
                        b = b * 255 / max;
                        pixels[x] = (rgb & 0xff000000) | (r << 16) | (g << 8) | b;
                    }
                    setRGB(dst, 0, y, width, 1, pixels);
                }
            }
        }

        finishProgressTracker();

        return dst;
    }

    private boolean doNormalize() {
        return normalize && radius1 != radius2;
    }

    @Override
    public String toString() {
        return "Edges/Difference of Gaussians...";
    }
}
