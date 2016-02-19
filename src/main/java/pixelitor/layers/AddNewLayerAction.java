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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An Action that adds a new layer
 */
public class AddNewLayerAction extends AbstractAction implements ImageSwitchListener {
    public static final AddNewLayerAction INSTANCE = new AddNewLayerAction();

    private AddNewLayerAction() {
        super("Add New Layer", IconUtils.loadIcon("add_layer.gif"));
        putValue(Action.SHORT_DESCRIPTION, "<html>Adds a new empty image layer.<br>Ctrl-click to add the new layer bellow the active one.");
        setEnabled(false);
        ImageComponents.addImageSwitchListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        boolean addBellowActive = ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
        comp.addNewEmptyLayer(null, addBellowActive);
    }

    @Override
    public void noOpenImageAnymore() {
        setEnabled(false);
    }

    @Override
    public void newImageOpened(Composition comp) {
        setEnabled(true);
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        setEnabled(true);
    }
}