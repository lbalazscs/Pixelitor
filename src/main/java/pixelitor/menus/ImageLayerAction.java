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

package pixelitor.menus;

import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An abstraction for a task that can be done only with image layers (or masks)
 */
public abstract class ImageLayerAction extends AbstractAction {
    protected final String name;
    protected String menuName;
    protected boolean hasDialog;

    private final boolean allowMasks;

    protected ImageLayerAction(String name) {
        this(name, true, true);
    }

    protected ImageLayerAction(String name, boolean hasDialog) {
        this(name, hasDialog, true);
    }

    protected ImageLayerAction(String name, boolean hasDialog, boolean allowMasks) {
        this.hasDialog = hasDialog;
        assert name != null;

        this.name = name;
        this.menuName = hasDialog ? name + "..." : name;
        setActionName(menuName);

        this.allowMasks = allowMasks;
    }

    /**
     * This callback method represents the task that has to be done.
     * It gets called only if we know that the active layer is
     * an image layer or if it was rasterized into an image layer.
     */
    protected abstract void process(ImageLayer layer);

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            ImageComponents.onActiveLayer(this::startOnLayer);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private void startOnLayer(Layer layer) {
        if (layer.isMaskEditing()) {
            if (allowMasks) {
                process(layer.getMask());
            } else {
                if (!RandomGUITest.isRunning()) {
                    Dialogs.showErrorDialog("Mask is active",
                            name + " cannot be applied to masks.");
                }
            }
        } else if (layer instanceof ImageLayer) {
            process((ImageLayer) layer);
        } else if (layer instanceof TextLayer) {
            if (RandomGUITest.isRunning()) {
                return;
            }

            boolean isNoun = name.contains("Tool");
            String firstName = isNoun ? "The " + name  : name;
            String secondName = isNoun ? "the " + name  : name;

            String msg = String.format("The active layer \"%s\" is a text layer.\n" +
                                "%s needs pixels and cannot be used on text layers.\n" +
                                "If you rasterize this text layer, you can use %s,\n" +
                                "but the text will no longer be editable.",
                        layer.getName(), firstName, secondName);

            String[] options = {"Rasterize", "Cancel"};

            if (Dialogs.showOKCancelWarningDialog(msg, "Text Layer", options, 1)) {
                ImageLayer newImageLayer = ((TextLayer) layer).replaceWithRasterized();
                process(newImageLayer);
            }
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
