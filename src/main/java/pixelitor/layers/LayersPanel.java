/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.View;

import javax.swing.*;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * The GUI container for {@link LayerGUI} objects.
 * Each {@link View} has its own {@link LayersPanel} instance.
 */
public class LayersPanel extends JLayeredPane {
    private final List<LayerGUI> layerButtons = new ArrayList<>();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final DragReorderHandler dragReorderHandler;
    private LayerGUI draggedButton;

    public LayersPanel() {
        dragReorderHandler = new DragReorderHandler(this);
    }

    /**
     * Adds a single layer button at the given index.
     */
    public void addLayerGUI(LayerGUI gui, int index) {
        addLayerGUIImpl(gui, index);

        // finalize the single layer
        gui.updateSelectionState();
        gui.setReactToItemEvents(true);
        
        revalidate();
        repaint();
    }

    /**
     * Batch addition method for all layers of a composition
     * that revalidates only once at the end.
     */
    public void addLayerGUIs(List<LayerGUI> guis) {
        assert layerButtons.isEmpty();

        for (int i = 0; i < guis.size(); i++) {
            addLayerGUIImpl(guis.get(i), i);
        }

        // finalize all layers after the batch addition
        for (LayerGUI gui : guis) {
            gui.updateSelectionState();
            gui.setReactToItemEvents(true);
        }

        revalidate();
        repaint();
    }

    private void addLayerGUIImpl(LayerGUI gui, int index) {
        assert gui != null;

        gui.setReactToItemEvents(false);

        buttonGroup.add(gui);
        layerButtons.add(index, gui);

        add(gui, JLayeredPane.DEFAULT_LAYER);

        // the new layer becomes selected in the button group, but
        // item listeners won't fire yet because events are disabled
        gui.setSelected(true);

        gui.attachDragHandler(dragReorderHandler);
    }

    public void removeLayerGUI(LayerGUI gui) {
        buttonGroup.remove(gui);
        layerButtons.remove(gui);
        remove(gui);
        revalidate();
        repaint();

        gui.detach();
    }

    public void reorderLayer(int oldIndex, int newIndex) {
        LayerGUI layerGUI = layerButtons.remove(oldIndex);
        layerButtons.add(newIndex, layerGUI);

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
            draggedButton = newDraggedGUI;
        }
        swapIfNecessary(dragY);
    }

    /**
     * Override doLayout() so that when the whole window is
     * resized, the layer GUIs are still laid out correctly
     */
    @Override
    public void doLayout() {
        // layout components from bottom to top
        int y = getHeight();
        for (LayerGUI layerButton : layerButtons) {
            int buttonHeight = layerButton.getPreferredHeight();
            y -= buttonHeight;
            if (layerButton != draggedButton) {
                layerButton.setSize(getWidth(), buttonHeight);
                layerButton.setLocation(0, y);
            }
            layerButton.setLayoutY(y);
        }
    }

    /**
     * Change the order of layer GUIs while dragging
     */
    private void swapIfNecessary(int dragY) {
        int deltaY = dragY - draggedButton.getLayoutY();
        int draggedIndex = layerButtons.indexOf(draggedButton);
        if (deltaY > 0) {  // dragging downwards
            int indexBelow = draggedIndex - 1;
            if (indexBelow < 0) {
                return;
            }
            int swapDistance = layerButtons.get(indexBelow).getPreferredHeight() / 2;
            if (deltaY >= swapDistance) {
                if (draggedIndex > 0) {
                    Collections.swap(layerButtons, indexBelow, draggedIndex);
                }
            }
        } else { // dragging upwards
            int indexAbove = draggedIndex + 1;
            if (indexAbove >= layerButtons.size()) {
                return;
            }
            int swapDistance = layerButtons.get(indexAbove).getPreferredHeight() / 2;
            if (deltaY <= -swapDistance) {
                if (draggedIndex < layerButtons.size() - 1) {
                    Collections.swap(layerButtons, indexAbove, draggedIndex);
                }
            }
        }
    }

    // drag finished, put the last dragged back to the default JLayeredPane layer
    public void dragFinished() {
        if (draggedButton != null) {
            setLayer(draggedButton, JLayeredPane.DEFAULT_LAYER);
            draggedButton.dragFinished(layerButtons.indexOf(draggedButton)); // notify the composition
        } else {
            throw new IllegalStateException();
        }
        draggedButton = null;
        doLayout();
    }

    @Override
    public Dimension getPreferredSize() {
        int totalHeight = 0;
        for (LayerGUI gui : layerButtons) {
            totalHeight += gui.getPreferredHeight();
        }
        return new Dimension(10, totalHeight);
    }

    public int getNumLayerGUIs() {
        return layerButtons.size();
    }

    public List<String> getLayerNames() {
        return layerButtons.stream()
            .map(LayerGUI::getLayerName)
            .collect(toList());
    }

    public boolean containsGUI(LayerGUI gui) {
        return layerButtons.contains(gui);
    }

    public void updateThumbSize(int newThumbSize) {
        for (LayerGUI gui : layerButtons) {
            gui.updateThumbSize(newThumbSize);
        }
    }
}
