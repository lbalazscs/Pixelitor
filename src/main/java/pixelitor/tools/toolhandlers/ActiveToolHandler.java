/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * The last {@link ToolHandler} in the {@link ToolHandlerChain}
 * that executes the active tool's primary behavior when
 * no other handlers have intercepted the events.
 */
public class ActiveToolHandler extends ToolHandler {
    private final Tool targetTool;

    public ActiveToolHandler(Tool tool) {
        this.targetTool = tool;
    }

    @Override
    boolean mousePressed(PMouseEvent e) {
        targetTool.mousePressed(e);
        // this is the last handler in the chain, therefore it always returns true
        return true;
    }

    @Override
    boolean mouseDragged(PMouseEvent e) {
        targetTool.mouseDragged(e);
        return true;
    }

    @Override
    boolean mouseReleased(PMouseEvent e) {
        targetTool.mouseReleased(e);
        return true;
    }

    @Override
    public void setNextHandler(ToolHandler handler) {
        throw new UnsupportedOperationException(); // allow no successor
    }
}
