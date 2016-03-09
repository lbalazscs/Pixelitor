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

import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.tools.Tools;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * Checks whether the current tool should behave as a hand tool
 * (space is down at the start of a mouse drag).
 */
public class HandToolHandler extends ToolHandler {
    private boolean handToolForwarding = false;
    private boolean normalToolUsage = false;
    private boolean spaceDown = false;

    private final Cursor cursor;

    public HandToolHandler(Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    boolean mousePressed(MouseEvent e, ImageComponent ic) {
        if (GlobalKeyboardWatch.isSpaceDown()) {
            Tools.HAND.mousePressed(e, ic);
            handToolForwarding = true;
            return true;
        }
        normalToolUsage = true;
        handToolForwarding = false;

        // forwards the mouse event to the next handler
        return false;
    }

    @Override
    boolean mouseDragged(MouseEvent e, ImageComponent ic) {
        if (handToolForwarding) {
            Tools.HAND.mouseDragged(e, ic);
            return true;
        }

        normalToolUsage = true;

        return false;
    }

    @Override
    boolean mouseReleased(MouseEvent e, ImageComponent ic) {
        normalToolUsage = false;

        if (handToolForwarding) {
            Tools.HAND.mouseReleased(e, ic);
            handToolForwarding = false;

//            ImageComponents.setCursorForAll(cursor);
            return true;
        }

        return false;
    }

    public void spacePressed() {
        if (!spaceDown) { // this is called all the time while the space is held down, but we are interested only in ist first call
            if (!normalToolUsage) {
                ImageComponents.setCursorForAll(Tools.HAND.getCursor());
            }
        }
        spaceDown = true;
    }

    public void spaceReleased() {
        spaceDown = false;
        if (!handToolForwarding) {
            ImageComponents.setCursorForAll(cursor);
        }
    }
}
