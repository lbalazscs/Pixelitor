/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.menus.view;

import pixelitor.Canvas;
import pixelitor.gui.AutoZoom;
import pixelitor.gui.ImageArea;
import pixelitor.utils.Rnd;

import java.awt.Dimension;
import java.text.DecimalFormat;

/**
 * The available zoom levels
 */
public class ZoomLevel {
    public static final ZoomLevel[] zoomLevels;
    private static final double[] percentValues = {
        6.25,
        7.432544468767007,
        8.838834764831844,
        10.511205190671431,
        12.5,
        14.865088937534013,
        17.67766952966369, // 12.5 * sqrt(2)
        21.022410381342862,
        25.0,
        29.730177875068026,
        35.35533905932738,
        42.044820762685724,
        50.0,
        59.46035575013605,
        70.71067811865476,
        84.08964152537145,
        100,
        118.9207115002721,
        141.4213562373095,
        168.1792830507429,
        200.0,
        237.8414230005442,
        282.842712474619,
        336.3585661014858,
        400.0,
        475.6828460010884,
        565.685424949238,
        672.7171322029716,
        800.0,
        951.3656920021768,
        1131.370849898476,
        1345.4342644059432,
        1600.0,
        1902.7313840043537,
        2262.741699796952,
        2690.8685288118863,
        3200.0,
        3805.4627680087074,
        4525.483399593904,
        5381.737057623773,
        6400.0};

    static {
        zoomLevels = new ZoomLevel[percentValues.length];
        for (int i = 0; i < zoomLevels.length; i++) {
            zoomLevels[i] = new ZoomLevel(percentValues[i], i);
        }
        for (int i = 0; i < zoomLevels.length; i++) {
            if (i == 0) {
                zoomLevels[i].setOut(zoomLevels[0]);
                zoomLevels[i].setIn(zoomLevels[i + 1]);
            } else if (i == zoomLevels.length - 1) {
                zoomLevels[i].setOut(zoomLevels[i - 1]);
                zoomLevels[i].setIn(zoomLevels[i]);
            } else {
                zoomLevels[i].setOut(zoomLevels[i - 1]);
                zoomLevels[i].setIn(zoomLevels[i + 1]);
            }
        }
    }

    public static final ZoomLevel Z100 = zoomLevels[16];
    public static final ZoomLevel Z50 = zoomLevels[12];
    public static final ZoomLevel Z25 = zoomLevels[8];
    public static final ZoomLevel Z12 = zoomLevels[4];

    private ZoomLevel in;
    private ZoomLevel out;
    private final double percent;
    private final String guiName;
    private final double scale;
    private final int sliderValue;

    private ZoomLevel(double percent, int sliderValue) {
        this.percent = percent;
        this.scale = percent / 100.0;
        this.sliderValue = sliderValue;
        DecimalFormat format;
        if (percent < 100) {
            format = new DecimalFormat("0.##");
        } else {
            format = new DecimalFormat("0.#");
        }
        this.guiName = format.format(percent) + "%";
    }

    public boolean isSpecial() {
        return sliderValue % 4 == 0;
    }

    private void setIn(ZoomLevel in) {
        this.in = in;
    }

    private void setOut(ZoomLevel out) {
        this.out = out;
    }

    public ZoomLevel zoomIn() {
        return in;
    }

    public ZoomLevel zoomOut() {
        return out;
    }

    public double asPercent() {
        return percent;
    }

    public static ZoomLevel getRandomZoomLevel() {
        return Rnd.chooseFrom(zoomLevels);
    }

    public double getViewScale() {
        return scale;
    }

    public int getSliderValue() {
        return sliderValue;
    }

    public boolean allowPixelGrid() {
        return asPercent() > 1500;
    }

    @Override
    public String toString() {
        return guiName;
    }

    /**
     * Calculate the optimal zoom level for a given canvas,
     * and possibly for a given auto zoom.
     */
    public static ZoomLevel calcBestFor(Canvas canvas, AutoZoom autoZoom,
                                        boolean zoomInToFitSpace) {
        if (autoZoom == AutoZoom.ACTUAL_PIXELS) {
            return Z100;
        }
        if (autoZoom == null) {
            // if this is not an auto zoom, then the algorithm is the same
            // as for "Fit Space"
            autoZoom = AutoZoom.FIT_SPACE;
        }

        double idealZoomPercent = 100.0 / calcSizeRatio(canvas, autoZoom);

        ZoomLevel maximallyZoomedOut = zoomLevels[0];

        if (maximallyZoomedOut.asPercent() > idealZoomPercent) {
            // the image is so big that it will need scroll bars
            // even if it is maximally zoomed out
            return maximallyZoomedOut;
        }

        ZoomLevel lastOK = maximallyZoomedOut;
        // iterate all the zoom levels from zoomed out to zoomed in
        for (ZoomLevel level : zoomLevels) {
            if (level.asPercent() > idealZoomPercent) {
                // found one that is too much zoomed in
                return lastOK;
            }
            if (!zoomInToFitSpace) { // we don't want to zoom in more than 100%
                if (lastOK == Z100) {
                    return Z100;
                }
            }
            lastOK = level;
        }
        // if we get here, the image is so small that even at maximal zoom
        // it fits in the available space: set it then to the maximal zoom
        return lastOK;
    }

    private static double calcSizeRatio(Canvas canvas, AutoZoom autoZoom) {
        Dimension availableArea = ImageArea.getSize();
        double availableWidth = availableArea.getWidth();
        double availableHeight = availableArea.getHeight();
        if (ImageArea.currentModeIs(ImageArea.Mode.FRAMES)) {
            int internalFrameHeight = 35; // Nimbus
            availableHeight -= internalFrameHeight;
        }

        double horRatio = canvas.getWidth() / availableWidth;
        double verRatio = canvas.getHeight() / availableHeight;

        return autoZoom.selectRatio(horRatio, verRatio);
    }
}
