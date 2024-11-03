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

import pixelitor.Composition;
import pixelitor.layers.Drawable;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.Tool;
import pixelitor.tools.util.PMouseEvent;

/**
 * Ensures that tools requiring a {@link Drawable} layer (like brushes)
 * are only used on appropriate layers.
 */
public class DrawableCheckHandler extends ToolHandler {
    private final DrawableAction drawableAction;
    private final Tool activeTool;

    public DrawableCheckHandler(Tool activeTool) {
        drawableAction = new DrawableAction(activeTool.getName()) {
            @Override
            protected void process(Drawable dr) {
                // The action itself does nothing - it's only used for
                // showing the rasterization dialogs
            }
        };
        this.activeTool = activeTool;
    }

    @Override
    boolean mousePressed(PMouseEvent e) {
        if (shouldForwardToNextHandler(e)) {
            return false;
        }

        // If we are here, offer the auto-rasterization as last resort.
        drawableAction.actionPerformed(null);

        // Whatever happened, do not forward this event,
        // the user should click again in order to use the tool
        return true;
    }

    @Override
    boolean mouseDragged(PMouseEvent e) {
        return !shouldForwardToNextHandler(e);
    }

    @Override
    boolean mouseReleased(PMouseEvent e) {
        return !shouldForwardToNextHandler(e);
    }

    private boolean shouldForwardToNextHandler(PMouseEvent e) {
        Composition activeComp = e.getComp();
        if (activeComp.canDrawOnActiveLayer()) { // normal case
            return true;
        }

        // special layers may bypass this check if the active tool is their preferred tool
        if (activeTool == activeComp.getActiveLayer().getPreferredTool()) {
            return true;
        }

        // show the auto-rasterization dialog
        return false;
    }
}
