/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.ImageDisplay;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * The Hand Tool
 */
public class HandTool extends Tool {
    private HandToolSupport handToolSupport = new HandToolSupport();
    
    HandTool() {
        super('h', "Hand", "hand_tool_icon.gif", "drag to move the view (if scrollbars are present)",
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), false, false, false, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {
        AutoZoomButtons.addAutoZoomButtons(toolSettingsPanel);
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {
        JViewport viewport = (JViewport) ic.getParent();
        handToolSupport.mousePressed(e, viewport);
    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageDisplay ic) {
        JViewport viewport = (JViewport) ic.getParent();
        handToolSupport.mouseDragged(e, viewport);
    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {

    }

}
