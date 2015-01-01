/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.utils.BlendingModePanel;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.Optional;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The GUI selector for the opacity and blending mode of the layers
 */
public class LayerBlendingModePanel extends BlendingModePanel implements ImageSwitchListener, LayerChangeListener {
    private boolean userInteractionChange = true;

    public static final LayerBlendingModePanel INSTANCE = new LayerBlendingModePanel();

    private LayerBlendingModePanel() {
        super(false);

        ImageComponents.addImageSwitchListener(this);
        AppLogic.addLayerChangeListener(this);

        opacityDDSlider.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (userInteractionChange) {
                    opacityChanged();
                }
            }
        });

        blendingModeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (userInteractionChange) {
                    blendingModeChanged();
                }
            }
        });

        setEnabled(false);
    }


    private void opacityChanged() {
        Optional<Layer> activeLayer = ImageComponents.getActiveLayer();

        if (activeLayer.isPresent()) {
            float floatValue = getOpacity();
            activeLayer.get().setOpacity(floatValue, false, true, true);
        }
    }

    private void blendingModeChanged() {
        Optional<Layer> activeLayer = ImageComponents.getActiveLayer();

        if (activeLayer.isPresent()) {
            BlendingMode blendingMode = getBlendingMode();
            activeLayer.get().setBlendingMode(blendingMode, false, true, true);
        }
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

//        Layer layer = comp.getActiveLayer();
//        activeLayerChanged(layer);
    }

    @Override
    public void activeCompLayerCountChanged(Composition comp, int newLayerCount) {
    }

    @Override
    public void layerOrderChanged(Composition comp) {

    }

    @Override
    public void activeLayerChanged(Layer newActiveLayer) {
        float floatOpacity = newActiveLayer.getOpacity();
        int intOpacity = (int) (floatOpacity * 100);

        BlendingMode bm = newActiveLayer.getBlendingMode();
        try {
            userInteractionChange = false;
            opacityDDSlider.setValue(intOpacity);
            blendingModeCombo.setSelectedItem(bm);
        } finally {
            userInteractionChange = true;
        }
    }

    public void setBlendingModeNotUI(BlendingMode bm) {
        try {
            userInteractionChange = false;
            blendingModeCombo.setSelectedItem(bm);
        } finally {
            userInteractionChange = true;
        }
    }
}
