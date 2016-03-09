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

import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.UpdateGUI;

import static pixelitor.gui.ImageComponents.onActiveLayer;

/**
 * The GUI selector for the opacity and blending mode of the layers
 */
public class LayerBlendingModePanel extends BlendingModePanel implements ImageSwitchListener, GlobalLayerChangeListener {
    private boolean userInteractionChange = true;

    public static final LayerBlendingModePanel INSTANCE = new LayerBlendingModePanel();

    private LayerBlendingModePanel() {
        super(false);

        ImageComponents.addImageSwitchListener(this);
        AppLogic.addLayerChangeListener(this);

        opacityDDSlider.addActionListener(e -> {
            if (userInteractionChange) {
                opacityChanged();
            }
        });

        bmCombo.addActionListener(e -> {
            if (userInteractionChange) {
                blendingModeChanged();
            }
        });

        setEnabled(false);
    }

    private void opacityChanged() {
        onActiveLayer(layer -> {
            float floatValue = getOpacity();
            layer.setOpacity(floatValue, UpdateGUI.NO, AddToHistory.YES, true);
        });
    }

    private void blendingModeChanged() {
        onActiveLayer(layer -> {
            BlendingMode blendingMode = getBlendingMode();
            layer.setBlendingMode(blendingMode, UpdateGUI.NO, AddToHistory.YES, true);
        });
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
            bmCombo.setSelectedItem(bm);
        } finally {
            userInteractionChange = true;
        }
    }

    public void setBlendingModeFromModel(BlendingMode bm) {
        try {
            userInteractionChange = false;
            bmCombo.setSelectedItem(bm);
        } finally {
            userInteractionChange = true;
        }
    }

    public void setOpacityFromModel(float f) {
        try {
            userInteractionChange = false;
            int intValue = (int) (f * 100);
            opacityDDSlider.setValue(intValue);
        } finally {
            userInteractionChange = true;
        }
    }
}
