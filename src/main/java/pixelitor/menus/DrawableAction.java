/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.OpenImages;
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.OpenImageEnabledAction;
import pixelitor.layers.*;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.EventQueue;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * An {@link Action} that can be done with {@link Drawable}
 * objects (image layers or masks)
 */
public abstract class DrawableAction extends OpenImageEnabledAction {
    protected final String name;
    protected String menuName;
    protected boolean hasDialog;
    private final boolean allowSmartObjects;

    private final boolean allowMasks;

    protected DrawableAction(String name) {
        this(name, true, false);
    }

    protected DrawableAction(String name, boolean hasDialog) {
        this(name, hasDialog, true);
    }

    private DrawableAction(String name, boolean hasDialog, boolean allowSmartObjects) {
        this.hasDialog = hasDialog;
        this.allowSmartObjects = allowSmartObjects;
        this.allowMasks = true;
        assert name != null;

        this.name = name;
        menuName = hasDialog ? name + "..." : name;
        setText(menuName);
    }

    /**
     * Runs the given task if the active layer is drawable
     */
    public static void run(String taskName, Consumer<Drawable> task) {
        var action = new DrawableAction(taskName) {
            @Override
            protected void process(Drawable dr) {
                task.accept(dr);
            }
        };
        // Invoke later, because this is typically called from a GUI listener
        // and showing a dialog right now can cause weird things (the combo box
        // remains opened, the dialog button has to be pressed twice).
        // Also it is nice to be sure that the effects of original GUI change are done.
        EventQueue.invokeLater(() -> action.actionPerformed(null));
    }

    /**
     * This callback method represents the task that has to be done.
     * It gets called only if we know that the active layer is
     * a {@link Drawable} or if it was rasterized into a {@link Drawable}.
     */
    protected abstract void process(Drawable dr);

    @Override
    public void onClick() {
        OpenImages.onActiveLayer(this::startOnLayer);
    }

    private void startOnLayer(Layer layer) {
        if (layer.isMaskEditing()) {
            if (allowMasks) {
                process(layer.getMask());
            } else {
                Dialogs.showErrorDialog("Mask is active",
                    name + " cannot be applied to masks.");
            }
        } else if (layer instanceof ImageLayer imageLayer) {
            if (!allowSmartObjects && imageLayer instanceof SmartObject so) {
                boolean rasterize = showRasterizeDialog(layer);
                if (rasterize) {
                    ImageLayer newImageLayer = so.replaceWithRasterized();
                    process(newImageLayer);
                }
            } else {
                process(imageLayer);
            }
        } else if (layer.isRasterizable()) {
            if (RandomGUITest.isRunning()) {
                return;
            }

            // special case: gradient tool is allowed on Gradient Fill Layers
            if (layer.getClass() == GradientFillLayer.class && name.equals("Gradient Tool")) {
                return;
            }

            boolean rasterize = showRasterizeDialog(layer);
            if (rasterize) {
                ImageLayer newImageLayer = layer.replaceWithRasterized();
                process(newImageLayer);
            }
        } else if (layer instanceof AdjustmentLayer) {
            Dialogs.showErrorDialog("Adjustment Layer",
                name + " cannot be applied to adjustment layers.");
        } else {
            throw new IllegalStateException("layer is " + layer.getClass().getSimpleName());
        }
    }

    private boolean showRasterizeDialog(Layer layer) {
        boolean isNoun = name.contains("Tool");
        String firstName = isNoun ? "The " + name : name;
        String secondName = isNoun ? "the " + name : name;

        String typeStringLC = layer.getTypeStringLC();
        String msg = format("<html>The active layer <i>\"%s\"</i> is a %s.<br><br>" +
                            "%s cannot be used on %ss.<br>" +
                            "If you rasterize this %s, you can use %s,<br>" +
                            "but the layer will become a regular image layer.",
            layer.getName(), typeStringLC, firstName,
            typeStringLC, typeStringLC, secondName);

        String[] options = {"Rasterize", GUIText.CANCEL};

        boolean rasterize = Dialogs.showOKCancelWarningDialog(msg,
            layer.getTypeString(), options, 1);
        return rasterize;
    }

    public String getName() {
        return name;
    }
}
