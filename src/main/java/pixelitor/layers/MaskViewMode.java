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

import pixelitor.gui.View;
import pixelitor.gui.utils.RestrictedLayerAction;
import pixelitor.gui.utils.RestrictedLayerAction.LayerRestriction;
import pixelitor.gui.utils.TaskAction;
import pixelitor.menus.PMenu;
import pixelitor.utils.Texts;

import javax.swing.*;

import static pixelitor.utils.Keys.CTRL_1;
import static pixelitor.utils.Keys.CTRL_2;
import static pixelitor.utils.Keys.CTRL_3;
import static pixelitor.utils.Keys.CTRL_4;

/**
 * Defines the visibility and editing state of a layer and its mask.
 */
public enum MaskViewMode {
    // Standard view where the layer content is visible and the mask applies transparency
    NORMAL("lm_edit_layer", false, false, false,
        LayerRestriction.ALLOW_ALL, CTRL_1),

    // Displays the grayscale mask image instead of the layer content
    VIEW_MASK("lm_view_mask", true, true, false,
        LayerRestriction.HAS_LAYER_MASK, CTRL_2),

    // Displays the layer content with the mask applied, but editing actions affect the mask
    EDIT_MASK("lm_edit_mask", false, true, false,
        LayerRestriction.HAS_LAYER_MASK, CTRL_3),

    // Displays the layer content with the mask overlaid as a red "rubylith" tint
    RUBYLITH("lm_ruby", false, true, true,
        LayerRestriction.HAS_LAYER_MASK, CTRL_4);

    private final String displayName;
    private final boolean showRuby;
    private final LayerRestriction layerRestriction;
    private final KeyStroke keyStroke;
    private final boolean showMask;
    private final boolean editMask;

    MaskViewMode(String displayKey, boolean showMask, boolean editMask, boolean showRuby,
                 LayerRestriction layerRestriction, KeyStroke keyStroke) {
        this.displayName = Texts.i18n(displayKey);
        this.showMask = showMask;
        this.editMask = editMask;
        this.showRuby = showRuby;
        this.layerRestriction = layerRestriction;
        this.keyStroke = keyStroke;
    }

    /**
     * Adds a menu item that acts on the active layer of the active composition.
     */
    public void addToMainMenu(PMenu sub) {
        sub.add(new RestrictedLayerAction(displayName,
            layerRestriction, this::activateOn), keyStroke);
    }

    /**
     * Adds a menu item that acts on the given layer.
     */
    public void addToPopupMenu(JMenu menu, Layer layer) {
        var menuItem = new JMenuItem(new TaskAction(displayName, () ->
            activateOn(layer)));
        menuItem.setAccelerator(keyStroke);
        menu.add(menuItem);
    }

    public void activateOn(Layer activeLayer) {
        View view = activeLayer.getComp().getView();
        view.setMaskViewMode(this, activeLayer);
    }

    public boolean isEditingMask() {
        return editMask;
    }

    public boolean isShowingMask() {
        return showMask;
    }

    public boolean isShowingRubylith() {
        return showRuby;
    }

    public boolean isApplicableTo(Layer layer) {
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
