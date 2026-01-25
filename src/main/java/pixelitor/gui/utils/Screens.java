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

package pixelitor.gui.utils;

import pixelitor.Views;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;

import java.awt.*;
import java.util.Objects;

public class Screens {
    private static final GraphicsEnvironment GRAPHICS_ENV = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private static final GraphicsDevice[] SCREEN_DEVICES = GRAPHICS_ENV.getScreenDevices();
    private static final boolean IS_MULTI_MONITOR = SCREEN_DEVICES.length > 1;

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
     * Positions a window on the screen based on the given alignment.
     */
    public static void positionWindow(Window window, Align align) {
        Rectangle screenBounds = getScreenBounds();

        restrictWindowSize(window, screenBounds);

        // calculate window position based on alignment
        switch (align) {
            case SCREEN_CENTER -> centerWindowOnScreen(window, screenBounds);
            case FRAME_RIGHT -> alignWindowToFrameRight(window, screenBounds);
            default -> throw new IllegalStateException();
        }
    }

    /**
     * Positions a window on the screen based on the given custom location.
     */
    public static void positionWindow(Window window, Point location) {
        restrictWindowSize(window, getScreenBounds());
        window.setLocation(Objects.requireNonNull(location));
    }

    private static Rectangle getScreenBounds() {
        // takes the taskbar into account
        return IS_MULTI_MONITOR
            ? PixelitorWindow.get().getGraphicsConfiguration().getBounds()
            : GRAPHICS_ENV.getMaximumWindowBounds();
    }

    private static void restrictWindowSize(Window window, Rectangle screenBounds) {
        Rectangle windowBounds = window.getBounds();

        // adjust window size if larger than the screen
        windowBounds.height = Math.min(windowBounds.height, screenBounds.height);
        windowBounds.width = Math.min(windowBounds.width, screenBounds.width);
        window.setSize(windowBounds.width, windowBounds.height);
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

        window.setLocation(locX, locY);
    }

    public static Dimension getMaxWindowSize() {
        Rectangle bounds;
        try {
            // use this because it takes into account areas
            // like the taskbar, but it can throw
            // a "Window must not be zero" if there are 3 monitors
            // on Linux with some newer Java versions, see
            // https://github.com/lbalazscs/Pixelitor/issues/15
            bounds = GRAPHICS_ENV.getMaximumWindowBounds();
        } catch (Exception e) {
            return Toolkit.getDefaultToolkit().getScreenSize(); // fallback
        }

        return new Dimension(bounds.width, bounds.height);
    }

    public static boolean isMultiMonitorSetup() {
        return IS_MULTI_MONITOR;
    }
}
