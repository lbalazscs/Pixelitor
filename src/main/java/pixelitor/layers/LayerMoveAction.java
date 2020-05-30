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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.Layers;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An {@link Action} that moves the active layer of the active composition
 * up or down in the layer stack
 */
public class LayerMoveAction extends AbstractAction
    implements ViewActivationListener, ActiveCompositionListener {

    public static final LayerMoveAction INSTANCE_UP = new LayerMoveAction(true);
    public static final LayerMoveAction INSTANCE_DOWN = new LayerMoveAction(false);

    // menu and history names (also for selection movements)
    public static final String RAISE_LAYER = "Raise Layer";
    public static final String LOWER_LAYER = "Lower Layer";
    public static final String LAYER_TO_TOP = "Layer to Top";
    public static final String LAYER_TO_BOTTOM = "Layer to Bottom";
    public static final String LOWER_LAYER_SELECTION = "Lower Layer Selection";
    public static final String RAISE_LAYER_SELECTION = "Raise Layer Selection";

    private final boolean up;

    private LayerMoveAction(boolean up) {
        super(getName(up), getIcon(up));
        this.up = up;
        setEnabled(false);
        OpenImages.addActivationListener(this);
        Layers.addCompositionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var comp = OpenImages.getActiveComp();
        if (up) {
            comp.moveActiveLayerUp();
        } else {
            comp.moveActiveLayerDown();
        }
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        enableDisable(newView.getComp());
    }

    public void enableDisable(Composition comp) {
        if (comp != null) {
            int activeLayerIndex = comp.getActiveLayerIndex();
            if (up) {
                int numLayers = comp.getNumLayers();
                if (activeLayerIndex < numLayers - 1) {
                    setEnabled(true);
                } else {
                    setEnabled(false);
                }
            } else {
                if (activeLayerIndex > 0) {
                    setEnabled(true);
                } else {
                    setEnabled(false);
                }
            }
        }
    }

    @Override
    public void numLayersChanged(Composition comp, int newLayerCount) {
        enableDisable(comp);
    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        var comp = newActiveLayer.getComp();
        enableDisable(comp);
    }

    @Override
    public void layerOrderChanged(Composition comp) {
        enableDisable(comp);
    }

    private static Icon getIcon(boolean up) {
        return up ? Icons.getNorthArrowIcon() : Icons.getSouthArrowIcon();
    }

    private static String getName(boolean up) {
        return up ? RAISE_LAYER : LOWER_LAYER;
    }
}
