/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.test.RandomGuiTest;

import java.awt.EventQueue;
import java.util.function.Consumer;

/**
 * An action that operates on a {@link Drawable}.
 * <p>
 * Depending on the active layer, the task is run
 * <ul>
 *   <li>on the layer's mask, if the mask is being edited,
 *   <li>on the layer itself, if it is a {@link Drawable},
 *   <li>on the smart object, if a smart object (or one of its smart filters)
 *   is active and a smart object action was given,
 *   <li>otherwise on a newly rasterized image layer, if the user
 *   agrees to rasterize the active layer.
 * </ul>
 * Layers that are edited by the active tool itself (those whose
 * {@link Layer#getPreferredTool()} is the active tool) never get here:
 * they are filtered out earlier by
 * {@link pixelitor.tools.toolhandlers.DrawableCheckHandler}.
 */
public class DrawableAction extends AbstractViewEnabledAction {
    private final String name;

    /**
     * The core task of this action. It receives the active layer if it is a
     * {@link Drawable}, the active layer's mask if the mask is being edited,
     * or a newly rasterized image layer that replaced the active layer.
     */
    private final Consumer<Drawable> action;

    /**
     * Handles smart objects (and smart filters, via their smart object)
     * directly. If null, smart objects are offered for rasterization
     * like any other non-drawable layer.
     */
    private final Consumer<SmartObject> smartObjectAction;

    public DrawableAction(String name, Consumer<Drawable> action) {
        this(name, true, action, null);
    }

    /**
     * @param smartObjectAction handles smart objects directly;
     *                          if null, smart objects are offered for rasterization
     */
    public DrawableAction(String name, boolean hasDialog,
                          Consumer<Drawable> action,
                          Consumer<SmartObject> smartObjectAction) {
        super(name);
        assert name != null;
        assert action != null;

        this.name = name;
        this.action = action;
        this.smartObjectAction = smartObjectAction;

        setText(hasDialog ? name + "..." : name);
    }

    /**
     * Runs the given task on the active layer (or on its mask if the mask
     * is being edited). If the active layer isn't a {@link Drawable}, the user
     * may be asked to rasterize it, and the task then runs on the rasterized layer.
     * The task is started asynchronously, after the current event is handled.
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
        if (layer.isEditingMask()) {
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
            askAndRasterize(layer);
        } else {
            handleUnsupportedLayer(layer);
        }
    }

    private void handleUnsupportedLayer(Layer layer) {
        if (layer instanceof AdjustmentLayer) {
            Dialogs.showError("Adjustment Layer",
                name + " can't be used on adjustment layers.");
        } else if (layer instanceof LayerGroup group) {
            // isolated groups can be rasterized and are handled by askAndRasterize
            assert group.isPassThrough();
            Messages.showUnrasterizableLayerGroupError(group, name);
        } else {
            // this should not be reached if all layer types are handled
            throw new IllegalStateException("unsupported layer type: " + layer.getClass().getSimpleName());
        }
    }

    private void handleSmartObject(SmartObject so) {
        if (smartObjectAction != null) {
            smartObjectAction.accept(so);
        } else {
            askAndRasterize(so);
        }
    }

    /**
     * Asks the user whether the layer should be rasterized, and if so,
     * replaces it with an image layer and runs the action on that.
     * <p>
     * Does nothing in {@link RandomGuiTest}: the robot can't close dialogs,
     * and silently rasterizing would also gradually turn the special layers
     * (text, shapes, smart objects...) into image layers, reducing test coverage.
     */
    private void askAndRasterize(Layer layer) {
        if (RandomGuiTest.isRunning()) {
            return;
        }
        if (Dialogs.showRasterizeQuestion(layer, name)) {
            action.accept(layer.replaceWithRasterized());
        }
    }
}
