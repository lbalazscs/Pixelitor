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

import pixelitor.OpenImages;
import pixelitor.gui.PanMethod;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Cursor;

/**
 * Checks whether the current tool should behave as a hand tool
 * (space is down at the start of a mouse drag).
 */
public class HandToolHandler extends ToolHandler {
    private boolean handToolForwarding = false;
    private boolean panning = false;

    private final Cursor cursor;
    private final Tool tool;

    public HandToolHandler(Cursor cursor, Tool tool) {
        this.cursor = cursor;
        this.tool = tool;
    }

    @Override
    boolean mousePressed(PMouseEvent e) {
        if (PanMethod.CURRENT.initPan(e)) {
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
        if (handToolForwarding) {
            if (!panning) { // space was released in the meantime
                handToolForwarding = false;
                tool.mousePressed(e); // initialize the real tool's drag
                return false;
            } else {
                Tools.HAND.mouseDragged(e);
            }
            return true;
        }

        return false;
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
        if (PanMethod.ignoreSpace()) {
            return;
        }
        if (!panning) { // this is called all the time while the space is held down
            if (handToolForwarding) {
                OpenImages.setCursorForAll(Tools.HAND.getStartingCursor());
            }
        }
        panning = true;
    }

    public void spaceReleased() {
        if (PanMethod.ignoreSpace()) {
            return;
        }
        panning = false;
        if (!handToolForwarding) {
            OpenImages.setCursorForAll(cursor);
        }
    }
}
