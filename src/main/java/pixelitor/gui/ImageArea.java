/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.io.DropListener;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.ImageArea.Mode.TABS;
import static pixelitor.io.DropListener.DropAction.OPEN_AS_NEW_IMAGES;

/**
 * Represents the area of the app where the edited images are.
 * The GUI is either a JDesktopPane (for internal windows)
 * or a JTabbedPane (for tabs).
 */
public class ImageArea {
    public enum Mode {
        TABS("Tabs") {
            @Override
            ImageAreaUI createUI() {
                return new TabsUI();
            }
        }, FRAMES("Internal Windows") {
            @Override
            ImageAreaUI createUI() {
                return new FramesUI();
            }
        };

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        abstract ImageAreaUI createUI();

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static Mode mode;
    private static final List<Consumer<Mode>> uiChangeListeners = new ArrayList<>();
    private static ImageAreaUI ui;

    private static int tabPlacement;

    static {
        ImageAreaConfig config = AppPreferences.loadDesktopMode();
        mode = config.mode();
        tabPlacement = config.tabPlacement();

        updateUI();
        setupDnD();
    }

    private ImageArea() {
        // static utility methods, do not instantiate
    }

    private static void updateUI() {
        ui = mode.createUI();
    }

    private static void setupDnD() {
        new DropTarget(getUI(), new DropListener(OPEN_AS_NEW_IMAGES));
    }

    public static JComponent getUI() {
        return (JComponent) ui;
    }

    public static Mode getMode() {
        return mode;
    }

    public static boolean currentModeIs(Mode m) {
        return getMode() == m;
    }

    public static void changeUI() {
        changeUI(mode == TABS ? FRAMES : TABS);
    }

    public static void changeUI(Mode mode) {
        if (mode == ImageArea.mode) {
            return;
        }
        ImageArea.mode = mode;

        var pw = PixelitorWindow.get();
        pw.removeImageArea(getUI());
        updateUI();
        pw.addImageArea();

        // this is necessary so that the size of the image area
        // is set correctly => the size of the internal frames can be set
        pw.revalidate();

        setupDnD();
        if (mode == FRAMES) {
            // make sure that the internal frames start
            // in the top-left corner when they are re-added
            FramesUI.resetCascadeCount();
        }
        Views.forEachView(ImageArea::addView);

        uiChangeListeners.forEach(listener -> listener.accept(mode));
    }

    public static void addUIChangeListener(Consumer<Mode> listener) {
        uiChangeListeners.add(listener);
    }

    public static void activateView(View view) {
        ui.activateView(view);
    }

    public static void addView(View view) {
        ui.addView(view);
    }

    public static Dimension getSize() {
        return ui.getSize();
    }

    public static void cascadeWindows() {
        if (mode == FRAMES) {
            ((FramesUI) ui).cascadeWindows();
        } else {
            // the "Cascade Windows" menu should be disabled
            throw new IllegalStateException("mode = " + mode);
        }
    }

    public static void tileWindows() {
        if (mode == FRAMES) {
            ((FramesUI) ui).tileWindows();
        } else {
            // the "Tile Windows" menu should be disabled
            throw new IllegalStateException("mode = " + mode);
        }
    }

    public static int getTabPlacement() {
        return tabPlacement;
    }

    public static void setTabPlacement(int tabPlacement) {
        ImageArea.tabPlacement = tabPlacement;
    }

    public static void pixelGridEnabled() {
        // the global pixel grid switch was turned on
        if (currentModeIs(FRAMES)) {
            if (Views.isAnyPixelGridAllowed()) {
                Views.repaintAll();
            } else {
                showNoPixelGridMsg();
            }
        } else { // Tabs: check only the current view
            View view = Views.getActive();
            if (view != null) {
                if (view.allowPixelGrid()) {
                    view.repaint();
                } else {
                    showNoPixelGridMsg();
                }
            }
        }
    }

    private static void showNoPixelGridMsg() {
        String msg = """
            The pixel grid consists of lines between the pixels,
            and is shown only if the zoom is at least 1600%.""";
        Messages.showInfo("Pixel Grid", msg, (Component) ui);
    }
}
