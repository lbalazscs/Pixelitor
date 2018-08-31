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

import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

/**
 * The Hand Tool
 */
public class HandTool extends Tool {
    private final HandToolSupport handToolSupport = new HandToolSupport();
    
    HandTool() {
        super("Hand", 'h', "hand_tool_icon.png",
                "<b>drag</b> to move the view (if scrollbars are present).",
                Cursors.HAND, false,
                false, ClipStrategy.CANVAS);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        handToolSupport.mousePressed(e.getOrigEvent(), e.getViewport());
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        handToolSupport.mouseDragged(e.getOrigEvent(), e.getViewport());
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
    }
}
