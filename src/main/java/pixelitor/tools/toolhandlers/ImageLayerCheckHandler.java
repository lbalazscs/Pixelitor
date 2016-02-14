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
import pixelitor.utils.Messages;

import java.awt.event.MouseEvent;

/**
 * Checks whether the active layer is an image layer.
 */
public class ImageLayerCheckHandler extends ToolHandler {
    public ImageLayerCheckHandler() {
    }

    @Override
    boolean mousePressed(MouseEvent e, ImageComponent ic) {
        if (!ic.activeIsImageLayerOrMask()) {
            Messages.showNotImageLayerError();
            return true;
        }

        // forwards the mouse event to the next handler
        return false;
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
