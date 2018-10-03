/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.menus.MenuAction;
import pixelitor.menus.MenuAction.AllowedLayerType;
import pixelitor.menus.PMenu;
import pixelitor.menus.edit.FadeMenuItem;
import pixelitor.tools.Tools;
import pixelitor.utils.test.Events;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static pixelitor.utils.Keys.CTRL_1;
import static pixelitor.utils.Keys.CTRL_2;
import static pixelitor.utils.Keys.CTRL_3;
import static pixelitor.utils.Keys.CTRL_4;

/**
 * Determines whether the layer or its mask is visible/edited.
 */
public enum MaskViewMode {
    NORMAL("Show and Edit Layer", false, false, false,
            AllowedLayerType.ANY, CTRL_1) {
    }, SHOW_MASK("Show and Edit Mask", true, true, false,
            AllowedLayerType.HAS_LAYER_MASK, CTRL_2) {
    }, EDIT_MASK("Show Layer, but Edit Mask", false, true, false,
            AllowedLayerType.HAS_LAYER_MASK, CTRL_3) {
    }, RUBYLITH("Show Mask as Rubylith, Edit Mask", false, true, true,
            AllowedLayerType.HAS_LAYER_MASK, CTRL_4) {
    };

    private final String guiName;
    private final boolean showRuby;
    private final AllowedLayerType allowedLayerType;
    private final KeyStroke keyStroke;
    private final boolean showMask;
    private final boolean editMask;

    MaskViewMode(String guiName, boolean showMask, boolean editMask, boolean showRuby,
                 AllowedLayerType allowedLayerType, KeyStroke keyStroke) {
        this.guiName = guiName;
        this.showMask = showMask;
        this.editMask = editMask;
        this.showRuby = showRuby;
        this.allowedLayerType = allowedLayerType;
        this.keyStroke = keyStroke;
    }

    /**
     * Adds a menu item that acts on the active layer of the active image
     */
    public void addToMainMenu(PMenu sub) {
        Action action = new MenuAction(guiName, allowedLayerType) {
            @Override
            public void onClick() {
                ImageComponents.onActiveIC(ic -> {
                    Layer activeLayer = ic.getComp().getActiveLayer();
                    activate(ic, activeLayer, "main menu");
                });
            }
        };
        sub.addActionWithKey(action, keyStroke);
    }

    /**
     * Adds a menu item that acts on the given layer and its image
     */
    public void addToPopupMenu(JMenu menu, Layer layer) {
        AbstractAction action = new AbstractAction(guiName) {
            @Override
            public void actionPerformed(ActionEvent e) {
                activate(layer, "popup menu");
            }
        };
        JMenuItem item = new JMenuItem(action);
        item.setAccelerator(keyStroke);
        menu.add(item);
    }

    public void activate(Layer activeLayer, String reason) {
        ImageComponent ic = activeLayer.getComp().getIC();
        activate(ic, activeLayer, reason);
    }

    public void activate(Composition comp, Layer activeLayer, String reason) {
        activate(comp.getIC(), activeLayer, reason);
    }

    public void activate(ImageComponent ic, Layer layer, String reason) {
        assert ic != null;
        if (Build.CURRENT != Build.FINAL) {
            Events.postMaskViewActivate(this, ic, layer, reason);
        }

        boolean change = ic.setMaskViewMode(this);
        layer.setMaskEditing(editMask);
        if (change) {
            FgBgColors.setLayerMaskEditing(editMask);

            if (!ic.isMock()) {
                Tools.BRUSH.setupMaskEditing(editMask);
                Tools.CLONE.setupMaskEditing(editMask);
                Tools.GRADIENT.setupMaskEditing(editMask);
            }

            boolean canFade;
            if (editMask) {
                canFade = History.canFade(layer.getMask());
            } else {
                if (layer instanceof ImageLayer) {
                    canFade = History.canFade((ImageLayer) layer);
                } else {
                    canFade = false;
                }
            }
            FadeMenuItem.INSTANCE.refresh(canFade);

            if (Build.isDevelopment()) {
                assert ConsistencyChecks.fadeWouldWorkOn(layer.getComp());
            }
        }
    }

    public boolean editMask() {
        return editMask;
    }

    public boolean showMask() {
        return showMask;
    }

    public boolean showRuby() {
        return showRuby;
    }

    // used in asserts
    public boolean canBeAssignedTo(Layer layer) {
        if (editMask || showMask) {
            return layer.hasMask();
        }
        return true;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
