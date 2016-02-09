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

package pixelitor.layers;

import pixelitor.FgBgColors;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.menus.MenuAction;
import pixelitor.menus.MenuAction.AllowedLayerType;
import pixelitor.tools.Tools;

/**
 * Determines whether the layer or its mask is visible/edited.
 * Every ImageComponent has an associated mask view mode, but
 * the layers don't.
 */
public enum MaskViewMode {
    NORMAL("Show and Edit Layer", false, false, AllowedLayerType.ANY) {
    }, SHOW_MASK("Show and Edit Mask", true, true, AllowedLayerType.HAS_LAYER_MASK) {
    }, EDIT_MASK("Show Layer, but Edit Mask", false, true, AllowedLayerType.HAS_LAYER_MASK) {
    };

    private final String guiName;
    private final AllowedLayerType allowedLayerType;
    private final boolean showMask;
    private final boolean editMask;

    MaskViewMode(String guiName, boolean showMask, boolean editMask, AllowedLayerType allowedLayerType) {
        this.guiName = guiName;
        this.showMask = showMask;
        this.editMask = editMask;
        this.allowedLayerType = allowedLayerType;
    }

    public MenuAction createMenuAction() {
        return new MenuAction(guiName, allowedLayerType) {
            @Override
            public void onClick() {
                ImageComponent ic = ImageComponents.getActiveIC();
                if (ic != null) {
                    Layer activeLayer = ic.getComp().getActiveLayer();
                    activate(ic, activeLayer);
                }
            }
        };
    }

    public void activate(Layer activeLayer) {
        ImageComponent ic = activeLayer.getComp().getIC();
        activate(ic, activeLayer);
    }

    public void activate(ImageComponent ic, Layer activeLayer) {
        if (ic != null) {
            ic.setMaskViewMode(this);
            FgBgColors.setLayerMaskEditing(editMask);
            activeLayer.setMaskEditing(editMask);

            if (!ic.isMock()) {
                Tools.BRUSH.setupMaskDrawing(editMask);
                Tools.CLONE.setupMaskDrawing(editMask);
                Tools.GRADIENT.setupMaskDrawing(editMask);
            }
        }
    }

    public boolean editMask() {
        return editMask;
    }

    public boolean showMask() {
        return showMask;
    }

    // used in asserts
    public boolean checkOnAssignment(Layer layer) {
        if (editMask || showMask()) {
            return layer.hasMask();
        }
        return true;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
