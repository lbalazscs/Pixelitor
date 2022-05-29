/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * The abstract superclass of all chained handlers in a {@link ToolHandlerChain}.
 * Each tool handler can handle the mouse events instead of the selected tool.
 */
public abstract class ToolHandler {
    private ToolHandler successor;

    public void setSuccessor(ToolHandler handler) {
        successor = handler;
    }

    public void handleMousePressed(PMouseEvent e) {
        if (mousePressed(e)) {
            return;
        }

        // forwards the mouse event to the next handler
        successor.handleMousePressed(e);
    }

    /**
     * Returns true if the event was handled, and it should
     * not be forwarded to the next handler
     */
    abstract boolean mousePressed(PMouseEvent e);

    public void handleMouseDragged(PMouseEvent e) {
        if (mouseDragged(e)) {
            return;
        }
        successor.handleMouseDragged(e);
    }

    /**
     * Returns true if the event was handled, and it should
     * not be forwarded to the next handler
     */
    abstract boolean mouseDragged(PMouseEvent e);

    public void handleMouseReleased(PMouseEvent e) {
        if (mouseReleased(e)) {
            return;
        }
        successor.handleMouseReleased(e);
    }

    /**
     * Returns true if the event was handled, and it should
     * not be forwarded to the next handler
     */
    abstract boolean mouseReleased(PMouseEvent e);
}
