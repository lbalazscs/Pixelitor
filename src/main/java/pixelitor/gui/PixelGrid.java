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

package pixelitor.gui;

import pixelitor.Views;
import pixelitor.utils.Messages;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;

import static pixelitor.gui.ImageArea.Mode.FRAMES;

/**
 * Utility class with methods related to the pixel grid.
 */
public class PixelGrid {
    static boolean visible = false;

    /**
     * Enables or disables the visibility of the pixel grid.
     */
    public static void setVisible(boolean visible) {
        if (PixelGrid.visible == visible) {
            return;
        }
        PixelGrid.visible = visible;
        if (visible) {
            pixelGridEnabled();
        } else {
            Views.repaintVisible();
        }
    }

    // called when the global pixel grid switch was turned on
    public static void pixelGridEnabled() {
        if (ImageArea.isActiveMode(FRAMES)) {
            if (Views.anyViewAllowsPixelGrid()) {
                Views.repaintAll();
            } else {
                showNoPixelGridMessage();
            }
        } else { // tabs: check only the active view
            View view = Views.getActive();
            if (view != null) {
                if (view.getZoomLevel().allowsPixelGrid()) {
                    view.repaint();
                } else {
                    showNoPixelGridMessage();
                }
            }
        }
    }

    private static void showNoPixelGridMessage() {
        String msg = """
            The pixel grid consists of lines between the pixels,
            and is shown only if the zoom is at least 1600%.""";
        Messages.showInfo("Pixel Grid", msg, ImageArea.getUI());
    }

    /**
     * Draws the pixel grid lines in component space.
     */
    static void draw(Graphics2D g, View view) {
        double pixelSize = view.getZoomScale();
        assert pixelSize > 1;

        Rectangle visibleRect = view.getVisibleRegion();

        int canvasStartX = view.getCanvasStartX();
        int canvasStartY = view.getCanvasStartY();

        // calculate horizontal bounds in component space
        double startX = canvasStartX;
        if (visibleRect.x > canvasStartX) {
            startX += Math.floor((visibleRect.x - canvasStartX) / pixelSize) * pixelSize;
        }

        double endX = Math.min(
            visibleRect.x + visibleRect.width + pixelSize,
            canvasStartX + view.getCanvasCoWidth()
        ) - 1;

        // calculate vertical bounds in component space
        double startY = canvasStartY;
        if (visibleRect.y > canvasStartY) {
            startY += Math.floor((visibleRect.y - canvasStartY) / pixelSize) * pixelSize;
        }

        double endY = Math.min(
            visibleRect.y + visibleRect.height + pixelSize,
            canvasStartY + view.getCanvasCoHeight()
        ) - 1;

        // vertical lines
        for (double x = startX + pixelSize; x < endX; x += pixelSize) {
            g.draw(new Line2D.Double(x, startY, x, endY));
        }

        // horizontal lines
        for (double y = startY + pixelSize; y < endY; y += pixelSize) {
            g.draw(new Line2D.Double(startX, y, endX, y));
        }
    }
}
