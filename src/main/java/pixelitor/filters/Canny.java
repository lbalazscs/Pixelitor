/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters;

import pd.CannyEdgeDetector;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Dialogs;
import pixelitor.utils.MemoryInfo;

import java.awt.image.BufferedImage;

/**
 * Canny edge detector - see http://en.wikipedia.org/wiki/Canny_edge_detector
 * based on CannyEdgeDetector by Tom Gibara - http://www.tomgibara.com/computer-vision/canny-edge-detector
 */
public class Canny extends FilterWithParametrizedGUI {
    private final RangeParam lowThreshold = new RangeParam("Low Threshold", 1, 1000, 250);
    private final RangeParam highThreshold = new RangeParam("High Threshold", 1, 1000, 750);
    private final RangeParam gaussianKernelWidth = new RangeParam("Gaussian Kernel Width", 2, 50, 16);
    private final RangeParam gaussianKernelRadius = new RangeParam("Gaussian Kernel Radius", 1, 10, 2);
    private final BooleanParam contrastNormalized = new BooleanParam("Contrast Normalized", false);

    public Canny() {
        super("Canny Edge Detector", true, false);
        setParamSet(new ParamSet(
                lowThreshold,
                highThreshold,
                gaussianKernelWidth,
                gaussianKernelRadius,
                contrastNormalized
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int width = src.getWidth();
        int height = src.getHeight();
        long numPixels = width * height;
        // 6 arrays with 4-byte data type
        int estimatedMemoryMB = (int) (6 * numPixels * 4 / MemoryInfo.ONE_MEGABYTE);
        estimatedMemoryMB *= 1.8; // found experimentally, this is still needed to prevent OutOfMemory errors
        System.gc(); // we need this.
        MemoryInfo memoryInfo = new MemoryInfo();
        int availableMemoryMB = (int) (memoryInfo.getMaxMemoryMB() - memoryInfo.getUsedMemoryMB());

//        System.out.println("Canny::doTransform: availableMemoryMB = " + availableMemoryMB + ", estimatedMemoryMB = " + estimatedMemoryMB);

        if (estimatedMemoryMB > availableMemoryMB) {
            Dialogs.showInfoDialog("Not enough memory",
                    "This image is too large for the Canny edge detection algorithm.\n" +
                            "Press Cancel in the following dialog and try with smaller images.\n" +
                            "Available memory = " + availableMemoryMB + " megabytes, memory needed for this image = " + estimatedMemoryMB + " megabytes.");
            dest = src;
            return dest;
        }

        // do not cache this object because it holds a lot of memory!
        CannyEdgeDetector detector = new CannyEdgeDetector();

        detector.setLowThreshold(lowThreshold.getValueAsPercentage());
        detector.setHighThreshold(highThreshold.getValueAsPercentage());
        detector.setContrastNormalized(contrastNormalized.getValue());
        detector.setGaussianKernelRadius(gaussianKernelRadius.getValueAsFloat());
        detector.setGaussianKernelWidth(gaussianKernelWidth.getValue());

        detector.setSourceImage(src);


        detector.process();
        dest = detector.getEdgesImage();

        return dest;
    }
}