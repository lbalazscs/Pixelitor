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
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Methods for zooming with the mouse wheel: with or without holding the Ctrl key.
 */
public enum MouseZoomMethod {
    WHEEL("Mouse Wheel", "wheel", false),
    CTRL_WHEEL("Ctrl + Mouse Wheel", "ctrl-wheel", true);

    public static MouseZoomMethod active = WHEEL;

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

    private static void viewZoomed(View view, MouseWheelEvent e, int steps) {
        Point point = e.getPoint();
        if (steps < 0) { // up, away from the user
            for (int i = 0; i < -steps; i++) {
                view.zoomIn(point);
            }
        } else {  // down, towards the user
            for (int i = 0; i < steps; i++) {
                view.zoomOut(point);
            }
        }
    }

    private static void otherZoomed(int steps) {
        View view = Views.getActive();
        if (view == null) {
            return; // all views are closed
        }
        if (steps < 0) { // up, away from the user
            for (int i = 0; i < -steps; i++) {
                view.zoomIn();
            }
        } else {  // down, towards the user
            for (int i = 0; i < steps; i++) {
                view.zoomOut();
            }
        }
    }

    /**
     * Forwards an unhandled mouse wheel event up the ancestor hierarchy to the
     * nearest container that listens for wheel events (such as an enclosing JScrollPane).
     */
    private static void forwardToParent(Component source, MouseWheelEvent e) {
        if (source == null) {
            return;
        }
        Container parent = source.getParent();
        while (parent != null && parent.getMouseWheelListeners().length == 0) {
            parent = parent.getParent();
        }
        if (parent != null) {
            parent.dispatchEvent(SwingUtilities.convertMouseEvent(source, e, parent));
            e.consume();
        }
    }

    public static void loadFromPreferences() {
        String loadedCode = AppPreferences.loadMouseZoomCode();

        for (MouseZoomMethod method : values()) {
            if (method.saveCode().equals(loadedCode)) {
                active = method;
                break;
            }
        }
    }

    public static void changeTo(MouseZoomMethod newMethod) {
        if (newMethod == active) {
            return;
        }
        active = newMethod;
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

    private static class ZoomListener implements MouseWheelListener {
        private final View view;
        private final boolean requiresCtrl;
        private double rotationAccumulator = 0.0;

        ZoomListener(View view, boolean requiresCtrl) {
            this.view = view;
            this.requiresCtrl = requiresCtrl;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (requiresCtrl && !e.isControlDown()) {
                rotationAccumulator = 0.0;
                Component source = (view != null) ? view : e.getComponent();
                forwardToParent(source, e);
                return;
            }

            double rotation = e.getPreciseWheelRotation();
            if (rotation == 0.0 && e.getWheelRotation() != 0) {
                rotation = e.getWheelRotation();
            }

            // reset partial ticks if the user reversed the scroll direction
            if ((rotationAccumulator > 0 && rotation < 0) || (rotationAccumulator < 0 && rotation > 0)) {
                rotationAccumulator = 0.0;
            }

            rotationAccumulator += rotation;
            int steps = (int) rotationAccumulator;

            if (steps != 0) {
                rotationAccumulator -= steps;
                if (view != null) {
                    viewZoomed(view, e, steps);
                } else {
                    otherZoomed(steps);
                }
            }

            e.consume();
        }
    }
}
