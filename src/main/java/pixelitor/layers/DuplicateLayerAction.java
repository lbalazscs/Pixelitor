/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An Action that duplicates a layer
 */
public class DuplicateLayerAction extends AbstractAction implements ImageSwitchListener {
    public static final DuplicateLayerAction INSTANCE = new DuplicateLayerAction();

    private DuplicateLayerAction() {
        super("Duplicate Layer", IconUtils.loadIcon("duplicate_layer.png"));
        setEnabled(false);
        ImageComponents.addImageSwitchListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveComp().get();
        comp.duplicateLayer();
    }

    @Override
    public void noOpenImageAnymore() {
        setEnabled(false);
    }

    @Override
    public void newImageOpened() {
        setEnabled(true);
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        setEnabled(true);
    }
}