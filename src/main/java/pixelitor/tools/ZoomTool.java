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

package pixelitor.tools;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Point;

/**
 * The Zoom Tool
 */
public class ZoomTool extends Tool {
    ZoomTool() {
        super('z', "Zoom", "zoom_tool_icon.png",
                "<b>click</b> to zoom in, <b>right-click</b> (or <b>Alt-click</b>) to zoom out.",
                Cursors.HAND, false, true, ClipStrategy.CANVAS);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        Point mousePos = e.getPoint();
        ImageComponent ic = e.getIC();
        if (e.isLeft()) {
            if(e.isAltDown()) {
                ic.decreaseZoom(mousePos);
            } else {
                ic.increaseZoom(mousePos);
            }
        } else if (e.isRight()) {
            ic.decreaseZoom(mousePos);
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        // do nothing
    }
}
