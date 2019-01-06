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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.gui.CompositionView;
import pixelitor.gui.OpenComps;
import pixelitor.utils.CompActivationListener;
import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static java.awt.event.ActionEvent.CTRL_MASK;

/**
 * An Action that adds a new layer to the active composition
 */
public class AddNewLayerAction extends AbstractAction implements CompActivationListener {
    public static final AddNewLayerAction INSTANCE = new AddNewLayerAction();

    private AddNewLayerAction() {
        super("Add New Layer", Icons.load("add_layer.gif"));
        putValue(SHORT_DESCRIPTION,
                "<html>Adds a new transparent image layer." +
                        "<br><b>Ctrl-click</b> to add the new layer bellow the active one.");
        setEnabled(false);
        OpenComps.addActivationListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = OpenComps.getActiveCompOrNull();
        boolean addBellowActive = ((e.getModifiers() & CTRL_MASK) == CTRL_MASK);
        comp.addNewEmptyLayer(comp.generateNewLayerName(), addBellowActive);
    }

    @Override
    public void allCompsClosed() {
        setEnabled(false);
    }

    @Override
    public void compActivated(CompositionView oldIC, CompositionView newIC) {
        setEnabled(true);
    }
}