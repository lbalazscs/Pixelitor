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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.NamedAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import java.awt.event.ActionEvent;

import static pixelitor.utils.Texts.i18n;

/**
 * An action that adds a new image layer to the active composition
 */
public class AddNewLayerAction extends NamedAction implements ViewActivationListener {
    public static final AddNewLayerAction INSTANCE = new AddNewLayerAction();

    private AddNewLayerAction() {
        super(i18n("new_layer"), Icons.loadThemed("add_layer.gif", ThemedImageIcon.GREEN));
        setToolTip("<html>Adds a new transparent image layer." +
            "<br><b>Ctrl-click</b> to add the new layer below the active one.");
        setEnabled(false);
        Views.addActivationListener(this);
    }

    @Override
    public void onClick(ActionEvent e) {
        Composition comp = Views.getActiveComp();
        String layerName = comp.generateNewLayerName();
        boolean belowActive = GUIUtils.isCtrlPressed(e);
        comp.addNewEmptyImageLayer(layerName, belowActive);
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        setEnabled(true);
    }
}
