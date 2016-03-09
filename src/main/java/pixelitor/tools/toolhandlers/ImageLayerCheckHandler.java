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
import pixelitor.layers.ImageLayer;
import pixelitor.menus.ImageLayerAction;
import pixelitor.tools.Tool;

import java.awt.event.MouseEvent;

/**
 * Checks whether the active layer is an image layer.
 */
public class ImageLayerCheckHandler extends ToolHandler {
    private final Tool currentTool;

    public ImageLayerCheckHandler(Tool currentTool) {
        this.currentTool = currentTool;
    }

    @Override
    boolean mousePressed(MouseEvent e, ImageComponent ic) {
        if(ic.activeIsImageLayerOrMask()) {
            // forwards the mouse event to the next handler
            return false;
        }

        ImageLayerAction action = new ImageLayerAction(currentTool.getName() + " Tool") {
            @Override
            protected void process(ImageLayer layer) {
                // do nothing
            }
        };
        // call it only for the dialogs and auto-rasterization
        action.actionPerformed(null);

        // whatever happened, do not forward this event,
        // the user should click again in order to use the tool
        return true;
    }

    @Override
    boolean mouseDragged(MouseEvent e, ImageComponent ic) {
        return !ic.activeIsImageLayerOrMask();
    }

    @Override
    boolean mouseReleased(MouseEvent e, ImageComponent ic) {
        return !ic.activeIsImageLayerOrMask();
    }
}
