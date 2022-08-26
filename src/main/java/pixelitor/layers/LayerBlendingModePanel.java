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
import pixelitor.utils.Messages;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;

import static pixelitor.Views.onActiveLayer;

/**
 * The GUI selector for the opacity and blending mode of the layers
 */
public class LayerBlendingModePanel extends BlendingModePanel
    implements ViewActivationListener, ActiveLayerHolderListener {

    private boolean userInteractionChange = true;

    private static final LayerBlendingModePanel INSTANCE = new LayerBlendingModePanel();

    private boolean lastActiveWasGroup = false;

    private final ComboBoxModel<BlendingMode> groupModel;

    private LayerBlendingModePanel() {
        super(false);

        groupModel = new DefaultComboBoxModel<>(BlendingMode.ALL_MODES);

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

    public static LayerBlendingModePanel get() {
        return INSTANCE;
    }

    private void opacityChanged() {
        onActiveLayer(layer ->
            layer.setOpacity(getOpacity(), true, true));
    }

    private void blendingModeChanged() {
        onActiveLayer(layer ->
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
    public void layerActivated(Layer newActiveLayer) {
        float floatOpacity = newActiveLayer.getOpacity();
        int intOpacity = (int) (floatOpacity * 100);

        BlendingMode bm = newActiveLayer.getBlendingMode();
        try {
            userInteractionChange = false;
            opacityDDSlider.setValue(intOpacity);
            setBlendingMode(bm, newActiveLayer);
        } finally {
            userInteractionChange = true;
        }
    }

    public void blendingModeChangedForLayer(BlendingMode bm, Layer layer) {
        try {
            userInteractionChange = false;
            setBlendingMode(bm, layer);
        } catch (Exception ex) {
            Messages.showException(ex);
        } finally {
            userInteractionChange = true;
        }
    }

    @Override
    public void setBlendingMode(BlendingMode bm, Layer newLayer) {
        if (newLayer == null) {
            newLayer = Views.getActiveLayer();
        }

        if (newLayer instanceof LayerGroup) {
            if (!lastActiveWasGroup) {
                bmCombo.setModel(groupModel);
                lastActiveWasGroup = true;
            }
        } else { // not a layer group
            if (lastActiveWasGroup) {
                bmCombo.setModel(layerModel);
                lastActiveWasGroup = false;
            }
        }
        bmCombo.setSelectedItem(bm);
    }

    public void opacityChangedForLayer(float newOpacity) {
        try {
            userInteractionChange = false;
            int intValue = (int) (newOpacity * 100);
            opacityDDSlider.setValue(intValue);
        } finally {
            userInteractionChange = true;
        }
    }
}
