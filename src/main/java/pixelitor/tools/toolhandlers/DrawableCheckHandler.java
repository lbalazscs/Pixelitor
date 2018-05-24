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

import pixelitor.gui.ImageComponent;
import pixelitor.layers.Drawable;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.Tool;

import java.awt.event.MouseEvent;

/**
 * Checks whether the active edited object is
 * a Drawable (image layer or mask)
 */
public class DrawableCheckHandler extends ToolHandler {
    private final Tool currentTool;

    public DrawableCheckHandler(Tool currentTool) {
        this.currentTool = currentTool;
    }

    @Override
    boolean mousePressed(MouseEvent e, ImageComponent ic) {
        if (ic.activeIsDrawable()) {
            // forwards the mouse event to the next handler
            return false;
        }

        DrawableAction action = new DrawableAction(currentTool.getName() + " Tool") {
            @Override
            protected void process(Drawable dr) {
                // do nothing
            }
        };
        // as the action doesn't do anything, the only effects
        // here are the dialogs and auto-rasterization
        action.actionPerformed(null);

        // whatever happened, do not forward this event,
        // the user should click again in order to use the tool
        return true;
    }

    @Override
    boolean mouseDragged(MouseEvent e, ImageComponent ic) {
        return !ic.activeIsDrawable();
    }

    @Override
    boolean mouseReleased(MouseEvent e, ImageComponent ic) {
        return !ic.activeIsDrawable();
    }
}
