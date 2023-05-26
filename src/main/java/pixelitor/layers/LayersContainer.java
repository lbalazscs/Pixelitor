/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.GUIMode;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.io.DropListener;
import pixelitor.utils.ViewActivationListener;
import pixelitor.utils.VisibleForTesting;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.util.List;

import static java.awt.BorderLayout.*;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.io.DropListener.Destination.NEW_LAYERS;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * The part of the GUI that manages the layers of a composition.
 */
public class LayersContainer extends JPanel implements ViewActivationListener {
    private LayersPanel layersPanel;
    private final JScrollPane scrollPane;

    private static final LayersContainer INSTANCE = new LayersContainer();

    private LayersContainer() {
        super(new BorderLayout());

        assert calledOnEDT();

        add(LayerBlendingModePanel.get(), NORTH);

        scrollPane = new JScrollPane();
        add(scrollPane, CENTER);

        JPanel southPanel = createSouthPanel();
        add(southPanel, SOUTH);

        setBorder(createTitledBorder(i18n("layers")));

        Views.addActivationListener(this);

        new DropTarget(this, new DropListener(NEW_LAYERS));
    }

    private static JPanel createSouthPanel() {
        JPanel southPanel = new JPanel(new FlowLayout(LEFT, 2, 0));

        southPanel.add(new LayerActionButton(AddNewLayerAction.INSTANCE, "addLayer"));
        southPanel.add(new LayerActionButton(DeleteActiveLayerAction.INSTANCE, "deleteLayer"));
        southPanel.add(new LayerActionButton(DuplicateLayerAction.INSTANCE, "duplicateLayer"));
        southPanel.add(new LayerActionButton(AddLayerMaskAction.INSTANCE, "addLayerMask"));
        southPanel.add(new LayerActionButton(AddTextLayerAction.INSTANCE, "addTextLayer"));

        if (GUIMode.enableExperimentalFeatures) {
            southPanel.add(new LayerActionButton(AddAdjLayerAction.INSTANCE, "addAdjLayer"));
        }

        return southPanel;
    }

    private void changeLayersPanel(LayersPanel newLayersPanel) {
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
    public void allViewsClosed() {
        scrollPane.setViewportView(null);
    }

    /**
     * Not used. The {@link LayersPanel} of the View is set in {@link  View#showLayersUI}
     */
    @Override
    public void viewActivated(View oldView, View newView) {
    }

    public static boolean areLayersShown() {
        return INSTANCE.getParent() != null;
    }

    public static void showLayersOf(View view) {
        INSTANCE.changeLayersPanel(view.getLayersPanel());
    }

    @VisibleForTesting
    public int getNumLayerGUIs() {
        return layersPanel.getNumLayerGUIs();
    }

    @VisibleForTesting
    public List<String> getLayerNames() {
        return layersPanel.getLayerNames();
    }

    public static LayersContainer get() {
        return INSTANCE;
    }

    public static boolean parentIs(JComponent parent) {
        return INSTANCE.getParent() == parent;
    }

    static class LayerActionButton extends JButton {
        private static final Dimension SIZE = new Dimension(44, 28);

        public LayerActionButton(Action a, String name) {
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

