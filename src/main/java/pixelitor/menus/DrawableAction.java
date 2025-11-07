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

package pixelitor.menus;

import pixelitor.Composition;
import pixelitor.gui.utils.AbstractViewEnabledAction;
import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.*;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import java.awt.EventQueue;
import java.util.function.Consumer;

/**
 * An action that operates on a {@link Drawable}, and can optionally
 * rasterize other layer types or apply to smart objects.
 */
public class DrawableAction extends AbstractViewEnabledAction {
    protected final String name;
    private final boolean allowSmartObjects;

    /**
     * Defines the core task for this action, which operates on a {@link Drawable}
     * that is either the original active layer or a newly rasterized one.
     */
    private final Consumer<Drawable> action;

    private final Consumer<SmartObject> smartObjectAction;

    public DrawableAction(String name, Consumer<Drawable> action) {
        this(name, true, false, action, null);
    }

    public DrawableAction(String name, boolean hasDialog, boolean allowSmartObjects,
                          Consumer<Drawable> action, Consumer<SmartObject> smartObjectAction) {
        super(name);
        this.allowSmartObjects = allowSmartObjects;
        this.action = action;
        this.smartObjectAction = smartObjectAction;
        assert name != null;
        assert action != null;

        this.name = name;
        String menuName = hasDialog ? name + "..." : name;
        setText(menuName);
    }

    /**
     * Runs the given task on the active layer if it is a {@link Drawable}.
     */
    public static void run(String taskName, Consumer<Drawable> task) {
        var drawableAction = new DrawableAction(taskName, task);
        // Invoke later, because this is typically called from a GUI listener
        // and showing a dialog right now can cause weird things (the combo box
        // remains opened, the dialog button has to be pressed twice).
        // Also, it is good practice to ensure that the effects of original GUI change are complete.
        EventQueue.invokeLater(() -> drawableAction.actionPerformed(null));
    }

    @Override
    protected void onClick(Composition comp) {
        startOnLayer(comp.getActiveLayer());
    }

    private void startOnLayer(Layer layer) {
        // if we are editing a mask, the action applies to the mask
        if (layer.isMaskEditing()) {
            action.accept(layer.getMask());
            return;
        }

        // if the layer itself is a drawable, process it directly
        if (layer instanceof Drawable dr) {
            action.accept(dr);
            return;
        }

        // handle layers that can be rasterized or have special handling
        if (layer instanceof SmartObject so) {
            handleSmartObject(so);
        } else if (layer instanceof SmartFilter smartFilter) {
            // for a smart filter, the action applies to its parent smart object
            handleSmartObject(smartFilter.getSmartObject());
        } else if (layer.isRasterizable()) {
            handleRasterizableLayer(layer);
        } else {
            handleUnsupportedLayer(layer);
        }
    }

    private void handleRasterizableLayer(Layer layer) {
        if (RandomGUITest.isRunning()) {
            return;
        }

        // special case: the gradient tool is handled by activating the tool
        // itself, so this action should do nothing on a gradient fill layer
        if (layer.getClass() == GradientFillLayer.class && name.equals("Gradient Tool")) {
            return;
        }

        boolean rasterize = Dialogs.showRasterizeDialog(layer, name);
        if (rasterize) {
            ImageLayer rasterizedLayer = layer.replaceWithRasterized();
            action.accept(rasterizedLayer);
        }
    }

    private void handleUnsupportedLayer(Layer layer) {
        if (layer instanceof AdjustmentLayer) {
            Dialogs.showErrorDialog("Adjustment Layer",
                name + " can't be used on adjustment layers.");
        } else if (layer instanceof LayerGroup group) {
            // isolated groups can be rasterized and are handled by handleRasterizableLayer
            assert group.isPassThrough();
            Messages.showUnrasterizableLayerGroupError(group, name);
        } else {
            // this should not be reached if all layer types are handled
            throw new IllegalStateException("unsupported layer type: " + layer.getClass().getSimpleName());
        }
    }

    private void handleSmartObject(SmartObject so) {
        if (allowSmartObjects) {
            if (smartObjectAction != null) {
                smartObjectAction.accept(so);
            }
            return;
        }
        boolean rasterize = Dialogs.showRasterizeDialog(so, name);
        if (rasterize) {
            ImageLayer rasterizedLayer = so.replaceWithRasterized();
            action.accept(rasterizedLayer);
        }
    }

    public String getName() {
        return name;
    }
}
