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

import javax.swing.*;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;

import static pixelitor.gui.Desktop.Mode.FRAMES;

/**
 * The desktop area of the app, where the edited images are.
 * Currently the GUI is a JDesktopPane, but a JTabbedPane
 * could be an alternative.
 */
public class Desktop {
    enum Mode {
        FRAMES, TABS;
    }

    Mode mode = Mode.TABS;


    public static final Desktop INSTANCE = new Desktop();

    private DesktopUI ui;

    private Desktop() {
        if (mode == FRAMES) {
            ui = new FramesUI();
        } else {
            ui = new TabsUI();
        }
        JComponent component = (JComponent) ui;
        GlobalKeyboardWatch.setAlwaysVisibleComponent(component);
        GlobalKeyboardWatch.registerBrushSizeActions();
        new DropTarget(component, new DropListener());
    }

    public JComponent getUI() {
        return (JComponent) ui;
    }

    public void activateIC(ImageComponent ic) {
        ui.activateIC(ic);
    }

    public void addNewIC(ImageComponent ic) {
        ui.addNewIC(ic);
    }

    public Dimension getDesktopSize() {
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
