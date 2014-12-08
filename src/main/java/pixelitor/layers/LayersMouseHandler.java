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

import javax.swing.event.MouseInputAdapter;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * The MouseListener and MouseMotionListener for the layer buttons for the drag-reordering
 */
public class LayersMouseHandler extends MouseInputAdapter {
    private static final Cursor MOVE_CURSOR = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    private static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();
    public static final int DRAG_X_INDENT = 10;
    private LayersPanel layersPanel;
    private int dragStartYInButton;
    private boolean dragging = false;

    public LayersMouseHandler(LayersPanel layersPanel) {
        this.layersPanel = layersPanel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        getLayerButtonFromEvent(e); // call is necessary for translating the mouse event
        dragStartYInButton = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        LayerButton layerButton = getLayerButtonFromEvent(e);

        // since the LayerButton is continuously relocated, e.getY() returns
        // the mouse relative to the last LayerButton position
        int newY = layerButton.getY() + e.getY() - dragStartYInButton;
        layerButton.setLocation(DRAG_X_INDENT, newY);

        layersPanel.updateDrag(layerButton, newY, !dragging);
        dragging = true;

        layerButton.setCursor(MOVE_CURSOR);
        layersPanel.doLayout();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragging) {
            LayerButton layerButton = getLayerButtonFromEvent(e);
            layerButton.setCursor(DEFAULT_CURSOR);
            layersPanel.dragFinished();
        }
        dragging = false;
    }

    /**
     * Returns the layer button for the mouse event and also translate
     * the coordinates of the argument into the layer button space
     */
    public LayerButton getLayerButtonFromEvent(MouseEvent e) {
        LayerButton layerButton = null;
        Component c = e.getComponent();
        // the source of the event must be either the layer button
        // or the textfield inside it
        if (c instanceof LayerNameEditor) {
            LayerNameEditor nameEditor = (LayerNameEditor) c;
            layerButton = nameEditor.getLayerButton();
            // translate into the LayerButton coordinate system
            e.translatePoint(nameEditor.getX(), nameEditor.getY());
        } else {
            layerButton = (LayerButton) c;
        }
        return layerButton;
    }
}
