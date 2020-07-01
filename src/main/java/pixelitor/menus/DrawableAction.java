/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.NamedAction;
import pixelitor.layers.*;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * An {@link Action} that can be done with {@link Drawable}
 * objects (image layers or masks)
 */
public abstract class DrawableAction extends NamedAction {
    protected final String name;
    protected String menuName;
    protected boolean hasDialog;

    private final boolean allowMasks;

    protected DrawableAction(String name) {
        this(name, true, true);
    }

    protected DrawableAction(String name, boolean hasDialog) {
        this(name, hasDialog, true);
    }

    protected DrawableAction(String name, boolean hasDialog, boolean allowMasks) {
        this.hasDialog = hasDialog;
        assert name != null;

        this.name = name;
        menuName = hasDialog ? name + "..." : name;
        setText(menuName);

        this.allowMasks = allowMasks;
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
    public void actionPerformed(ActionEvent e) {
        try {
            OpenImages.onActiveLayer(this::startOnLayer);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private void startOnLayer(Layer layer) {
        if (layer.isMaskEditing()) {
            if (allowMasks) {
                process(layer.getMask());
            } else {
                Dialogs.showErrorDialog("Mask is active",
                    name + " cannot be applied to masks.");
            }
        } else if (layer instanceof ImageLayer) {
            process((ImageLayer) layer);
        } else if (layer instanceof TextLayer) {
            if (RandomGUITest.isRunning()) {
                return;
            }

            boolean isNoun = name.contains("Tool");
            String firstName = isNoun ? "The " + name : name;
            String secondName = isNoun ? "the " + name : name;

            String msg = format("<html>The active layer <i>\"%s\"</i> is a text layer.<br><br>" +
                    "%s needs pixels and cannot be used on text layers.<br>" +
                    "If you rasterize this text layer, you can use %s,<br>" +
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
}
