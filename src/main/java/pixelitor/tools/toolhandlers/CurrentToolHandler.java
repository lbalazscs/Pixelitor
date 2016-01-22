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

package pixelitor.tools.toolhandlers;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.Tool;

import java.awt.event.MouseEvent;

/**
 * At the end of a AbstractToolHandler chain there is always a CurrentToolHandler,
 * which forwards the events to the current tool in order to do the real job of the tool
 */
public class CurrentToolHandler extends ToolHandler {
    private final Tool tool;

    public CurrentToolHandler(Tool tool) {
        this.tool = tool;
    }

    @Override
    boolean mousePressed(MouseEvent e, ImageComponent ic) {
        tool.mousePressed(e, ic);
        // this is the last handler in the chain, therefore it always returns true
        return true;
    }

    @Override
    boolean mouseDragged(MouseEvent e, ImageComponent ic) {
        tool.mouseDragged(e, ic);
        return true;
    }

    @Override
    boolean mouseReleased(MouseEvent e, ImageComponent ic) {
        tool.mouseReleased(e, ic);
        return true;
    }
}
