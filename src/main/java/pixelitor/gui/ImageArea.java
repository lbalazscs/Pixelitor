/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.OpenImages;
import pixelitor.gui.utils.Dialogs;
import pixelitor.io.DropListener;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static javax.swing.SwingConstants.TOP;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.ImageArea.Mode.TABS;
import static pixelitor.io.DropListener.Destination.NEW_IMAGES;

/**
 * Represents the area of the app where the edited images are.
 * The GUI is either a JDesktopPane (for internal windows)
 * or a JTabbedPane (for tabs).
 */
public class ImageArea {
    public enum Mode {
        TABS("Tabs"), FRAMES("Internal Windows");

        private final String guiName;

        Mode(String guiName) {
            this.guiName = guiName;
        }

        @Override
        public String toString() {
            return guiName;
        }
    }

    private static Mode mode;
    private static final List<Consumer<Mode>> uiChangeListeners = new ArrayList<>();
    private static ImageAreaUI ui;

    // the tab placement used for creating new
    // tabbed panes and for saving the preferences
    private static int tabPlacement = TOP;

    static {
        SavedInfo savedInfo = AppPreferences.loadDesktopMode();
        mode = savedInfo.getMode();
        tabPlacement = savedInfo.getTabPlacement();

        setUI();
        setupKeysAndDnD();
    }

    private ImageArea() {
        // static utility methods, do not instantiate
    }

    private static void setUI() {
        if (mode == FRAMES) {
            ui = new FramesUI();
        } else {
            ui = new TabsUI(tabPlacement);
        }
    }

    private static void setupKeysAndDnD() {
        JComponent component = (JComponent) ui;
        new DropTarget(component, new DropListener(NEW_IMAGES));
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
        if (mode == TABS) {
            changeUI(FRAMES);
        } else {
            changeUI(TABS);
        }
    }

    public static void changeUI(Mode mode) {
        if (mode == ImageArea.mode) {
            return;
        }
        ImageArea.mode = mode;

        var pw = PixelitorWindow.get();
        pw.removeImagesArea(getUI());
        setUI();
        pw.addImagesArea();

        // this is necessary so that the size of the image area
        // is set correctly => the size of the internal frames can be set
        pw.revalidate();

        setupKeysAndDnD();
        if (mode == FRAMES) {
            // make sure that the internal frames start
            // in the top-left corner when they are re-added
            FramesUI.resetCascadeIndex();
        }
        OpenImages.forEachView(ImageArea::addNewView);

        uiChangeListeners.forEach(listener -> listener.accept(mode));
    }

    public static void addUIChangeListener(Consumer<Mode> listener) {
        uiChangeListeners.add(listener);
    }

    public static void activateView(View view) {
        ui.activateView(view);
    }

    public static void addNewView(View view) {
        ui.addNewView(view);
    }

    public static Dimension getSize() {
        return ui.getSize();
    }

    public static void cascadeWindows() {
        if (mode == FRAMES) {
            FramesUI framesUI = (FramesUI) ui;
            framesUI.cascadeWindows();
        } else {
            // the "Cascade Windows" menu should be grayed out, but for safety:
            showCascadeTileHelp();
        }
    }

    public static void tileWindows() {
        if (mode == FRAMES) {
            FramesUI framesUI = (FramesUI) ui;
            framesUI.tileWindows();
        } else {
            // the "Tile Windows" menu should be grayed out, but for safety:
            showCascadeTileHelp();
        }
    }

    private static void showCascadeTileHelp() {
        Dialogs.showInfoDialog("Help", "<html><b>\"Cascade Windows\"</b> and <b>\"Tile Windows\"</b> works only<br>" +
            "when images are displayed in internal windows, not tabs (see the Edit/Preferences dialog)");
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
            if (OpenImages.isAnyPixelGridAllowed()) {
                OpenImages.repaintAll();
            } else {
                showNoPixelGridMsg();
            }
        } else { // Tabs: check only the current view
            View view = OpenImages.getActiveView();
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
        Messages.showInfo("Pixel Grid",
            "The pixel grid consists of lines between the pixels,\n" +
                "and is shown only if the zoom is at least 1600%\n" +
                "and there is no selection.");
    }

    public static class SavedInfo {
        private final Mode mode;
        private final int tabPlacement;

        public SavedInfo(Mode mode, int tabPlacement) {
            this.mode = mode;
            this.tabPlacement = tabPlacement;
        }

        public Mode getMode() {
            return mode;
        }

        public int getTabPlacement() {
            return tabPlacement;
        }
    }
}
