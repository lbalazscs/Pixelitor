/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import pixelitor.Canvas;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.menus.view.ZoomMenu;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * Zoom levels that are automatically calculated based on the available space
 */
public enum AutoZoom {
    SPACE("Fit Space", ZoomMenu.FIT_SPACE_TOOLTIP) {
        @Override
        public double calcImageToDesktopRatio(double hor, double ver) {
            return Math.max(hor, ver);
        }
    }, WIDTH("Fit Width", "Fit the width of the image to the available space") {
        @Override
        public double calcImageToDesktopRatio(double hor, double ver) {
            return hor;
        }
    }, HEIGHT("Fit Height", "Fit the height of the image to the available space") {
        @Override
        public double calcImageToDesktopRatio(double hor, double ver) {
            return ver;
        }
    }, ACTUAL("Actual Pixels", ZoomMenu.ACTUAL_PIXELS_TOOLTIP) {
        @Override
        public double calcImageToDesktopRatio(double hor, double ver) {
            throw new IllegalStateException("should not be called");
        }
    };

    public static final Action ACTUAL_PIXELS_ACTION = ACTUAL.asAction();
    public static final Action FIT_HEIGHT_ACTION = HEIGHT.asAction();
    public static final Action FIT_WIDTH_ACTION = WIDTH.asAction();
    public static final Action FIT_SPACE_ACTION = SPACE.asAction();

    private final String guiName;
    private final String toolTip;

    AutoZoom(String guiName, String toolTip) {
        this.guiName = guiName;
        this.toolTip = toolTip;
    }

    public abstract double calcImageToDesktopRatio(double hor, double ver);

    private Action asAction() {
        AbstractAction action = new AbstractAction(guiName) {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenComps.fitActiveTo(AutoZoom.this);
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, toolTip);
        return action;
    }

    public ZoomLevel calcZoom(Canvas canvas, boolean zoomInToFitSpace) {
        if (this == ACTUAL) {
            return ZoomLevel.Z100;
        }

        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();

        Dimension desktopSize = ImageArea.getSize();
        double desktopWidth = desktopSize.getWidth();
        double desktopHeight = desktopSize.getHeight();

        double canvasToDesktopHorRatio = canvasWidth / desktopWidth;
        // TODO what about the tabbed interface
        int internalFrameHeightFactor = 35;
        double canvasToDesktopVerRatio = canvasHeight / (desktopHeight - internalFrameHeightFactor);

        double imageToDesktopRatio = calcImageToDesktopRatio(
                canvasToDesktopHorRatio, canvasToDesktopVerRatio);

        double idealZoomPercent = 100.0 / imageToDesktopRatio;
        ZoomLevel[] zoomLevels = ZoomLevel.values();
        ZoomLevel maximallyZoomedOut = zoomLevels[0];

        if (maximallyZoomedOut.getPercentValue() > idealZoomPercent) {
            // the image is so big that it will have scroll bars even
            // if it is maximally zoomed out
            return maximallyZoomedOut;
        }

        ZoomLevel lastOK = maximallyZoomedOut;
        // iterate all the zoom levels from zoomed out to zoomed in
        for (ZoomLevel level : zoomLevels) {
            if (level.getPercentValue() > idealZoomPercent) {
                // found one that is too much zoomed in
                return lastOK;
            }
            if (!zoomInToFitSpace) { // we don't want to zoom in more than 100%
                if (lastOK == ZoomLevel.Z100) {
                    return ZoomLevel.Z100;
                }
            }
            lastOK = level;
        }
        // if we get here, the image is so small that even at maximal zoom
        // it fits the available space: set it then to the maximal zoom
        return lastOK;
    }
}
