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

package pixelitor.layers;

import pixelitor.Invariants;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.NamedAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An {@link Action} that adds a new layer mask
 * to the active layer of the active composition.
 */
public class AddLayerMaskAction extends NamedAction
    implements ViewActivationListener, ActiveMaskListener, ActiveHolderListener {

    public static final AddLayerMaskAction INSTANCE = new AddLayerMaskAction();

    private AddLayerMaskAction() {
        super("Add Layer Mask", Icons.loadThemed("add_layer_mask.png", ThemedImageIcon.GREEN));
        setToolTip("<html>Adds a layer mask to the active layer. " +
            "<br><b>Ctrl-click</b> to add an inverted layer mask.");
        setEnabled(false);
        Views.addActivationListener(this);
        LayerEvents.addHolderListener(this);
        LayerEvents.addMaskListener(this);
    }

    @Override
    public void onClick(ActionEvent e) {
        var layer = Views.getActiveComp().getActiveLayer();
        assert !layer.hasMask();

        layer.addMask(GUIUtils.isCtrlPressed(e));
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        boolean hasMask = newView.getComp().getActiveLayer().hasMask();
        setEnabled(!hasMask);
    }

    @Override
    public void maskAdded(Layer layer) {
        assert layer.hasMask();
        setEnabled(false);
    }

    @Override
    public void maskDeleted(Layer layer) {
        assert !layer.hasMask();
        setEnabled(true);
    }

    @Override
    public void layerActivated(Layer layer) {
        setEnabled(!layer.hasMask());
    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(newValue);

        assert Invariants.addMaskActionEnabled(Views.getActiveComp());
    }
}
