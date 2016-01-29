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

import pixelitor.ThreadPool;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.concurrent.Future;

/**
 * An abstract superclass for point filters. The interface is the same as the old RGBImageFilter.
 */
public abstract class PointFilter extends AbstractBufferedImageOp {
    protected PointFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        setDimensions(width, height);
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return grayFilter(src, dst);
        }

        int[] inPixels = ImageUtils.getPixelsAsArray(src);
        int[] outPixels = ImageUtils.getPixelsAsArray(dst);

        pt = createProgressTracker(height);
        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable calculateLineTask = () -> {
                for (int x = 0; x < width; x++) {
                    int index = finalY * width + x;
                    outPixels[index] = filterRGB(x, finalY, inPixels[index]);
                }
            };
            futures[y] = ThreadPool.submit(calculateLineTask);
        }

        ThreadPool.waitForFutures(futures, pt);
        finishProgressTracker();

        return dst;
    }

    public BufferedImage grayFilter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(height);
        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable calculateLineTask = () -> {
                int[] inPixels = new int[width];
                src.getRGB(0, finalY, width, 1, inPixels, 0, width);
                for (int x = 0; x < width; x++) {
                    inPixels[x] = filterRGB(x, finalY, inPixels[x]);
                }
                dst.setRGB(0, finalY, width, 1, inPixels, 0, width);
            };
            futures[y] = ThreadPool.submit(calculateLineTask);
        }
        ThreadPool.waitForFutures(futures, pt);
        finishProgressTracker();

        return dst;
    }

    public void setDimensions(int width, int height) {
    }

    public abstract int filterRGB(int x, int y, int rgb);
}
