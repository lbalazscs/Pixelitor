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

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * The part of the GUI that manages the layers of an image.
 */
public class LayersContainer extends JPanel implements ImageSwitchListener {
    private LayersPanel layersPanel;
    private final JScrollPane scrollPane;

    public static final LayersContainer INSTANCE = new LayersContainer();

    private LayersContainer() {
        setLayout(new BorderLayout());

        add(LayerBlendingModePanel.INSTANCE, BorderLayout.NORTH);

        scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        JPanel southPanel = initSouthPanel();
        add(southPanel, BorderLayout.SOUTH);

        setBorder(BorderFactory.createTitledBorder("Layers"));

        ImageComponents.addImageSwitchListener(this);
    }

    private static JPanel initSouthPanel() {
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));

        southPanel.add(createButtonFromAction(AddNewLayerAction.INSTANCE, "addLayer"));
        southPanel.add(createButtonFromAction(DeleteActiveLayerAction.INSTANCE, "deleteLayer"));
        southPanel.add(createButtonFromAction(DuplicateLayerAction.INSTANCE, "duplicateLayer"));
        southPanel.add(createButtonFromAction(AddLayerMaskAction.INSTANCE, "addLayerMask"));
        southPanel.add(createButtonFromAction(AddTextLayerAction.INSTANCE, "addTextLayer"));

        if (Build.enableAdjLayers) {
            southPanel.add(createButtonFromAction(AddAdjLayerAction.INSTANCE, "addAdjLayer"));
        }

        return southPanel;
    }

    private static JButton createButtonFromAction(Action a, String name) {
        JButton button = new JButton(a);
        button.setHideActionText(true);
        button.setName(name);
        return button;
    }

    private void setLayersPanel(LayersPanel newLayersPanel) {
        if (layersPanel != null) {
            scrollPane.remove(layersPanel);
        }
        layersPanel = newLayersPanel;
        scrollPane.setViewportView(newLayersPanel);
    }

    @Override
    public void noOpenImageAnymore() {
        scrollPane.setViewportView(null);
    }

    @Override
    public void newImageOpened(Composition comp) {
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        // the layers pane of the imageComponent is set in
        // ImageComponent.onActivation()
    }

    public static boolean areLayersShown() {
        return (INSTANCE.getParent() != null);
    }

    /**
     * Each image has its own LayersPanel object, and when a new image is activated, this
     * method is called
     */
    public static void showLayersPanel(LayersPanel p) {
        INSTANCE.setLayersPanel(p);
    }
}

