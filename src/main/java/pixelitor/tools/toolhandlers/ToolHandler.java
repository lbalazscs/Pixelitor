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

import java.awt.event.MouseEvent;

/**
 * Can be used to handle the mouse events instead of the current tool.
 *
 * The tool event handler objects follow the "Chain of responsibility" design pattern.
 * The last event handler is always the current tool.
 * This is the abstract superclass of all the chained handlers.
 */
public abstract class ToolHandler {
    private ToolHandler successor;

    public void setSuccessor(ToolHandler handler) {
        successor = handler;
    }

    public void handleMousePressed(MouseEvent e, ImageComponent ic) {
        if (mousePressed(e, ic)) {
            return;
        }
        // forwards the mouse event to the next handler
        successor.handleMousePressed(e, ic);
    }

    /**
     * @return true if the event was handled and it should no be forwarded to the next handler
     */
    abstract boolean mousePressed(MouseEvent e, ImageComponent ic);

    public void handleMouseDragged(MouseEvent e, ImageComponent ic) {
        if (mouseDragged(e, ic)) {
            return;
        }
        successor.handleMouseDragged(e, ic);
    }

    abstract boolean mouseDragged(MouseEvent e, ImageComponent ic);

    public void handleMouseReleased(MouseEvent e, ImageComponent ic) {
        if (mouseReleased(e, ic)) {
            return;
        }
        successor.handleMouseReleased(e, ic);
    }

    abstract boolean mouseReleased(MouseEvent e, ImageComponent ic);

}
