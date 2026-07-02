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

package pixelitor.gui.utils;

import pixelitor.Views;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;

import java.awt.*;
import java.util.Objects;

public class Screens {
    private static final GraphicsEnvironment GRAPHICS_ENV = GraphicsEnvironment.getLocalGraphicsEnvironment();

    /**
     * Alignment strategies for positioning windows.
     */
    public enum Align {
        SCREEN_CENTER, // position the window at the center of the current screen
        FRAME_RIGHT // align the window to the right side of the main app frame
    }

    private Screens() {
        // prevent instantiation
    }

    /**
     * Positions a window or dialog on the screen based on the given alignment.
     */
    public static void positionWindow(Window window, Align align) {
        Rectangle screenBounds = getScreenBounds();

        restrictWindowSize(window, screenBounds);

        // calculate window position based on alignment
        switch (align) {
            case SCREEN_CENTER -> centerWindowOnScreen(window, screenBounds);
            case FRAME_RIGHT -> alignWindowToFrameRight(window, screenBounds);
        }
    }

    /**
     * Positions a window on the screen based on the given custom location.
     */
    public static void positionWindow(Window window, Point location) {
        Objects.requireNonNull(location);

        Rectangle requestedBounds = new Rectangle(location, window.getSize());
        GraphicsConfiguration targetGc = null;
        int maxIntersectionArea = 0;

        // find if the restored window bounds intersect with any currently active screen
        for (GraphicsDevice screen : GRAPHICS_ENV.getScreenDevices()) {
            // all configurations should have the same size, so use the default one
            GraphicsConfiguration gc = screen.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Rectangle intersection = screenBounds.intersection(requestedBounds);

            // if it intersects, track the screen with the largest overlap area
            if (intersection.width > 0 && intersection.height > 0) {
                int area = intersection.width * intersection.height;
                if (area > maxIntersectionArea) {
                    maxIntersectionArea = area;
                    targetGc = gc;
                }
            }
        }

        if (targetGc != null) { // the location belongs to a currently active monitor
            Rectangle usableBounds = getUsableScreenBounds(targetGc);

            restrictWindowSize(window, usableBounds);
            Dimension windowSize = window.getSize();

            // clamp coordinates inside the monitor where it was found
            int locX = Math.clamp(location.x, usableBounds.x, usableBounds.x + usableBounds.width - windowSize.width);
            int locY = Math.clamp(location.y, usableBounds.y, usableBounds.y + usableBounds.height - windowSize.height);

            window.setLocation(locX, locY);
        } else {
            // the location is completely off-screen (e.g., monitor was disconnected),
            // so fall back to safely centering on the main window's current screen
            positionWindow(window, Align.SCREEN_CENTER);
        }
    }

    /**
     * Calculates the true usable area (subtracting taskbars, docks, etc.)
     * for a specific GraphicsConfiguration.
     */
    private static Rectangle getUsableScreenBounds(GraphicsConfiguration gc) {
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        return new Rectangle(
            bounds.x + insets.left,
            bounds.y + insets.top,
            bounds.width - insets.left - insets.right,
            bounds.height - insets.top - insets.bottom
        );
    }

    // must be called after the constructor of the main window completes
    private static Rectangle getScreenBounds() {
        GraphicsConfiguration gc = PixelitorWindow.get().getGraphicsConfiguration();
        return getUsableScreenBounds(gc);
    }

    private static void restrictWindowSize(Window window, Rectangle screenBounds) {
        Rectangle windowBounds = window.getBounds();

        // adjust window size if larger than the screen
        int width = Math.min(windowBounds.width, screenBounds.width);
        int height = Math.min(windowBounds.height, screenBounds.height);
        window.setSize(width, height);
    }

    /**
     * Centers the window on the screen.
     */
    private static void centerWindowOnScreen(Window window, Rectangle screenBounds) {
        Dimension windowSize = window.getSize();
        int locX = screenBounds.x + (screenBounds.width - windowSize.width) / 2;
        int locY = screenBounds.y + (screenBounds.height - windowSize.height) / 2;
        window.setLocation(locX, locY);
    }

    /**
     * Aligns the window to the right side of the main application frame.
     */
    private static void alignWindowToFrameRight(Window window, Rectangle screenBounds) {
        PixelitorWindow mainWindow = PixelitorWindow.get();
        Point pwLoc = mainWindow.getLocationOnScreen();
        Dimension windowSize = window.getSize();

        // the horizontal position is aligned to the main window's right edge
        int locX = pwLoc.x + mainWindow.getWidth() - windowSize.width;

        // calculates vertical alignment for the window
        // based on its size and the main frame's height
        int locY;
        if (windowSize.height < mainWindow.getHeight()) {
            // align relative to the main window
            locY = pwLoc.y + (mainWindow.getHeight() - windowSize.height) / 2;
        } else {
            // center on screen
            locY = screenBounds.y + (screenBounds.height - windowSize.height) / 2;
        }

        // if there is a horizontal gap between the canvas and
        // the window, then put the window at half distance
        View view = Views.getActive();
        if (view != null && view.isShowing()) {
            Rectangle canvasBounds = view.getVisibleCanvasBoundsOnScreen();
            int canvasRightEdge = canvasBounds.x + canvasBounds.width;
            if (canvasRightEdge < locX) {
                locX -= (locX - canvasRightEdge) / 2;
            }
        }

        // clamp the coordinates to stay within screen boundaries
        locX = Math.clamp(locX, screenBounds.x, screenBounds.x + screenBounds.width - windowSize.width);
        locY = Math.clamp(locY, screenBounds.y, screenBounds.y + screenBounds.height - windowSize.height);

        window.setLocation(locX, locY);
    }

    public static boolean isMultiMonitorSetup() {
        return GRAPHICS_ENV.getScreenDevices().length > 1;
    }
}
