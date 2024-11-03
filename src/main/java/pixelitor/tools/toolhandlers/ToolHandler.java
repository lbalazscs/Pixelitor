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

import pixelitor.tools.util.PMouseEvent;

/**
 * The abstract base class of all chained handlers in a {@link ToolHandlerChain}.
 * Each {@link ToolHandler} can intercept and modify tool behavior
 * by processing mouse events before they reach the actual tool.
 */
public abstract class ToolHandler {
    private ToolHandler nextHandler;

    public void setNextHandler(ToolHandler handler) {
        nextHandler = handler;
    }

    /**
     * Processes a mouse pressed event or forwards it to the next handler.
     */
    public void handleMousePressed(PMouseEvent e) {
        if (mousePressed(e)) {
            return;
        }

        // forwards the mouse event to the next handler
        nextHandler.handleMousePressed(e);
    }

    /**
     * Processes a mouse dragged event or forwards it to the next handler.
     */
    public void handleMouseDragged(PMouseEvent e) {
        if (mouseDragged(e)) {
            return;
        }
        nextHandler.handleMouseDragged(e);
    }

    /**
     * Processes a mouse released event or forwards it to the next handler.
     */
    public void handleMouseReleased(PMouseEvent e) {
        if (mouseReleased(e)) {
            return;
        }
        nextHandler.handleMouseReleased(e);
    }

    /**
     * Handles a mouse pressed event and returns true if the event
     * was handled, and it should not be forwarded to the next handler.
     */
    abstract boolean mousePressed(PMouseEvent e);

    /**
     * Handles a mouse dragged event and returns true if the event
     * was handled, and it should not be forwarded to the next handler.
     */
    abstract boolean mouseDragged(PMouseEvent e);

    /**
     * Handles a mouse released event and returns true if the event
     * was handled, and it should not be forwarded to the next handler.
     */
    abstract boolean mouseReleased(PMouseEvent e);
}
