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
     * Sets the radius of the first kernel, and hence the amount of blur. The bigger the radius, the longer this filter will take.
     *
     * @param radius1 the radius of the blur in pixels.
     */
    public void setRadius1(float radius1) {
        this.radius1 = radius1;
    }

    /**
     * Sets the radius of the second kernel, and hence the amount of blur. The bigger the radius, the longer this filter will take.
     *
     * @param radius2 the radius of the blur in pixels.
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

        workUnits += singleBlurUnit / 2; // subtract

        if (doNormalize()) {
            workUnits += (int) (singleBlurUnit * 0.16); // normalize
        }

        pt = createProgressTracker(workUnits);

        if (radius1 > 0.0f) {
            BoxBlurFilter blur = new BoxBlurFilter(radius1, radius1, 3, filterName);
            blur.setProgressTracker(pt);
            image1 = blur.filter(src, null);
        } else {
            image1 = src;
        }

        Graphics2D dstG;
        if (radius2 > 0.0f) {
            BoxBlurFilter blur = new BoxBlurFilter(radius2, radius2, 3, filterName);
            blur.setProgressTracker(pt);
            dst = blur.filter(src, dst);
            dstG = dst.createGraphics();
        } else {
            dstG = dst.createGraphics();
            dstG.drawImage(src, 0, 0, null);
        }

        dstG.setComposite(new SubtractComposite(1.0f));
        dstG.drawImage(image1, 0, 0, null);
        dstG.dispose();

        pt.unitsDone(singleBlurUnit / 2);

        if (doNormalize()) {
            ImageUtils.normalizeImage(dst);
        }

        finishProgressTracker();

        return dst;
    }

    private boolean doNormalize() {
        return normalize && radius1 != radius2;
    }
}
