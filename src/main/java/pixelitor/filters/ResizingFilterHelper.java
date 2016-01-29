/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.filters;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.SubtaskProgressTracker;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;

/**
 * For some filters it makes sense to apply them to a
 * downscaled image, and then scale the image back.
 */
public class ResizingFilterHelper {
    public enum ScaleUpQuality {
        BILINEAR_FAST {
            @Override
            public BufferedImage scaleUp(BufferedImage src, BufferedImage smallDest, double resizeFactor, ProgressTracker pt) {
                BufferedImage dest = ImageUtils.createImageWithSameColorModel(src);
                Graphics2D g2 = dest.createGraphics();
                g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
                g2.scale(resizeFactor, resizeFactor);
                g2.drawImage(smallDest, 0, 0, null);
                g2.dispose();

                pt.addUnits(getWorkUnits(resizeFactor));

                return dest;
            }

            @Override
            public int getWorkUnits(double resizeFactor) {
                return 1 + (int) (resizeFactor / 2); // estimate
            }
        }, BILINEAR12 {
            @Override
            public BufferedImage scaleUp(BufferedImage src, BufferedImage smallDest, double resizeFactor, ProgressTracker pt) {
                return ImageUtils.enlargeSmooth(smallDest, src.getWidth(), src.getHeight(),
                        VALUE_INTERPOLATION_BILINEAR, 1.2, pt);
            }

            @Override
            public int getWorkUnits(double resizeFactor) {
                return ImageUtils.getNumStepsForEnlargeSmooth(resizeFactor, 1.2);
            }
        }, BILINEAR11 { // highest quality with 1.1 steps

            @Override
            public BufferedImage scaleUp(BufferedImage src, BufferedImage smallDest, double resizeFactor, ProgressTracker pt) {
                return ImageUtils.enlargeSmooth(smallDest, src.getWidth(), src.getHeight(),
                        VALUE_INTERPOLATION_BILINEAR, 1.1, pt);
            }

            @Override
            public int getWorkUnits(double resizeFactor) {
                return ImageUtils.getNumStepsForEnlargeSmooth(resizeFactor, 1.1);
            }
        };

        public abstract BufferedImage scaleUp(BufferedImage src, BufferedImage smallDest, double resizeFactor, ProgressTracker pt);

        public abstract int getWorkUnits(double resizeFactor);
    }

    private final BufferedImage src;
    private final int srcWidth;
    private final int srcHeight;
    private double resizeFactor = 1.0;

    public ResizingFilterHelper(BufferedImage src) {
        this.src = src;
        srcWidth = src.getWidth();
        srcHeight = src.getHeight();
    }

    public int getResizeWorkUnits(ScaleUpQuality quality) {
        // count one for the scaling down
        // don't count the filter
        return 1 + quality.getWorkUnits(resizeFactor);
    }

    public boolean shouldResize() {
        boolean resize = false;

        int numPixels = srcWidth * srcHeight;
        int resizeThreshold = 600_000;

        if (numPixels > resizeThreshold) {
            int ratio = numPixels / resizeThreshold;
            resize = true;
            resizeFactor = 1 + Math.sqrt(ratio);
        }
        return resize;
    }

    public double getResizeFactor() {
        return resizeFactor;
    }

    public BufferedImage invoke(ScaleUpQuality quality, BufferedImageOp filter, ProgressTracker pt, int filterUnits) {
        assert resizeFactor > 1.0;
        assert onlyNullTrackersHaveFilterUnits(filter, filterUnits);

        BufferedImage dest;

        // scale down
        BufferedImage smallSrc = getDownscaledSource();
        pt.unitDone();

        // filter
        BufferedImage smallDest = filter.filter(smallSrc, null);
        pt.addUnits(filterUnits);

        // scale up
        dest = quality.scaleUp(src, smallDest, resizeFactor, pt);

        return dest;
    }

    public BufferedImage getDownscaledSource() {
        // For the downscaling there is no quality improvement if it is done
        // in multiple steps, so this is always done the fast way.
        int smallWidth = (int) (srcWidth / resizeFactor);
        int smallHeight = (int) (srcHeight / resizeFactor);
        BufferedImage smallSrc = ImageUtils.createSysCompatibleImage(smallWidth, smallHeight);
        Graphics2D g = smallSrc.createGraphics();
        g.scale(1.0 / resizeFactor, 1.0 / resizeFactor);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return smallSrc;
    }

    public ProgressTracker createFilterTracker(ProgressTracker realTracker, int allocatedFilterUnits) {
        // this method assumes that the filter is a regular filter with "height" units
        double smallHeight = src.getHeight() / resizeFactor;
        return createFilterTracker(realTracker, allocatedFilterUnits, smallHeight);
    }

    @SuppressWarnings("MethodMayBeStatic")
    public ProgressTracker createFilterTracker(ProgressTracker realTracker, int allocatedFilterUnits, double realFilterUnits) {
        double ratio = allocatedFilterUnits / realFilterUnits;
        ProgressTracker filterTracker = new SubtaskProgressTracker(ratio, realTracker);
        return filterTracker;
    }

    private static boolean onlyNullTrackersHaveFilterUnits(BufferedImageOp filter, int filterUnits) {
        if (filterUnits > 0) {
            if (filter instanceof AbstractBufferedImageOp) {
                return ((AbstractBufferedImageOp) filter).getProgressTracker()
                        == ProgressTracker.NULL_TRACKER;
            }
        }
        return true;
    }
}
