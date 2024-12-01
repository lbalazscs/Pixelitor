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

import pixelitor.Views;
import pixelitor.gui.PanMethod;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Cursor;

/**
 * Enables temporary hand tool (panning) behavior for any active tool
 * when the space key is pressed at the start of a mouse drag.
 * The original tool behavior resumes if the space key is released
 * before mouse drag starts or if the drag ends (mouse released).
 */
public class HandToolHandler extends ToolHandler {
    private boolean handToolForwarding = false;
    private boolean panning = false;

    private final Cursor origCursor;
    private final Tool tool;

    public HandToolHandler(Cursor origCursor, Tool tool) {
        this.origCursor = origCursor;
        this.tool = tool;
    }

    @Override
    boolean mousePressed(PMouseEvent e) {
        if (PanMethod.ACTIVE.shouldStartPan(e)) {
            panning = true; // necessary in unit tests

            Tools.HAND.mousePressed(e);
            handToolForwarding = true;
            return true;
        }
        panning = false;
        handToolForwarding = false;

        // forwards the mouse event to the next handler
        return false;
    }

    @Override
    boolean mouseDragged(PMouseEvent e) {
        if (!handToolForwarding) {
            return false;
        }

        if (!panning) { // space was released during dragging
            // switch back to the original tool
            handToolForwarding = false;
            tool.mousePressed(e); // initialize the real tool's drag
            return false;
        } else {
            Tools.HAND.mouseDragged(e);
            return true;
        }
    }

    @Override
    boolean mouseReleased(PMouseEvent e) {
        if (handToolForwarding) {
            Tools.HAND.mouseReleased(e);
            handToolForwarding = false;

            return true;
        }

        return false;
    }

    public void spacePressed() {
        if (PanMethod.shouldIgnoreSpace()) {
            return;
        }
        if (!panning) { // this is called all the time while the space is held down
            if (handToolForwarding) {
                Views.setCursorForAll(Tools.HAND.getStartingCursor());
            }
        }
        panning = true;
    }

    public void spaceReleased() {
        if (PanMethod.shouldIgnoreSpace()) {
            return;
        }
        panning = false;
        if (!handToolForwarding) {
            Views.setCursorForAll(origCursor);
        }
    }
}
