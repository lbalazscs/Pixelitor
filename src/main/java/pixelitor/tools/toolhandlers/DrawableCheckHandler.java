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
import pixelitor.layers.GradientFillLayer;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PMouseEvent;

/**
 * Checks whether the active edited object is
 * a Drawable (image layer or mask)
 */
public class DrawableCheckHandler extends ToolHandler {
    private final DrawableAction drawableAction;
    private final Tool currentTool;

    public DrawableCheckHandler(Tool currentTool) {
        drawableAction = new DrawableAction(currentTool.getName() + " Tool") {
            @Override
            protected void process(Drawable dr) {
                // do nothing
            }
        };
        this.currentTool = currentTool;
    }

    @Override
    boolean mousePressed(PMouseEvent e) {
        if (shouldForwardToNextHandler(e)) {
            return false;
        }

        // If we are here, offer the auto-rasterization as last resort.
        // The action itself does nothing, we call it only for the dialogs.
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
        if (e.getComp().activeAcceptsToolDrawing()) {
            return true;
        }
        if (currentTool == Tools.GRADIENT) {
            if (e.getComp().getActiveLayer().getClass() == GradientFillLayer.class) {
                return true;
            }
        }
        return false;
    }
}
