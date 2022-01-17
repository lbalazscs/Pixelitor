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

package pixelitor.gui;

import pixelitor.Views;
import pixelitor.utils.AppPreferences;

import javax.swing.*;

/**
 * The configurable ways to zoom with the mouse
 */
public enum MouseZoomMethod {
    WHEEL("Mouse Wheel", "wheel") {
        @Override
        public void installOnView(View view) {
            removeExistingListeners(view);
            view.addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0) { // up, away from the user
                    view.increaseZoom(e.getPoint());
                } else {  // down, towards the user
                    view.decreaseZoom(e.getPoint());
                }
            });
        }

        @Override
        public void installOnJComponent(JComponent component, View view) {
            component.addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0) { // up, away from the user
                    // this.view will be always the active image...
                    if (view != null) { // ...and it is null if all images are closed
                        view.increaseZoom();
                    }
                } else {  // down, towards the user
                    if (view != null) {
                        view.decreaseZoom();
                    }
                }
            });
        }
    }, CTRL_WHEEL("Ctrl + Mouse Wheel", "ctrl-wheel") {
        @Override
        public void installOnView(View view) {
            removeExistingListeners(view);

            view.addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) { // up, away from the user
                        view.increaseZoom(e.getPoint());
                    } else {  // down, towards the user
                        view.decreaseZoom(e.getPoint());
                    }
                }
            });
        }

        @Override
        public void installOnJComponent(JComponent component, View view) {
            component.addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) { // up, away from the user
                        // this.view will be always the active image...
                        if (view != null) { // ...and it is null if all images are closed
                            view.increaseZoom();
                        }
                    } else {  // down, towards the user
                        if (view != null) {
                            view.decreaseZoom();
                        }
                    }
                }
            });
        }
    };

    public static MouseZoomMethod CURRENT = WHEEL;

    private final String guiName;
    private final String saveCode;

    MouseZoomMethod(String guiName, String saveCode) {
        this.guiName = guiName;
        this.saveCode = saveCode;
    }

    public abstract void installOnView(View view);

    public abstract void installOnJComponent(JComponent component, View view);

    private static void removeExistingListeners(JComponent c) {
        var existingListeners = c.getMouseWheelListeners();
        if (existingListeners.length > 0) {
            assert existingListeners.length == 1;
            c.removeMouseWheelListener(existingListeners[0]);
        }
    }

    public static void load() {
        String loadedCode = AppPreferences.loadMouseZoom();

        for (MouseZoomMethod method : values()) {
            if (method.saveCode().equals(loadedCode)) {
                CURRENT = method;
                break;
            }
        }
    }

    public static void changeTo(MouseZoomMethod newMethod) {
        if (newMethod == CURRENT) {
            return;
        }
        CURRENT = newMethod;
        Views.forEachView(newMethod::installOnView);
        Navigator.setMouseZoomMethod(newMethod);
    }

    public String saveCode() {
        return saveCode;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
