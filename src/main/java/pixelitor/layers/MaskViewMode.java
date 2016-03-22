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

import static pixelitor.menus.MenuBar.CTRL_1;
import static pixelitor.menus.MenuBar.CTRL_2;
import static pixelitor.menus.MenuBar.CTRL_3;
import static pixelitor.menus.MenuBar.CTRL_4;

/**
 * Determines whether the layer or its mask is visible/edited.
 * Every ImageComponent has an associated mask view mode, but
 * the layers don't.
 */
public enum MaskViewMode {

    NORMAL("Show and Edit Layer", false, false, false, AllowedLayerType.ANY, CTRL_1) {
    }, SHOW_MASK("Show and Edit Mask", true, true, false, AllowedLayerType.HAS_LAYER_MASK, CTRL_2) {
    }, EDIT_MASK("Show Layer, but Edit Mask", false, true, false, AllowedLayerType.HAS_LAYER_MASK, CTRL_3) {
    }, RUBYLITH("Show Mask as Rubylith, Edit Mask", false, true, true, AllowedLayerType.HAS_LAYER_MASK, CTRL_4) {
    };


    private final String guiName;
    private final boolean showRuby;
    private final AllowedLayerType allowedLayerType;
    private final KeyStroke keyStroke;
    private final boolean showMask;
    private final boolean editMask;

    MaskViewMode(String guiName, boolean showMask, boolean editMask, boolean showRuby, AllowedLayerType allowedLayerType, KeyStroke keyStroke) {
        this.guiName = guiName;
        this.showMask = showMask;
        this.editMask = editMask;
        this.showRuby = showRuby;
        this.allowedLayerType = allowedLayerType;
        this.keyStroke = keyStroke;
    }

    public void addToMenu(PMenu sub) {
        Action action = new MenuAction(guiName, allowedLayerType) {
            @Override
            public void onClick() {
                ImageComponent ic = ImageComponents.getActiveIC();
                if (ic != null) {
                    Layer activeLayer = ic.getComp().getActiveLayer();
                    activate(ic, activeLayer);
                }
            }
        };
        sub.addActionWithKey(action, keyStroke);
    }

    public JMenuItem createPopupMenuItem(Layer layer) {
        AbstractAction action = new AbstractAction(guiName) {
            @Override
            public void actionPerformed(ActionEvent e) {
                activate(layer);
            }
        };
        JMenuItem item = new JMenuItem(action);
        item.setAccelerator(keyStroke);
        return item;
    }

    public void activate(Layer activeLayer) {
        ImageComponent ic = activeLayer.getComp().getIC();
        activate(ic, activeLayer);
    }

    public void activate(Composition comp, Layer activeLayer) {
        activate(comp.getIC(), activeLayer);
    }

    public void activate(ImageComponent ic, Layer layer) {
        if (ic != null) {
            if (Build.CURRENT != Build.FINAL) {
                Events.postMaskViewActivate(this, ic, layer);
            }

            boolean change = ic.setMaskViewMode(this);
            layer.setMaskEditing(editMask);
            if (change) {
                FgBgColors.setLayerMaskEditing(editMask);

                if (!ic.isMock()) {
                    Tools.BRUSH.setupMaskDrawing(editMask);
                    Tools.CLONE.setupMaskDrawing(editMask);
                    Tools.GRADIENT.setupMaskDrawing(editMask);
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

                if (Build.CURRENT.isDevelopment()) {
                    assert ConsistencyChecks.fadeCheck(layer.getComp());
                }
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
    public boolean checkOnAssignment(Layer layer) {
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
