/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.OpenComps;
import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * An abstraction for a task that works with an "active image",
 * which can be a rasterized text layer.
 */
public abstract class GetImageAction extends AbstractAction {
    private final String name;
    private final String menuName;

    private final boolean allowMasks;

    protected GetImageAction(String name) {
        this(name, true, true);
    }

    protected GetImageAction(String name, boolean hasDialog) {
        this(name, hasDialog, true);
    }

    protected GetImageAction(String name, boolean hasDialog, boolean allowMasks) {
        assert name != null;

        this.name = name;
        this.menuName = hasDialog ? name + "..." : name;
        setActionName(menuName);

        this.allowMasks = allowMasks;
    }

    /**
     * This callback method represents the task that has to be done.
     */
    protected abstract void process(Layer layer, BufferedImage image);

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            OpenComps.onActiveLayer(this::startOnLayer);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private void startOnLayer(Layer layer) {
        if (layer.isMaskEditing()) {
            if (allowMasks) {
                BufferedImage image = layer.getMask().getImage();
                process(layer, image);
            } else {
                Dialogs.showErrorDialog("Layer Mask",
                        name + " cannot be applied to layer masks.");
            }
        } else if (layer instanceof ImageLayer) {
            BufferedImage image = ((ImageLayer) layer).getImage();
            process(layer, image);
        } else if (layer instanceof TextLayer) {
            BufferedImage image = ((TextLayer) layer).createRasterizedImage();
            process(layer, image);
        } else if (layer instanceof AdjustmentLayer) {
            Dialogs.showErrorDialog("Adjustment Layer",
                    name + " cannot be applied to adjustment layers.");
        } else {
            throw new IllegalStateException("layer is " + layer.getClass().getSimpleName());
        }
    }

    public String getMenuName() {
        return menuName;
    }

    public String getName() {
        return name;
    }

    public void setActionName(String newName) {
        putValue(AbstractAction.NAME, newName);
    }
}
