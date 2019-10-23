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

import pixelitor.Build;
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.io.DropListener;
import pixelitor.utils.CompActivationListener;
import pixelitor.utils.VisibleForTesting;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.dnd.DropTarget;
import java.util.List;

import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.io.DropListener.Destination.NEW_LAYERS;

/**
 * The part of the GUI that manages the layers of a composition.
 */
public class LayersContainer extends JPanel implements CompActivationListener {
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

        setBorder(createTitledBorder("Layers"));

        OpenComps.addActivationListener(this);

        new DropTarget(this, new DropListener(NEW_LAYERS));
    }

    private static JPanel initSouthPanel() {
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        southPanel.add(new SouthButton(AddNewLayerAction.INSTANCE, "addLayer"));
        southPanel.add(new SouthButton(DeleteActiveLayerAction.INSTANCE, "deleteLayer"));
        southPanel.add(new SouthButton(DuplicateLayerAction.INSTANCE, "duplicateLayer"));
        southPanel.add(new SouthButton(AddLayerMaskAction.INSTANCE, "addLayerMask"));
        southPanel.add(new SouthButton(AddTextLayerAction.INSTANCE, "addTextLayer"));

        if (Build.enableAdjLayers) {
            southPanel.add(new SouthButton(AddAdjLayerAction.INSTANCE, "addAdjLayer"));
        }

        return southPanel;
    }

    private void setLayersPanel(LayersPanel newLayersPanel) {
        if (layersPanel == newLayersPanel) {
            return;
        }
        if (layersPanel != null) {
            scrollPane.remove(layersPanel);
        }
        layersPanel = newLayersPanel;
        scrollPane.setViewportView(newLayersPanel);
    }

    @Override
    public void allCompsClosed() {
        scrollPane.setViewportView(null);
    }

    @Override
    public void compActivated(View oldView, View newView) {
        // the layers pane of the CompositionView is set in
        // CompositionView.onActivation()
    }

    public static boolean areLayersShown() {
        return INSTANCE.getParent() != null;
    }

    public static void showLayersFor(View view) {
        INSTANCE.setLayersPanel(view.getLayersPanel());
    }

    @VisibleForTesting
    public int getNumLayerButtons() {
        return layersPanel.getNumLayerButtons();
    }

    @VisibleForTesting
    public List<String> getLayerNames() {
        return layersPanel.getLayerNames();
    }

    static class SouthButton extends JButton {
        private static final Dimension SIZE = new Dimension(44, 28);

        public SouthButton(Action a, String name) {
            super(a);
            setHideActionText(true);
            setName(name);
        }

        @Override
        public Dimension getMinimumSize() {
            return SIZE;
        }

        @Override
        public Dimension getPreferredSize() {
            return SIZE;
        }
    }
}

