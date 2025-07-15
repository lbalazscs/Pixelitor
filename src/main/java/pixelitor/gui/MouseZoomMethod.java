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

package pixelitor.gui;

import pixelitor.Views;
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;

/**
 * Methods for zooming with the mouse wheel: with or without holding the Ctrl key.
 */
public enum MouseZoomMethod {
    WHEEL("Mouse Wheel", "wheel") {
        @Override
        public void installOnView(View view) {
            removeExistingListeners(view);
            view.addMouseWheelListener(e -> viewZoomed(view, e));
        }

        @Override
        public void installOnOther(JComponent component) {
            component.addMouseWheelListener(MouseZoomMethod::otherZoomed);
        }
    }, CTRL_WHEEL("Ctrl + Mouse Wheel", "ctrl-wheel") {
        @Override
        public void installOnView(View view) {
            removeExistingListeners(view);

            view.addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    viewZoomed(view, e);
                }
            });
        }

        @Override
        public void installOnOther(JComponent component) {
            component.addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    otherZoomed(e);
                }
            });
        }
    };

    public static MouseZoomMethod ACTIVE = WHEEL;

    private final String displayName;
    private final String saveCode;

    MouseZoomMethod(String displayName, String saveCode) {
        this.displayName = displayName;
        this.saveCode = saveCode;
    }

    public abstract void installOnView(View view);

    // used on other components, like the Navigator, where the
    // exact mouse position doesn't matter
    public abstract void installOnOther(JComponent component);

    private static void viewZoomed(View view, MouseWheelEvent e) {
        if (e.getWheelRotation() < 0) { // up, away from the user
            view.zoomIn(e.getPoint());
        } else {  // down, towards the user
            view.zoomOut(e.getPoint());
        }
    }

    private static void otherZoomed(MouseWheelEvent e) {
        View view = Views.getActive();
        if (view == null) {
            return; // all views are closed
        }
        if (e.getWheelRotation() < 0) { // up, away from the user
            view.zoomIn();
        } else {  // down, towards the user
            view.zoomOut();
        }
    }

    private static void removeExistingListeners(JComponent c) {
        var existingListeners = c.getMouseWheelListeners();
        if (existingListeners.length > 0) {
            assert existingListeners.length == 1;
            c.removeMouseWheelListener(existingListeners[0]);
        }
    }

    public static void loadFromPreferences() {
        String loadedCode = AppPreferences.loadMouseZoom();

        for (MouseZoomMethod method : values()) {
            if (method.saveCode().equals(loadedCode)) {
                ACTIVE = method;
                break;
            }
        }
    }

    public static void changeTo(MouseZoomMethod newMethod) {
        if (newMethod == ACTIVE) {
            return;
        }
        ACTIVE = newMethod;
        Views.forEach(newMethod::installOnView);
        Navigator.setMouseZoomMethod(newMethod);
    }

    public String saveCode() {
        return saveCode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
