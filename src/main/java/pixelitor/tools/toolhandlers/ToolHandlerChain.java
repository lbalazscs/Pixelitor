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

import java.awt.Cursor;

/**
 * Implements the Chain of Responsibility pattern to manage tool behavior
 * modifications. The chain processes mouse events through a series of
 * {@link ToolHandler}s before reaching the tool itself. Each handler
 * can either handle the event and stop propagation or pass the event
 * to the next handler in the chain.
 * <p>
 * Most mouse events are sent to this, and not directly to the tool.
 */
public class ToolHandlerChain {
    private ToolHandler firstHandler;
    private ToolHandler lastHandler;

    private HandToolHandler handToolHandler;

    public ToolHandlerChain(Tool tool, Cursor cursor) {
        lastHandler = null;

        if (tool.hasHandToolForwarding()) {
            // enables temporary pan behavior
            handToolHandler = new HandToolHandler(cursor, tool);
            lastHandler = appendNext(handToolHandler);
        }

        if (tool.allowOnlyDrawables()) {
            // ensures proper layer type for the tool
            lastHandler = appendNext(new DrawableCheckHandler(tool));
        }

        if (tool.hasColorPickerForwarding()) {
            // brush tools behave like the color picker if Alt is pressed
            lastHandler = appendNext(new ColorPickerToolHandler());
        }

        // The last handler executes the tool's primary behavior
        // if the event was not intercepted before.
        lastHandler = appendNext(new ActiveToolHandler(tool));
    }

    /**
     * Appends the new handler to the end of the chain and also returns it
     */
    private ToolHandler appendNext(ToolHandler newHandler) {
        if (lastHandler == null) {
            firstHandler = newHandler;
        } else {
            lastHandler.setNextHandler(newHandler);
        }
        return newHandler;
    }

    /**
     * Activates temporary hand tool behavior if this chain
     * includes a hand tool handler.
     */
    public void spacePressed() {
        if (handToolHandler != null) {
            handToolHandler.spacePressed();
        }
    }

    public void spaceReleased() {
        if (handToolHandler != null) {
            handToolHandler.spaceReleased();
        }
    }

    /**
     * Processes a mouse pressed event through the handler chain.
     */
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
