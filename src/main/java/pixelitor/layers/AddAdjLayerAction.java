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
import pixelitor.filters.Invert;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An Action that adds a new adjustment layer.
 */
public class AddAdjLayerAction extends AbstractAction implements ImageSwitchListener {
    public static final AddAdjLayerAction INSTANCE = new AddAdjLayerAction();

    private AddAdjLayerAction() {
        super("Add Adjustment Layer", IconUtils.loadIcon("add_adj_layer.png"));
        putValue(Action.SHORT_DESCRIPTION, "Adds a new adjustment layer.");
        setEnabled(false);
        ImageComponents.addImageSwitchListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        AdjustmentLayer adjustmentLayer = new AdjustmentLayer(comp, "Invert", new Invert());
        comp.addLayer(adjustmentLayer, AddToHistory.YES, "New Adjustment Layer", true, false);
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