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
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Methods for zooming with the mouse wheel: with or without holding the Ctrl key.
 */
public enum MouseZoomMethod {
    WHEEL("Mouse Wheel", "wheel", false),
    CTRL_WHEEL("Ctrl + Mouse Wheel", "ctrl-wheel", true);

    public static MouseZoomMethod ACTIVE = WHEEL;

    private final String displayName;
    private final String saveCode;
    private final boolean requiresCtrl;

    MouseZoomMethod(String displayName, String saveCode, boolean requiresCtrl) {
        this.displayName = displayName;
        this.saveCode = saveCode;
        this.requiresCtrl = requiresCtrl;
    }

    public void installOnView(View view) {
        removeExistingListeners(view);
        view.addMouseWheelListener(new ZoomListener(view, requiresCtrl));
    }

    // used on other components, like the Navigator, where the
    // exact mouse position doesn't matter
    public void installOnOther(JComponent component) {
        removeExistingListeners(component);
        component.addMouseWheelListener(new ZoomListener(null, requiresCtrl));
    }

    private static void removeExistingListeners(JComponent c) {
        for (MouseWheelListener listener : c.getMouseWheelListeners()) {
            if (listener instanceof ZoomListener) {
                c.removeMouseWheelListener(listener);
            }
        }
    }

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

    private record ZoomListener(View view, boolean requiresCtrl) implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (requiresCtrl && !e.isControlDown()) {
                return;
            }
            if (view != null) {
                viewZoomed(view, e);
            } else {
                otherZoomed(e);
            }
        }
    }
}
