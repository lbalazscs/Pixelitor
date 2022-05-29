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

import pixelitor.AppContext;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.io.DropListener;
import pixelitor.utils.ViewActivationListener;
import pixelitor.utils.VisibleForTesting;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

        JPanel southPanel = initSouthPanel();
        add(southPanel, SOUTH);

        setBorder(createTitledBorder(i18n("layers")));

        Views.addActivationListener(this);

        new DropTarget(this, new DropListener(NEW_LAYERS));
    }

    private static JPanel initSouthPanel() {
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new FlowLayout(LEFT, 2, 0));

        southPanel.add(new SouthButton(AddNewLayerAction.INSTANCE, "addLayer"));
        southPanel.add(new SouthButton(DeleteActiveLayerAction.INSTANCE, "deleteLayer"));
        southPanel.add(new SouthButton(DuplicateLayerAction.INSTANCE, "duplicateLayer"));
        southPanel.add(new SouthButton(AddLayerMaskAction.INSTANCE, "addLayerMask"));
        southPanel.add(new SouthButton(AddTextLayerAction.INSTANCE, "addTextLayer"));

        if (AppContext.enableAdjLayers) {
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

    public static LayersContainer get() {
        return INSTANCE;
    }

    public static boolean parentIs(JComponent parent) {
        return INSTANCE.getParent() == parent;
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

