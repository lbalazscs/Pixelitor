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

import pixelitor.utils.Cursors;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.Component;
import java.awt.event.MouseEvent;

/**
 * The MouseListener and MouseMotionListener for the layer GUIs for the drag-reordering
 */
public class DragReorderHandler extends MouseInputAdapter {
    private static final int DRAG_X_INDENT = 10;
    private final LayersPanel layersPanel;
    private int dragStartYInLayerGUI;
    private boolean dragging = false;
    private long lastNameEditorPressedMillis;

    public DragReorderHandler(LayersPanel layersPanel) {
        this.layersPanel = layersPanel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // a manual double-click watch - necessary on Mac?
        Component c = e.getComponent();
        if (c instanceof LayerNameEditor editor) {
            long when = e.getWhen();
            long diffMillis = when - lastNameEditorPressedMillis;
            if (diffMillis < 250) {
                editor.enableEditing();
            }
            lastNameEditorPressedMillis = when;
        }

        layerGUIForEvent(e); // the call is necessary for translating the mouse event
        dragStartYInLayerGUI = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        LayerGUI layerGUI = layerGUIForEvent(e);
        if (!dragging && Math.abs(dragStartYInLayerGUI - e.getY()) < 5) {
            // it seems that on Mac we get mouseDragged events even when the mouse is not moved
            return;
        }
        if (layerGUI.isNameEditing()) {
            return;
        }

        // since the LayerGUI is continuously relocated, e.getY()
        // returns the mouse relative to the last LayerGUI position
        int newY = layerGUI.getY() + e.getY() - dragStartYInLayerGUI;
        layerGUI.setLocation(DRAG_X_INDENT, newY);

//        assert layersPanel.containsGUI(layerGUI) : layerGUI.getLayer().getName() + " GUI not contained";
        layersPanel.updateDrag(layerGUI, newY, !dragging);
        dragging = true;

        layerGUI.setCursor(Cursors.HAND);
        layersPanel.doLayout();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        LayerGUI layerGUI = layerGUIForEvent(e);
        if (dragging) {
            layerGUI.setCursor(Cursors.DEFAULT);
            layersPanel.dragFinished();
        } else {
            // select the layer if the user clicks on the name field
            realLayerGUIForEvent(e).getLayer().activate();
        }
        dragging = false;
    }

    /**
     * Returns the layer GUI for the mouse event and also translate
     * the coordinates of the argument into the layer GUI's space
     */
    private static LayerGUI layerGUIForEvent(MouseEvent e) {
        LayerGUI layerGUI;
        Component c = e.getComponent();
        // the source of the event must be either the
        // layer GUI or the textfield inside it
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

        // ensure that mouse drags on smart filter
        // guis move the whole smart object
        while (layerGUI.isEmbedded()) {
            e.translatePoint(layerGUI.getX(), layerGUI.getY());
            layerGUI = layerGUI.getOwner();
        }

        return layerGUI;
    }

    /**
     * Returns the real LayerGUI, without going up in the hierarchy
     */
    private static LayerGUI realLayerGUIForEvent(MouseEvent e) {
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
