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

package pixelitor.tools.toolhandlers;

import pixelitor.tools.Tool;
import pixelitor.tools.util.PMouseEvent;

/**
 * Forwards the events to the current tool in order to do the real job of the tool.
 * This is always the last {@link ToolHandler} in the {@link ToolHandlerChain}.
 */
public class CurrentToolHandler extends ToolHandler {
    private final Tool tool;

    public CurrentToolHandler(Tool tool) {
        this.tool = tool;
    }

    @Override
    boolean mousePressed(PMouseEvent e) {
        tool.mousePressed(e);
        // this is the last handler in the chain, therefore it always returns true
        return true;
    }

    @Override
    boolean mouseDragged(PMouseEvent e) {
        tool.mouseDragged(e);
        return true;
    }

    @Override
    boolean mouseReleased(PMouseEvent e) {
        tool.mouseReleased(e);
        return true;
    }
}
