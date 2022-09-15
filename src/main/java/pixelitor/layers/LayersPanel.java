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
import pixelitor.gui.View;
import pixelitor.utils.VisibleForTesting;

import javax.swing.*;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static pixelitor.layers.LayerMoveAction.MOVE_LAYER_DOWN;
import static pixelitor.layers.LayerMoveAction.MOVE_LAYER_UP;

/**
 * The GUI container for {@link LayerGUI} objects.
 * Each {@link View} has its own {@link LayersPanel} instance.
 */
public class LayersPanel extends JLayeredPane {
    private final List<LayerGUI> layerGUIs = new ArrayList<>();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final DragReorderHandler dragReorderHandler;
    private LayerGUI draggedGUI;

    public LayersPanel() {
        dragReorderHandler = new DragReorderHandler(this);
    }

    public void addLayerGUI(LayerGUI gui, int index) {
        assert gui != null;

        buttonGroup.add(gui);
        layerGUIs.add(index, gui);

        add(gui, JLayeredPane.DEFAULT_LAYER);

        // the new layer always becomes the selected layer
        gui.setUserInteraction(false); // this selection should not go into history
        gui.setSelected(true);
        gui.setUserInteraction(true);

        revalidate();
        repaint();

        gui.addDragReorderHandler(dragReorderHandler);
    }

    public void removeLayerGUI(LayerGUI gui) {
        buttonGroup.remove(gui);
        layerGUIs.remove(gui);
        remove(gui);
        revalidate();
        repaint();

        gui.removeDragReorderHandler(dragReorderHandler);
    }

    public void changeLayerGUIOrder(int oldIndex, int newIndex) {
        LayerGUI layerGUI = layerGUIs.remove(oldIndex);
        layerGUIs.add(newIndex, layerGUI);

        revalidate();
    }

    /**
     * @param firstUpdate true if called for the first time during this drag
     */
    public void updateDrag(LayerGUI newDraggedGUI, int dragY, boolean firstUpdate) {
        assert newDraggedGUI != null;

        if (firstUpdate) {
            // put it into the drag layer so that it is always visible
            setLayer(newDraggedGUI, JLayeredPane.DRAG_LAYER);
            draggedGUI = newDraggedGUI;
        }
        swapIfNecessary(dragY);
    }

    /**
     * Override doLayout() so that when the whole window is
     * resized, the layer GUIs are still laid out correctly
     */
    @Override
    public void doLayout() {
        int parentHeight = getHeight();
        int y = parentHeight;
        for (LayerGUI layerGUI : layerGUIs) {
            int guiHeight = layerGUI.getPreferredHeight();
            y -= guiHeight;
            if (layerGUI != draggedGUI) {
                layerGUI.setSize(getWidth(), guiHeight);
                layerGUI.setLocation(0, y);
            }
            layerGUI.setStaticY(y);
        }
    }

    /**
     * Change the order of layer GUIs while dragging
     */
    private void swapIfNecessary(int dragY) {
        int staticY = draggedGUI.getStaticY();
        int deltaY = dragY - staticY;
        int draggedIndex = layerGUIs.indexOf(draggedGUI);
        if (deltaY > 0) {  // dragging downwards
            int indexBellow = draggedIndex - 1;
            if (indexBellow < 0) {
                return;
            }
            int swapDistance = layerGUIs.get(indexBellow).getPreferredHeight() / 2;
            if (deltaY >= swapDistance) {
                if (draggedIndex > 0) {
                    Collections.swap(layerGUIs, indexBellow, draggedIndex);
                }
            }
        } else { // dragging upwards
            int indexAbove = draggedIndex + 1;
            if (indexAbove >= layerGUIs.size()) {
                return;
            }
            int swapDistance = layerGUIs.get(indexAbove).getPreferredHeight() / 2;
            if (deltaY <= -swapDistance) {
                if (draggedIndex < layerGUIs.size() - 1) {
                    Collections.swap(layerGUIs, indexAbove, draggedIndex);
                }
            }
        }
    }

    // drag finished, put the last dragged back to the default JLayeredPane layer
    public void dragFinished() {
        if (draggedGUI != null) {
            setLayer(draggedGUI, JLayeredPane.DEFAULT_LAYER);
            draggedGUI.dragFinished(layerGUIs.indexOf(draggedGUI)); // notify the composition
        } else {
            throw new IllegalStateException();
        }
        draggedGUI = null;
        doLayout();

        // notify the raise/lower layer menu items
        var comp = Views.getActiveComp();
        MOVE_LAYER_UP.enableDisable(comp);
        MOVE_LAYER_DOWN.enableDisable(comp);
    }

    @Override
    public Dimension getPreferredSize() {
        int totalHeight = 0;
        for (LayerGUI gui : layerGUIs) {
            totalHeight += gui.getPreferredHeight();
        }
        return new Dimension(10, totalHeight);
    }

    @VisibleForTesting
    public int getNumLayerGUIs() {
        return layerGUIs.size();
    }

    @VisibleForTesting
    public List<String> getLayerNames() {
        return layerGUIs.stream()
            .map(LayerGUI::getLayerName)
            .collect(toList());
    }

    public boolean containsGUI(LayerGUI gui) {
        return layerGUIs.contains(gui);
    }

    public void thumbSizeChanged(int newThumbSize) {
        for (LayerGUI gui : layerGUIs) {
            gui.thumbSizeChanged(newThumbSize);
        }
    }
}
