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

import pixelitor.gui.utils.NamedAction;
import pixelitor.gui.utils.TaskAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Creates the popup menus for layer mask actions.
 */
public class LayerMaskActions {
    private LayerMaskActions() {
    }

    public static void addPopupMenu(JLabel label, Layer layer) {
        label.addMouseListener(new MaskPopupListener(layer));
    }

    private static class MaskPopupListener extends MouseAdapter {
        private final JPopupMenu menu;

        protected MaskPopupListener(Layer layer) {
            menu = new JPopupMenu();

            JMenu showMenu = new JMenu("Show/Edit");
            menu.add(showMenu);
            MaskViewMode.NORMAL.addToPopupMenu(showMenu, layer);
            MaskViewMode.VIEW_MASK.addToPopupMenu(showMenu, layer);
            MaskViewMode.EDIT_MASK.addToPopupMenu(showMenu, layer);
            MaskViewMode.RUBYLITH.addToPopupMenu(showMenu, layer);

            menu.addSeparator();

            // delete mask action
            menu.add(new TaskAction("Delete", () -> layer.deleteMask(true)));

            // masks can be applied only to image layers
            if (layer instanceof ImageLayer) {
                // apply mask action
                menu.add(new TaskAction("Apply", () -> {
                    ((ImageLayer) layer).applyLayerMask(true);
                    layer.update();
                }));
            }

            menu.add(new ToggleMaskEnabledAction(layer));

            // masks can be linked only to content layers
            if (layer instanceof ContentLayer) {
                menu.add(new ToggleMaskLinkedAction(layer));
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                LayerGUI.selectLayerIfIconClicked(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        private void showPopup(MouseEvent e) {
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Enables or disables a layer's layer mask.
     */
    static class ToggleMaskEnabledAction extends NamedAction implements LayerListener {
        private final Layer layer;

        public ToggleMaskEnabledAction(Layer layer) {
            super(calcText(layer));
            this.layer = layer;
            layer.addListener(this);
        }

        @Override
        protected void onClick(ActionEvent e) {
            layer.setMaskEnabled(!layer.isMaskEnabled(), true);
            refreshText();
        }

        private void refreshText() {
            setText(calcText(layer));
        }

        private static String calcText(Layer layer) {
            return layer.isMaskEnabled() ? "Disable" : "Enable";
        }

        @Override
        public void layerStateChanged(Layer changedLayer) {
            assert changedLayer == layer;
            refreshText();
        }
    }

    /**
     * Links or unlinks the movement of a layer and its layer mask.
     */
    static class ToggleMaskLinkedAction extends NamedAction implements LayerListener {
        private final Layer layer;

        public ToggleMaskLinkedAction(Layer layer) {
            super(calcText(layer));
            this.layer = layer;
            layer.getMask().addListener(this);
        }

        @Override
        protected void onClick(ActionEvent e) {
            LayerMask mask = layer.getMask();
            mask.setLinked(!mask.isLinked(), true);
            refreshText();
        }

        private void refreshText() {
            setText(calcText(layer));
        }

        private static String calcText(Layer layer) {
            assert layer.hasMask() : "name = " + layer.getName() + ", type = " + layer.getClass();
            return layer.getMask().isLinked() ? "Unlink" : "Link";
        }

        @Override
        public void layerStateChanged(Layer changedLayer) {
            assert changedLayer == layer.getMask();
            assert ((LayerMask) changedLayer).getOwner() == layer;

            refreshText();
        }
    }
}
