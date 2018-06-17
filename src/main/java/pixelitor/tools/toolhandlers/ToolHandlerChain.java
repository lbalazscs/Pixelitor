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

package pixelitor.tools.toolhandlers;

import pixelitor.tools.PMouseEvent;
import pixelitor.tools.Tool;

import java.awt.Cursor;

/**
 * The chain of tool handlers. Most events are sent
 * to this, and not directly to the tool.
 */
public class ToolHandlerChain {
    private ToolHandler handlerChainStart;
    private HandToolHandler handToolHandler;

    public ToolHandlerChain(Tool tool, Cursor cursor, boolean allowOnlyDrawables, boolean handToolForwarding) {
        ToolHandler lastHandler = null;
        if (handToolForwarding) {
            // most tools behave like the hand tool if the space is pressed
            handToolHandler = new HandToolHandler(cursor);
            lastHandler = addHandlerToChain(handToolHandler, lastHandler);
        }
        if (allowOnlyDrawables) {
            lastHandler = addHandlerToChain(
                    new DrawableCheckHandler(tool), lastHandler);
        }
        if (tool.doColorPickerForwarding()) {
            // brush tools behave like the color picker if Alt is pressed
            ColorPickerToolHandler colorPickerHandler = new ColorPickerToolHandler();
            lastHandler = addHandlerToChain(colorPickerHandler, lastHandler);
        }
        // if there was no special case, the current tool should handle the events
        addHandlerToChain(new CurrentToolHandler(tool), lastHandler);
    }

    /**
     * Adds the new handler to the end of the chain and returns the new end of the chain
     */
    private ToolHandler addHandlerToChain(ToolHandler newHandler, ToolHandler lastOne) {
        if (lastOne == null) {
            handlerChainStart = newHandler;
            return handlerChainStart;
        } else {
            lastOne.setSuccessor(newHandler);
            return newHandler;
        }
    }

    public void spacePressed() {
        if (handToolHandler != null) { // there is hand tool forwarding
            handToolHandler.spacePressed();
        }
    }

    public void spaceReleased() {
        if (handToolHandler != null) { // there is hand tool forwarding
            handToolHandler.spaceReleased();
        }
    }

    public void handleMousePressed(PMouseEvent e) {
        handlerChainStart.handleMousePressed(e);
    }

    public void handleMouseReleased(PMouseEvent e) {
        handlerChainStart.handleMouseReleased(e);
    }

    public void handleMouseDragged(PMouseEvent e) {
        handlerChainStart.handleMouseDragged(e);
    }
}
