/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import static pixelitor.gui.utils.Screens.Align.FRAME_RIGHT;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;

public class Screens {
    private static final GraphicsEnvironment LOCAL_GRAPHICS = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private static final GraphicsDevice[] SCREEN_DEVICES = LOCAL_GRAPHICS.getScreenDevices();
    private static final boolean MULTI_MONITORS = SCREEN_DEVICES.length > 1;

    public enum Align {SCREEN_CENTER, FRAME_RIGHT}

    private Screens() {
    }

    /**
     * Positions the given window in the screen in which Pixelitor is running.
     */
    public static void position(Window window, Align align, Point loc) {
        PixelitorWindow pw = PixelitorWindow.get();
        Rectangle screenBounds;
        if (MULTI_MONITORS) {
            screenBounds = pw.getGraphicsConfiguration().getBounds();
        } else {
            // takes the taskbar into account
            screenBounds = LOCAL_GRAPHICS.getMaximumWindowBounds();
        }
        Rectangle windowBounds = window.getBounds();

        if (windowBounds.height > screenBounds.height) {
            windowBounds.height = screenBounds.height;
        }

        if (windowBounds.width > screenBounds.width) {
            windowBounds.width = screenBounds.width;
        }

        // if it was bigger than the screen, restrict it to screen size
        window.setSize(windowBounds.width, windowBounds.height);

        if (loc != null) {
            // the presence of a non-null loc argument indicates
            // that the location of the window was already set
            return;
        }
        // position it
        if (align == SCREEN_CENTER) {
            int locX = screenBounds.x + (screenBounds.width - windowBounds.width) / 2;
            int locY = screenBounds.y + (screenBounds.height - windowBounds.height) / 2;
            window.setLocation(locX, locY);
        } else if (align == FRAME_RIGHT) {
            Point pwLoc = pw.getLocationOnScreen();
            int locX = pwLoc.x + pw.getWidth() - windowBounds.width;
            int locY;
            if (windowBounds.height < pw.getHeight()) {
                // if it is less tall than the main window, then position relative to it
                locY = pwLoc.y + (pw.getHeight() - windowBounds.height) / 2;
            } else {
                // else position it relative to the screen
                locY = screenBounds.y + (screenBounds.height - windowBounds.height) / 2;
            }

            // if there is place between the canvas and the window,
            // then place the window at half distance
            View view = Views.getActive();
            if (view != null && view.isShowing()) {
                Rectangle canvasBounds = view.getVisibleCanvasBoundsOnScreen();
                int canvasMaxX = canvasBounds.x + canvasBounds.width;
                if (canvasMaxX < locX) {
                    int dist = locX - canvasMaxX;
                    locX -= dist / 2;
                }
            }

            window.setLocation(locX, locY);
        } else {
            throw new IllegalStateException();
        }
    }

    public static Dimension getMaxWindowSize() {
        Rectangle bounds;
        try {
            // use this because it takes into account areas
            // like the taskbar, but it can throw
            // a "Window must not be zero" if there are 3 monitors
            // on Linux with some newer Java versions, see
            // https://github.com/lbalazscs/Pixelitor/issues/15
            bounds = LOCAL_GRAPHICS.getMaximumWindowBounds();
        } catch (Exception e) {
            return Toolkit.getDefaultToolkit().getScreenSize();
        }

        return new Dimension(bounds.width, bounds.height);
    }

    public static boolean hasMultipleMonitors() {
        return MULTI_MONITORS;
    }
}
