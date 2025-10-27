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

import pixelitor.utils.Cursors;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.Component;
import java.awt.event.MouseEvent;

/**
 * Handles mouse events for drag-reordering layers in the {@link LayersPanel}.
 * Used booth as MouseListener and as MouseMotionListener.
 */
public class DragReorderHandler extends MouseInputAdapter {
    // horizontal offset while dragging
    private static final int DRAG_X_OFFSET = 10;

    private final LayersPanel layersPanel;
    private boolean dragging = false;

    // the y coordinate within a LayerGUI where the drag started
    private int dragStartY;

    public DragReorderHandler(LayersPanel layersPanel) {
        this.layersPanel = layersPanel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // double-click enables editing on a LayerNameEditor
        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            Component c = e.getComponent();
            if (c instanceof LayerNameEditor editor) {
                editor.enableEditing();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        translateMouseEvent(e);
        dragStartY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        LayerGUI layerGUI = translateMouseEvent(e);
        if (!dragging && Math.abs(dragStartY - e.getY()) < 5) {
            // it seems that on Mac we get mouseDragged events even when the mouse isn't moved
            return;
        }
        if (layerGUI.isNameEditing()) {
            return;
        }

        // Calculate the new LayerGUI y position.
        // Since the LayerGUI is continuously relocated, e.getY()
        // returns the mouse relative to the last LayerGUI position
        int newY = layerGUI.getY() + e.getY() - dragStartY;
        layerGUI.setLocation(DRAG_X_OFFSET, newY);

        layersPanel.updateDrag(layerGUI, newY, !dragging);
        dragging = true;

        layerGUI.setCursor(Cursors.HAND);
        layersPanel.doLayout();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        LayerGUI layerGUI = translateMouseEvent(e);
        if (dragging) {
            layerGUI.setCursor(Cursors.DEFAULT);
            layersPanel.dragFinished();
        } else {
            // activate the layer if the user clicks on the name field
            getRealLayerGUI(e).getLayer().activate();
        }
        dragging = false;
    }

    /**
     * Translates the mouse event coordinates into the LayerGUI's
     * coordinate system, and returns the corresponding LayerGUI.
     */
    private static LayerGUI translateMouseEvent(MouseEvent e) {
        LayerGUI layerGUI;
        Component c = e.getComponent();

        // determine the source of the event and translate coordinates accordingly
        if (c instanceof LayerNameEditor nameEditor) {
            layerGUI = nameEditor.getLayerGUI();
            // translate into the LayerGUI coordinate system
            e.translatePoint(nameEditor.getX(), nameEditor.getY());
        } else if (c instanceof JLabel) {
            layerGUI = (LayerGUI) c.getParent();
            e.translatePoint(c.getX(), c.getY());
        } else {
            layerGUI = (LayerGUI) c;
        }

        // ensure that mouse drags on embedded LayerGUIs
        // move the entire top parent
        while (layerGUI.isEmbedded()) {
            e.translatePoint(layerGUI.getX(), layerGUI.getY());
            layerGUI = layerGUI.getParentUI();
        }

        return layerGUI;
    }

    /**
     * Returns the real LayerGUI, without going up in the hierarchy.
     */
    private static LayerGUI getRealLayerGUI(MouseEvent e) {
        Component c = e.getComponent();
        if (c instanceof LayerNameEditor nameEditor) {
            return nameEditor.getLayerGUI();
        } else if (c instanceof JLabel) {
            return (LayerGUI) c.getParent();
        } else {
            return (LayerGUI) c;
        }
    }

    public void attachTo(JComponent c) {
        c.addMouseListener(this);
        c.addMouseMotionListener(this);
    }

    public void detachFrom(JComponent c) {
        c.removeMouseListener(this);
        c.removeMouseMotionListener(this);
    }
}
