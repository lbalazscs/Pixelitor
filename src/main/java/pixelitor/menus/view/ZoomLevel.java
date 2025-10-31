/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.Themes;
import pixelitor.utils.Rnd;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * A discrete zoom level in a predefined sequence of steps.
 */
public class ZoomLevel {
    private static final double[] ZOOM_PERCENTAGES = {
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

    public static final ZoomLevel[] zoomLevels;

    static {
        zoomLevels = new ZoomLevel[ZOOM_PERCENTAGES.length];
        for (int i = 0; i < zoomLevels.length; i++) {
            zoomLevels[i] = new ZoomLevel(ZOOM_PERCENTAGES[i], i);
        }

        // link zoom levels in the chain
        for (int i = 0; i < zoomLevels.length; i++) {
            // the lowest level's 'nextOut' points to itself: can't zoom out further
            int prevIndex = Math.max(0, i - 1);
            zoomLevels[i].setNextOut(zoomLevels[prevIndex]);

            // the highest level's 'nextIn' points to itself: can't zoom in further
            int nextIndex = Math.min(zoomLevels.length - 1, i + 1);
            zoomLevels[i].setNextIn(zoomLevels[nextIndex]);
        }
    }

    public static final ZoomLevel ACTUAL_SIZE = zoomLevels[16]; // 100%
    public static final ZoomLevel HALF_SIZE = zoomLevels[12];   // 50%
    public static final ZoomLevel QUARTER_SIZE = zoomLevels[8]; // 25%
    public static final ZoomLevel EIGHTH_SIZE = zoomLevels[4];  // 12.5%

    private ZoomLevel nextIn;
    private ZoomLevel nextOut;
    private final double percent;
    private final String displayText;
    private final double scale;
    private final int sliderValue;

    private ZoomLevel(double percent, int sliderValue) {
        this.percent = percent;
        this.scale = percent / 100.0;
        this.sliderValue = sliderValue;

        DecimalFormat formatter = percent < 100
            ? new DecimalFormat("0.##")
            : new DecimalFormat("0.#");
        this.displayText = formatter.format(percent) + "%";
    }

    private void setNextIn(ZoomLevel nextIn) {
        this.nextIn = nextIn;
    }

    private void setNextOut(ZoomLevel nextOut) {
        this.nextOut = nextOut;
    }

    public ZoomLevel zoomIn() {
        return nextIn;
    }

    public ZoomLevel zoomOut() {
        return nextOut;
    }

    public double getPercent() {
        return percent;
    }

    public static ZoomLevel getRandomZoomLevel() {
        return Rnd.chooseFrom(zoomLevels);
    }

    public double getScale() {
        return scale;
    }

    public int getSliderValue() {
        return sliderValue;
    }

    /**
     * Returns whether the pixel grid should be displayed at this zoom level.
     */
    public boolean allowPixelGrid() {
        return getPercent() > 1500;
    }

    @Override
    public String toString() {
        return displayText;
    }

    /**
     * Calculates the best-fitting zoom level for the given canvas,
     * and possibly for a given auto zoom.
     */
    public static ZoomLevel calcBestFitZoom(Canvas canvas, AutoZoom autoZoom,
                                            boolean allowUpscalingToFit) {
        if (autoZoom == AutoZoom.ACTUAL_PIXELS) {
            return ACTUAL_SIZE;
        }
        if (autoZoom == null) {
            // if this isn't an auto zoom, then the
            // algorithm is the same as for "Fit Space"
            autoZoom = AutoZoom.FIT_SPACE;
        }

        double targetZoomPercent = 100.0 / calcSizeRatio(canvas, autoZoom);

        // if not allowed to zoom in to fit, cap the zoom at 100%
        if (!allowUpscalingToFit) {
            targetZoomPercent = Math.min(targetZoomPercent, 100.0);
        }

        // find the largest zoom percentage that is less than or equal to the target
        int index = Arrays.binarySearch(ZOOM_PERCENTAGES, targetZoomPercent);

        int bestFitIndex;
        if (index >= 0) {
            // an exact match was found
            bestFitIndex = index;
        } else {
            // we want the element right before the insertion point
            int insertionPoint = -index - 1;
            bestFitIndex = Math.max(0, insertionPoint - 1);
        }

        return zoomLevels[bestFitIndex];
    }

    private static double calcSizeRatio(Canvas canvas, AutoZoom autoZoom) {
        Dimension availableArea = ImageArea.getSize();
        double availableWidth = availableArea.getWidth();
        double availableHeight = availableArea.getHeight();

        // adjust for frame decoration height if in frames mode
        if (ImageArea.isActiveMode(ImageArea.Mode.FRAMES)) {
            availableHeight -= Themes.getActive().getFrameDecorationHeight();
        }

        double horRatio = canvas.getWidth() / availableWidth;
        double verRatio = canvas.getHeight() / availableHeight;

        return autoZoom.calcRatio(horRatio, verRatio);
    }
}
