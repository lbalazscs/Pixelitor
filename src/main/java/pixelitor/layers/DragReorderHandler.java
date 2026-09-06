/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Handles mouse events for drag-reordering layers in the {@link LayersPanel}.
 * Used both as MouseListener and as MouseMotionListener.
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
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        LayerGUI layerGUI = findTopLevelLayerGUI(e.getComponent());
        if (layerGUI != null) {
            Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), layersPanel);
            dragStartY = p.y - layerGUI.getY();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        LayerGUI enclosingGUI = findEnclosingLayerGUI(e.getComponent());
        if (enclosingGUI == null || enclosingGUI.isEditingName()) {
            return;
        }

        LayerGUI layerGUI = findTopLevelLayerGUI(enclosingGUI);
        if (layerGUI == null || layerGUI.isEditingName()) {
            return;
        }

        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), layersPanel);
        int newY = p.y - dragStartY;

        if (!dragging && Math.abs(newY - layerGUI.getY()) < 5) {
            // it seems that on Mac we get mouseDragged events even when the mouse isn't moved
            return;
        }

        layerGUI.setLocation(DRAG_X_OFFSET, newY);

        layersPanel.updateDrag(layerGUI, newY, !dragging);
        dragging = true;

        layerGUI.setCursor(Cursors.HAND);
        layersPanel.doLayout();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        LayerGUI layerGUI = findTopLevelLayerGUI(e.getComponent());
        if (dragging) {
            if (layerGUI != null) {
                layerGUI.setCursor(Cursors.DEFAULT);
            }
            layersPanel.dragFinished();
        } else {
            // activate the layer if the user clicks on the name field
            LayerGUI sourceGUI = findEnclosingLayerGUI(e.getComponent());
            if (sourceGUI != null) {
                sourceGUI.getLayer().activate();
            }
        }
        dragging = false;
    }

    private static LayerGUI findEnclosingLayerGUI(Component c) {
        while (c != null && !(c instanceof LayerGUI)) {
            c = c.getParent();
        }
        return (LayerGUI) c;
    }

    private static LayerGUI findTopLevelLayerGUI(Component c) {
        return findTopLevelLayerGUI(findEnclosingLayerGUI(c));
    }

    private static LayerGUI findTopLevelLayerGUI(LayerGUI gui) {
        while (gui != null && gui.isEmbedded()) {
            gui = gui.getParentUI();
        }
        return gui;
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
