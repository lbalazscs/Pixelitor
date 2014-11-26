/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.toolhandlers;

import pixelitor.ImageComponent;
import pixelitor.layers.Layers;
import pixelitor.utils.Dialogs;

import java.awt.event.MouseEvent;

/**
 * Checks whether the active layer is an image layer, and forwards the request to the next ToolEventHandler if not.
 */
public class ImageLayerCheckHandler extends ToolEventHandler {
    @Override
    boolean mousePressed(MouseEvent e, ImageComponent ic) {
        if (!Layers.activeIsImageLayer()) {
            Dialogs.showNotImageLayerDialog();
            return true;
        }

        return false;
    }

    @Override
    boolean mouseDragged(MouseEvent e, ImageComponent ic) {
        return !Layers.activeIsImageLayer();
    }

    @Override
    boolean mouseReleased(MouseEvent e, ImageComponent ic) {
        return !Layers.activeIsImageLayer();
    }
}
