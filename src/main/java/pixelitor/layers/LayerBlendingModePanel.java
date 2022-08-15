/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Views;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.View;
import pixelitor.utils.ViewActivationListener;

import static pixelitor.Views.onEditingTarget;

/**
 * The GUI selector for the opacity and blending mode of the layers
 */
public class LayerBlendingModePanel extends BlendingModePanel
    implements ViewActivationListener, ActiveLayerHolderListener {

    private boolean userInteractionChange = true;

    private static final LayerBlendingModePanel INSTANCE = new LayerBlendingModePanel();

    public static LayerBlendingModePanel get() {
        return INSTANCE;
    }

    private LayerBlendingModePanel() {
        super(false);

        opacityDDSlider.setName("layerOpacity");
        bmCombo.setName("layerBM");

        Views.addActivationListener(this);
        Layers.addLayerHolderListener(this);

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
        onEditingTarget(layer ->
            layer.setOpacity(getOpacity(), true, true));
    }

    private void blendingModeChanged() {
        onEditingTarget(layer ->
            layer.setBlendingMode(getBlendingMode(), true, true));
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        setEnabled(true);
    }

    @Override
    public void numLayersChanged(LayerHolder layerHolder, int newLayerCount) {
    }

    @Override
    public void layerOrderChanged(LayerHolder layerHolder) {
    }

    @Override
    public void layerTargeted(Layer newEditingTarget) {
        float floatOpacity = newEditingTarget.getOpacity();
        int intOpacity = (int) (floatOpacity * 100);

        BlendingMode bm = newEditingTarget.getBlendingMode();
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
