/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Lazy;
import pixelitor.utils.Rnd;

import java.awt.Dimension;

/**
 * The available zoom levels
 */
public enum ZoomLevel {
    Z12("12.5%") {
        @Override
        public double asPercent() {
            return 12.5;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z18;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z12;
        }
    }, Z18("17.7%") { // 12.5 * sqrt(2)

        @Override
        public double asPercent() {
            return 17.67766952966369;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z25;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z12;
        }
    }, Z25("25%") {
        @Override
        public double asPercent() {
            return 25;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z35;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z18;
        }
    }, Z35("35.3%") {
        @Override
        public double asPercent() {
            return 35.35533905932738;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z50;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z25;
        }
    }, Z50("50%") {
        @Override
        public double asPercent() {
            return 50;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z71;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z35;
        }
    }, Z71("70.7%") {
        @Override
        public double asPercent() {
            return 70.71067811865476;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z100;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z50;
        }
    }, Z100("100%") {
        @Override
        public double asPercent() {
            return 100;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z141;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z71;
        }
    }, Z141("141.4%") {
        @Override
        public double asPercent() {
            return 141.4213562373095;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z200;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z100;
        }
    }, Z200("200%") {
        @Override
        public double asPercent() {
            return 200;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z283;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z141;
        }
    }, Z283("282.8%") {
        @Override
        public double asPercent() {
            return 282.842712474619;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z200;
        }
    }, Z400("400%") {
        @Override
        public double asPercent() {
            return 400;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z566;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z283;
        }
    }, Z566("565.7%") {
        @Override
        public double asPercent() {
            return 565.685424949238;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z800;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z400;
        }
    }, Z800("800%") {
        @Override
        public double asPercent() {
            return 800;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z1131;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z566;
        }
    }, Z1131("1131.4%") {
        @Override
        public double asPercent() {
            return 1131.370849898476;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z1600;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z800;
        }
    }, Z1600("1600%") {
        @Override
        public double asPercent() {
            return 1600;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z2263;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z1131;
        }
    }, Z2263("2262.7%") {
        @Override
        public double asPercent() {
            return 2262.741699796952;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z3200;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z1600;
        }
    }, Z3200("3200%") {
        @Override
        public double asPercent() {
            return 3200;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z4525;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z2263;
        }
    }, Z4525("4525.5%") {
        @Override
        public double asPercent() {
            return 4525.483399593904;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z6400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z3200;
        }
    }, Z6400("6400%") {
        @Override
        public double asPercent() {
            return 6400;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z6400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z4525;
        }
    };

    private final String guiName;

    ZoomLevel(String guiName) {
        this.guiName = guiName;
    }

    // The menuItem must be initialized only after the enum constructor
    // in order to make sure that it has a name
    private final Lazy<ZoomMenuItem> menuItem = Lazy.of(
            () -> new ZoomMenuItem(this));

    @Override
    public String toString() {
        return guiName;
    }

    public ZoomMenuItem getMenuItem() {
        return menuItem.get();
    }

    public abstract double asPercent();

    public abstract ZoomLevel zoomIn();

    public abstract ZoomLevel zoomOut();

    public static ZoomLevel getRandomZoomLevel() {
        return Rnd.chooseFrom(values());
    }

    public double getViewScale() {
        return asPercent() / 100.0;
    }

    public boolean allowPixelGrid() {
        return asPercent() > 1500;
    }

    /**
     * Calculate the optimal zoom level for a given canvas,
     * and possibly for a given auto zoom.
     */
    public static ZoomLevel calcZoom(Canvas canvas, AutoZoom autoZoom, boolean zoomInToFitSpace) {
        if (autoZoom == AutoZoom.ACTUAL_PIXELS) {
            return Z100;
        }
        if (autoZoom == null) {
            // if this is not an auto zoom, then the algorithm is the same
            // as for "Fit Space"
            autoZoom = AutoZoom.FIT_SPACE;
        }

        double idealZoomPercent = 100.0 / calcSizeRatio(canvas, autoZoom);

        ZoomLevel[] zoomLevels = values();
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
