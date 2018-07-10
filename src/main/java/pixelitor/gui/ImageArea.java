/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.io.DropListener;
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;

import static pixelitor.gui.ImageArea.Mode.FRAMES;

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

        public static Mode fromString(String value) {
            if (value.equals(TABS.toString())) {
                return TABS;
            } else {
                return FRAMES;
            }
        }

        @Override
        public String toString() {
            return guiName;
        }
    }

    private Mode mode;

    public static final ImageArea INSTANCE = new ImageArea();

    private ImageAreaUI ui;

    private ImageArea() {
        mode = AppPreferences.loadDesktopMode();

        setUI();
        setupKeysAndDnD();
    }

    private void setUI() {
        if (mode == FRAMES) {
            ui = new FramesUI();
        } else {
            ui = new TabsUI();
        }
    }

    private void setupKeysAndDnD() {
        JComponent component = (JComponent) ui;
        GlobalKeyboardWatch.setAlwaysVisibleComponent(component);
        GlobalKeyboardWatch.registerKeysOnAlwaysVisibleComponent();
        new DropTarget(component, new DropListener());
    }

    public JComponent getUI() {
        return (JComponent) ui;
    }

    public Mode getMode() {
        return mode;
    }

    public void changeUI(Mode mode) {
        if (mode == this.mode) {
            return;
        }
        this.mode = mode;

        PixelitorWindow pw = PixelitorWindow.getInstance();
        pw.removeImagesArea(getUI());
        setUI();
        pw.addImagesArea();

        // this is necessary so that the size of the image area
        // is set correctly => the size of the internal frames can be set
        pw.revalidate();

        setupKeysAndDnD();
        if (mode == FRAMES) {
            // make sure they start in the top-left
            // corner when they are re-added
            FramesUI.cascadeIndex = 0;
        }
        ImageComponents.forAllImages(this::addNewIC);
    }

    public void activateIC(ImageComponent ic) {
        ui.activateIC(ic);
    }

    public void addNewIC(ImageComponent ic) {
        ui.addNewIC(ic);
    }

    public Dimension getSize() {
        return ui.getSize();
    }

    public void cascadeWindows() {
        if (mode == FRAMES) {
            FramesUI framesUI = (FramesUI) ui;
            framesUI.cascadeWindows();
        } else {
            // TODO
        }
    }

    public void tileWindows() {
        if (mode == FRAMES) {
            FramesUI framesUI = (FramesUI) ui;
            framesUI.tileWindows();
        } else {
            // TODO
        }
    }
}
