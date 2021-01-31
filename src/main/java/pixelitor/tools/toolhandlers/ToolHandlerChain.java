/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Cursor;

/**
 * The chain of {@link ToolHandler}s, following the "Chain of Responsibility"
 * design pattern. The last handler is always the current tool.
 *
 * Most mouse events are sent to this, and not directly to the tool.
 */
public class ToolHandlerChain {
    private ToolHandler firstHandler;
    private ToolHandler lastHandler;

    private HandToolHandler handToolHandler;

    public ToolHandlerChain(Tool tool, Cursor cursor) {
        lastHandler = null;
        if (tool.hasHandToolForwarding()) {
            handToolHandler = new HandToolHandler(cursor, tool);
            lastHandler = appendNext(handToolHandler);
        }

        if (tool.allowOnlyDrawables()) {
            lastHandler = appendNext(new DrawableCheckHandler(tool));
        }

        if (tool.hasColorPickerForwarding()) {
            // brush tools behave like the color picker if Alt is pressed
            lastHandler = appendNext(new ColorPickerToolHandler());
        }

        // if there was no special case, the current tool should handle the events
        lastHandler = appendNext(new CurrentToolHandler(tool));
    }

    /**
     * Appends the new handler to the end of the chain and also returns it
     */
    private ToolHandler appendNext(ToolHandler newHandler) {
        if (lastHandler == null) {
            firstHandler = newHandler;
        } else {
            lastHandler.setSuccessor(newHandler);
        }
        return newHandler;
    }

    public void spacePressed() {
        if (hasHandToolForwarding()) {
            handToolHandler.spacePressed();
        }
    }

    public void spaceReleased() {
        if (hasHandToolForwarding()) {
            handToolHandler.spaceReleased();
        }
    }

    private boolean hasHandToolForwarding() {
        return handToolHandler != null;
    }

    public void handleMousePressed(PMouseEvent e) {
        firstHandler.handleMousePressed(e);
    }

    public void handleMouseReleased(PMouseEvent e) {
        firstHandler.handleMouseReleased(e);
    }

    public void handleMouseDragged(PMouseEvent e) {
        firstHandler.handleMouseDragged(e);
    }
}
