/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.layers.*;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.EventQueue;
import java.util.function.Consumer;

/**
 * An {@link Action} that can be done with {@link Drawable}
 * objects (image layers or masks)
 */
public abstract class DrawableAction extends OpenViewEnabledAction.Checked {
    protected final String name;
    private final boolean allowSmartObjects;

    protected DrawableAction(String name) {
        this(name, true, false);
    }

    protected DrawableAction(String name, boolean hasDialog) {
        this(name, hasDialog, true);
    }

    protected DrawableAction(String name, boolean hasDialog, boolean allowSmartObjects) {
        this.allowSmartObjects = allowSmartObjects;
        assert name != null;

        this.name = name;
        String menuName = hasDialog ? name + "..." : name;
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
        // Also, it is nice to be sure that the effects of original GUI change are done.
        EventQueue.invokeLater(() -> action.actionPerformed(null));
    }

    /**
     * This callback method represents the task that has to be done.
     * It gets called only if we know that the active layer is
     * a {@link Drawable} or if it was rasterized into a {@link Drawable}.
     */
    protected abstract void process(Drawable dr);

    @Override
    protected void onClick(Composition comp) {
        startOnLayer(comp.getActiveLayer());
    }

    private void startOnLayer(Layer layer) {
        if (layer.isMaskEditing()) {
            process(layer.getMask());
        } else if (layer instanceof ImageLayer imageLayer) {
            process(imageLayer);
        } else if (layer instanceof SmartObject so) {
            if (!allowSmartObjects) {
                boolean rasterize = Dialogs.showRasterizeDialog(layer, name);
                if (rasterize) {
                    ImageLayer newImageLayer = so.replaceWithRasterized();
                    process(newImageLayer);
                }
            }
        } else if (layer.isRasterizable()) {
            if (RandomGUITest.isRunning()) {
                return;
            }

            // special case: gradient tool is allowed on Gradient Fill Layers
            if (layer.getClass() == GradientFillLayer.class && name.equals("Gradient Tool")) {
                return;
            }

            boolean rasterize = Dialogs.showRasterizeDialog(layer, name);
            if (rasterize) {
                ImageLayer newImageLayer = layer.replaceWithRasterized();
                process(newImageLayer);
            }
        } else if (layer instanceof AdjustmentLayer) {
            Dialogs.showErrorDialog("Adjustment Layer",
                    name + " can't be used on adjustment layers.");
        } else if (layer instanceof LayerGroup group) {
            assert group.isPassThrough(); // isolated groups can be rasterized
            Messages.showUnrasterizableLayerGroupError(group, name);
        } else {
            throw new IllegalStateException("layer is " + layer.getClass().getSimpleName());
        }
    }

    public String getName() {
        return name;
    }
}
