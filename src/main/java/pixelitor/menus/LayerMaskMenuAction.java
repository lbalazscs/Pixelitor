/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.menus;

import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.layers.Layer;
import pixelitor.utils.Dialogs;

import java.awt.event.ActionEvent;

/**
 * A menu action that is supposed to work only if the active
 * layer has a layer mask
 */
public abstract class LayerMaskMenuAction extends MenuAction {
    public LayerMaskMenuAction(String name) {
        super(name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageComponent ic = ImageComponents.getActiveImageComponent();
        Layer activeLayer = ic.getComp().getActiveLayer();
        if (activeLayer.hasLayerMask()) {
            try {
                onClick();
            } catch (Exception ex) {
                Dialogs.showExceptionDialog(ex);
            }
        } else {
            String msg = String.format("The layer \"%s\" has no layer mask", activeLayer.getName());
            Dialogs.showInfoDialog("No Layer mask", msg);
        }
    }
}
