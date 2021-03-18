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

import pixelitor.layers.Drawable;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.Tool;
import pixelitor.tools.util.PMouseEvent;

/**
 * Checks whether the active edited object is
 * a Drawable (image layer or mask)
 */
public class DrawableCheckHandler extends ToolHandler {
    private final DrawableAction drawableAction;

    public DrawableCheckHandler(Tool currentTool) {
        drawableAction = new DrawableAction(currentTool.getName() + " Tool") {
            @Override
            protected void process(Drawable dr) {
                // do nothing
            }
        };
    }

    @Override
    boolean mousePressed(PMouseEvent e) {
        if (e.getComp().activeIsDrawable()) {
            // forwards the mouse event to the next handler
            return false;
        }

        // as the action doesn't do anything, the only effects
        // here are the dialogs and auto-rasterization
        drawableAction.actionPerformed(null);

        // whatever happened, do not forward this event,
        // the user should click again in order to use the tool
        return true;
    }

    @Override
    boolean mouseDragged(PMouseEvent e) {
        return !e.getComp().activeIsDrawable();
    }

    @Override
    boolean mouseReleased(PMouseEvent e) {
        return !e.getComp().activeIsDrawable();
    }
}
