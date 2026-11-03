/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import pixelitor.progress.ProgressTracker;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static pixelitor.utils.Threads.onPool;

/**
 * Static utility methods related to resizing images.
 */
public class ImageResizeUtils {
    private ImageResizeUtils() {
    }

    public static CompletableFuture<BufferedImage> resizeAsync(BufferedImage img,
                                                               int targetWidth,
                                                               int targetHeight) {
        return CompletableFuture.supplyAsync(() ->
            resize(img, targetWidth, targetHeight), onPool);
    }

    public static BufferedImage resize(BufferedImage img, int targetWidth, int targetHeight) {
        boolean progressiveBilinear = targetWidth < img.getWidth() / 2
            || targetHeight < img.getHeight() / 2;
        return scaleImage(img, targetWidth, targetHeight, VALUE_INTERPOLATION_BICUBIC, progressiveBilinear);
    }

    // From the Filthy Rich Clients book

    /**
     * Convenience method that returns a scaled instance of the
     * provided BufferedImage.
     *
     * @param img                 the original image to be scaled
     * @param targetWidth         the desired width of the scaled instance,
     *                            in pixels
     * @param targetHeight        the desired height of the scaled instance,
     *                            in pixels
     * @param hint                one of the rendering hints that corresponds to
     *                            RenderingHints.KEY_INTERPOLATION (e.g.
     *                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
     *                            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
     *                            RenderingHints.VALUE_INTERPOLATION_BICUBIC)
     * @param progressiveBilinear if true, this method will use a multi-step
     *                            scaling technique that provides higher quality than the usual
     *                            one-step technique (only useful in down-scaling cases, where
     *                            targetWidth or targetHeight is
     *                            smaller than the original dimensions)
     * @return a scaled version of the original BufferedImage
     */
    private static BufferedImage scaleImage(BufferedImage img,
                                            int targetWidth, int targetHeight,
                                            Object hint,
                                            boolean progressiveBilinear) {
        assert img != null;

        int prevW = img.getWidth();
        int prevH = img.getHeight();

        if (targetWidth >= prevW || targetHeight >= prevH) {
            progressiveBilinear = false;
        }

        int type = img.getType();

        BufferedImage ret = img;
        int w, h;
        boolean isTranslucent = img.getTransparency() != Transparency.OPAQUE;

        if (progressiveBilinear) {
            // use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        BufferedImage scratchImage = null;
        Graphics2D g = null;
        do {
            if (progressiveBilinear && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (progressiveBilinear && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (scratchImage == null || isTranslucent) {
                // use a single scratch buffer for all iterations
                // and then copy to the final, correctly-sized image
                // before returning
                scratchImage = new BufferedImage(w, h, type);
                g = scratchImage.createGraphics();
            }
            g.setRenderingHint(KEY_INTERPOLATION, hint);
            g.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
            prevW = w;
            prevH = h;

            ret = scratchImage;
        } while (w != targetWidth || h != targetHeight);

        if (g != null) {
            g.dispose();
        }

        // if we used a scratch buffer that is larger than our target size,
        // create an image of the right size and copy the results into it
        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g = scratchImage.createGraphics();
            g.drawImage(ret, 0, 0, null);
            g.dispose();
            ret = scratchImage;
        }

        return ret;
    }

    /**
     * Also an iterative approach, but using even smaller steps.
     */
    public static BufferedImage enlargeSmoothly(BufferedImage src,
                                                int targetWidth, int targetHeight,
                                                Object hint, double step, ProgressTracker pt) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        double factorX = targetWidth / (double) srcWidth;
        double factorY = targetHeight / (double) srcHeight;

        // they should be the same, but rounding errors can cause small discrepancies
        assert Math.abs(factorX - factorY) < 0.05;

        double factor = (factorX + factorY) / 2.0;
        assert factor > 1.0; // this only makes sense for enlarging
        double progress = 1.0;
        double lastStep = factor / step;
        BufferedImage last = src;
        AffineTransform stepScale = AffineTransform.getScaleInstance(step, step);
        while (progress < lastStep) {
            progress = progress * step;
            int newSrcWidth = (int) (srcWidth * progress);
            int newSrcHeight = (int) (srcHeight * progress);
            BufferedImage tmp = new BufferedImage(newSrcWidth, newSrcHeight, src.getType());
            Graphics2D g = tmp.createGraphics();
            if (hint != null) {
                g.setRenderingHint(KEY_INTERPOLATION, hint);
            }

            g.drawImage(last, stepScale, null);
            g.dispose();

            BufferedImage willBeForgotten = last;
            last = tmp;
            willBeForgotten.flush();
            pt.unitDone();
        }

        // do the last step: resize exactly to the target values
        BufferedImage retVal = new BufferedImage(targetWidth, targetHeight, src.getType());
        Graphics2D g = retVal.createGraphics();
        if (hint != null) {
            g.setRenderingHint(KEY_INTERPOLATION, hint);
        }

        g.drawImage(last, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        pt.unitDone();

        return retVal;
    }

    /**
     * Returns the number of steps (progress tracking work units)
     * required for smooth enlargement.
     */
    public static int calcEnlargeSmoothlySteps(double resizeFactor, double step) {
        double progress = 1.0;
        double lastStep = resizeFactor / step;
        int retVal = 1; // for the final step
        while (progress < lastStep) {
            progress = progress * step;
            retVal++;
        }
        return retVal;
    }
}
