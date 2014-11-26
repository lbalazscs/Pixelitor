/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.toolhandlers;

import pixelitor.ImageComponent;
import pixelitor.tools.Tool;

import java.awt.event.MouseEvent;

/**
 * At the end of a ToolEventHandler chain there is always a ToolHandler, which does
 * the real job of the tool
 */
public class ToolHandler extends ToolEventHandler {
    Tool tool;

    public ToolHandler(Tool tool) {
        this.tool = tool;
    }

    @Override
    boolean mousePressed(MouseEvent e, ImageComponent ic) {
        tool.toolMousePressed(e, ic);
        return true;
    }

    @Override
    boolean mouseDragged(MouseEvent e, ImageComponent ic) {
        tool.toolMouseDragged(e, ic);
        return true;
    }

    @Override
    boolean mouseReleased(MouseEvent e, ImageComponent ic) {
        tool.toolMouseReleased(e, ic);
        return true;
    }
}
