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

import pixelitor.ImageComponent;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * The Zoom Tool
 */
public class ZoomTool extends Tool {
    ZoomTool() {
        super('z', "Zoom", "zoom_tool_icon.png", "click to zoom in, right-click (or Alt-click) to zoom out",
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), false, true, false, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        int x = e.getX();
        int y = e.getY();

        if(SwingUtilities.isLeftMouseButton(e)) {
            if(e.isAltDown()) {
                ic.decreaseZoom(x, y);
            } else {
                ic.increaseZoom(x, y);
            }
        } else if(SwingUtilities.isRightMouseButton(e)) {
            ic.decreaseZoom(x, y);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {

    }

}
