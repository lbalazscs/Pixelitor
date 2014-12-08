/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The GUI container for LayerButton objects.
 * Each ImageComponent has its own LayersPanel instance.
 */
public class LayersPanel extends JLayeredPane {
    private List<LayerButton> layerButtons = new ArrayList<>();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final LayersMouseHandler mouseHandler;
    private LayerButton draggedButton = null;
    public LayersPanel() {
        mouseHandler = new LayersMouseHandler(this);
    }

    public void addLayerButton(LayerButton button, int newLayerIndex) {
        if (button == null) {
            throw new IllegalArgumentException("button is null");
        }

        buttonGroup.add(button);
        layerButtons.add(newLayerIndex, button);

        add(button, JLayeredPane.DEFAULT_LAYER);

        button.setUserInteraction(false);
        button.setSelected(true);
        button.setUserInteraction(true);

        revalidate();
        repaint();

        button.addMouseHandler(mouseHandler);
    }

    public void deleteLayerButton(LayerButton button) {
        buttonGroup.remove(button);
        layerButtons.remove(button);
        remove(button);
        revalidate();
        repaint();

        button.removeMouseHandler(mouseHandler);
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
        if (newDraggedButton == null) {
            throw new IllegalArgumentException("newDraggedButton is null");
        }

        if (firstDragUpdate) {
            // put it into the drag layer so that it is always visible
            remove(newDraggedButton);
            add(newDraggedButton, JLayeredPane.DRAG_LAYER);
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
        int buttonHeight = layerButtons.get(0).getPreferredSize().height; // all buttons have the same height
        for (int i = 0; i < layerButtons.size(); i++) {
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
     * Decides whether two layer buttons should be swapped in the layerButtons list
     * and swaps them if necessary
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
            remove(draggedButton);
            add(draggedButton, JLayeredPane.DEFAULT_LAYER);
        } else {
            throw new IllegalStateException();
        }
        draggedButton = null;
        doLayout();
    }
}
