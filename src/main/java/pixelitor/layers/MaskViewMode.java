/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.View;
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.gui.utils.RestrictedLayerAction.LayerRestriction;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.History;
import pixelitor.menus.PMenu;
import pixelitor.menus.edit.FadeAction;
import pixelitor.tools.Tools;
import pixelitor.utils.test.Events;

import javax.swing.*;

import static pixelitor.utils.Keys.CTRL_1;
import static pixelitor.utils.Keys.CTRL_2;
import static pixelitor.utils.Keys.CTRL_3;
import static pixelitor.utils.Keys.CTRL_4;

/**
 * Whether the layer or its mask is visible/edited,
 * and whether the mask editing is done in "rubylith" mode.
 */
public enum MaskViewMode {
    NORMAL("Show and Edit Layer", false, false, false,
        LayerRestriction.ALLOW_ALL, CTRL_1) {
    }, SHOW_MASK("Show and Edit Mask", true, true, false,
        LayerRestriction.HAS_LAYER_MASK, CTRL_2) {
    }, EDIT_MASK("Show Layer, but Edit Mask", false, true, false,
        LayerRestriction.HAS_LAYER_MASK, CTRL_3) {
    }, RUBYLITH("Show Mask as Rubylith, Edit Mask", false, true, true,
        LayerRestriction.HAS_LAYER_MASK, CTRL_4) {
    };

    private final String displayName;
    private final boolean showRuby;
    private final LayerRestriction layerRestriction;
    private final KeyStroke keyStroke;
    private final boolean showMask;
    private final boolean editMask;

    MaskViewMode(String displayName, boolean showMask, boolean editMask, boolean showRuby,
                 LayerRestriction layerRestriction, KeyStroke keyStroke) {
        this.displayName = displayName;
        this.showMask = showMask;
        this.editMask = editMask;
        this.showRuby = showRuby;
        this.layerRestriction = layerRestriction;
        this.keyStroke = keyStroke;
    }

    /**
     * Adds a menu item that acts on the active layer of the active image
     */
    public void addToMenuBar(PMenu sub) {
        var action = new RestrictedLayerAction(displayName, layerRestriction) {
            @Override
            public void onActiveLayer(Layer layer) {
                activate(layer);
            }
        };
        sub.add(action, keyStroke);
    }

    /**
     * Adds a menu item that acts on the given layer and its image
     */
    public void addToPopupMenu(JMenu menu, Layer layer) {
        var menuItem = new JMenuItem(new TaskAction(displayName, () ->
            activate(layer)));
        menuItem.setAccelerator(keyStroke);
        menu.add(menuItem);
    }

    public void activate(Layer activeLayer) {
        View view = activeLayer.getComp().getView();
        activate(view, activeLayer);
    }

    public void activate(Composition comp, Layer activeLayer) {
        activate(comp.getView(), activeLayer);
    }

    public void activate(View view, Layer layer) {
        assert view != null;
        assert layer.isActive();

        if (AppMode.isDevelopment()) {
            Events.postMaskViewActivate(this, view, layer);
        }

        assert canBeAssignedTo(layer);

        boolean changed = view.setMaskViewModeInternal(this);
        layer.setMaskEditing(editMask);
        if (changed) {
            FgBgColors.maskEditingChanged(editMask);

            if (!view.isMock()) {
                Tools.maskEditingChanged(editMask);
            }

            boolean canFade;
            if (editMask) {
                canFade = History.canFade(layer.getMask());
            } else {
                if (layer instanceof ImageLayer imageLayer) {
                    canFade = History.canFade(imageLayer);
                } else {
                    canFade = false;
                }
            }
            FadeAction.INSTANCE.updateGUI(canFade);
        }
    }

    public boolean editMask() {
        return editMask;
    }

    public boolean showMask() {
        return showMask;
    }

    public boolean showRubylith() {
        return showRuby;
    }

    private boolean canBeAssignedTo(Layer layer) {
        if (editMask || showMask) {
            boolean hasMask = layer.hasMask();
            if (!hasMask) {
                throw new AssertionError("layer " + layer.getName()
                    + " has no mask, view mode = " + this
                    + ", mask icon = " + layer.getUI().hasMaskIcon());
            }
            return hasMask;
        }
        return true;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
