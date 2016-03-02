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

import pixelitor.Composition;
import pixelitor.gui.ImageComponents;

import javax.swing.*;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The GUI container for LayerButton objects.
 * Each ImageComponent has its own LayersPanel instance.
 */
public class LayersPanel extends JLayeredPane {
    private final List<LayerButton> layerButtons = new ArrayList<>();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final DragReorderHandler dragReorderHandler;
    private LayerButton draggedButton = null;

    public LayersPanel() {
        dragReorderHandler = new DragReorderHandler(this);
    }

    public void addLayerButton(LayerButton button, int newLayerIndex) {
        buttonGroup.add(Objects.requireNonNull(button));
        layerButtons.add(newLayerIndex, button);

        add(button, JLayeredPane.DEFAULT_LAYER);

        // the new layer always becomes the selected layer
        button.setUserInteraction(false); // this selection should not go into history
        button.setSelected(true);
        button.setUserInteraction(true);

        revalidate();
        repaint();

        button.addDragReorderHandler(dragReorderHandler);
    }

    public void deleteLayerButton(LayerButton button) {
        buttonGroup.remove(button);
        layerButtons.remove(button);
        remove(button);
        revalidate();
        repaint();

        button.removeDragReorderHandler(dragReorderHandler);
    }

    public void changeLayerOrderInTheGUI(int oldIndex, int newIndex) {
        LayerButton button = layerButtons.remove(oldIndex);
        layerButtons.add(newIndex, button);

        revalidate();
    }

    /**
     * @param firstDragUpdate true if called for the first time during this drag
     */
    public void updateDrag(LayerButton newDraggedButton, int dragY, boolean firstDragUpdate) {
        assert newDraggedButton != null;

        if (firstDragUpdate) {
            // put it into the drag layer so that it is always visible
            // (removing and adding works on Java 7, but not on Java 8, setLayer is fine on both)
            setLayer(newDraggedButton, JLayeredPane.DRAG_LAYER);
            this.draggedButton = newDraggedButton;
        }
        swapIfNecessary(dragY);
    }

    /**
     * Override doLayout() so that when the whole window is resized, the
     * layer buttons are still laid out correctly
     */
    @Override
    public void doLayout() {
        int parentHeight = getHeight();

        // assumes that all buttons have the same height
        int buttonHeight = layerButtons.get(0).getPreferredSize().height;

        int numButtons = layerButtons.size();

        for (int i = 0; i < numButtons; i++) {
            LayerButton button = layerButtons.get(i);
            int y = parentHeight - (i + 1) * buttonHeight;
            if (button != draggedButton) {
                button.setSize(getWidth(), buttonHeight);
                button.setLocation(0, y);
            }
            button.setStaticY(y);
        }
    }

    /**
     * Change the order of buttons while dragging
     */
    private void swapIfNecessary(int dragY) {
        int staticY = draggedButton.getStaticY();
        int deltaY = dragY - staticY;
        int buttonHeight = draggedButton.getPreferredSize().height; // all buttons have the same height
        int halfHeight = buttonHeight / 2;
        if (deltaY > 0) {  // dragging downwards
            if (deltaY < halfHeight) {
                return;
            } else {
                int draggedIndex = layerButtons.indexOf(draggedButton);
                if (draggedIndex > 0) {
                    int indexBellow = draggedIndex - 1;
                    Collections.swap(layerButtons, indexBellow, draggedIndex);
                }
            }
        } else { // dragging upwards
            if (deltaY > -halfHeight) {
                return;
            } else {
                int draggedIndex = layerButtons.indexOf(draggedButton);
                if (draggedIndex < layerButtons.size() - 1) {
                    int indexAbove = draggedIndex + 1;
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

        // notify the raise/lower layer menu items
        Composition comp = ImageComponents.getActiveCompOrNull();
        LayerMoveAction.INSTANCE_UP.enableDisable(comp);
        LayerMoveAction.INSTANCE_DOWN.enableDisable(comp);
    }

    @Override
    public Dimension getPreferredSize() {
        // assumes that all buttons have the same height
        int buttonHeight = layerButtons.get(0).getPreferredSize().height;
        int numButtons = layerButtons.size();
        int allButtonsHeight = numButtons * buttonHeight;
        return new Dimension(10, allButtonsHeight);
    }
}
