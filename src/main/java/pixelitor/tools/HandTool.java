/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import pixelitor.gui.ImageComponent;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * The Hand Tool
 */
public class HandTool extends Tool {
    private final HandToolSupport handToolSupport = new HandToolSupport();
    
    HandTool() {
        super('h', "Hand", "hand_tool_icon.png", "drag to move the view (if scrollbars are present)",
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), false, false, false, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        JViewport viewport = (JViewport) ic.getParent();
        handToolSupport.mousePressed(e, viewport);
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        JViewport viewport = (JViewport) ic.getParent();
        handToolSupport.mouseDragged(e, viewport);
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {

    }

}
