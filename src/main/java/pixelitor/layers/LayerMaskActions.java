/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LayerMaskActions {
    private LayerMaskActions() {
    }

    public static void addPopupMenu(JLabel label, Layer layer) {
        label.addMouseListener(new PopupMouseListener(layer));
    }

    private static class PopupMouseListener extends MouseAdapter {
        private final JPopupMenu menu;

        public PopupMouseListener(Layer layer) {
            menu = new JPopupMenu();

            JMenu showMenu = new JMenu("Show/Edit");
            menu.add(showMenu);
            MaskViewMode.NORMAL.addToPopupMenu(showMenu, layer);
            MaskViewMode.SHOW_MASK.addToPopupMenu(showMenu, layer);
            MaskViewMode.EDIT_MASK.addToPopupMenu(showMenu, layer);
            MaskViewMode.RUBYLITH.addToPopupMenu(showMenu, layer);

            menu.addSeparator();

            menu.add(new JMenuItem(new DeleteMaskAction(layer)));

            // masks can be applied only to image layers
            if (layer instanceof ImageLayer) {
                menu.add(new JMenuItem(new ApplyMaskAction(layer)));
            }

            menu.add(new JMenuItem(new EnableDisableMaskAction(layer)));

            // masks can be linked only to content layers
            if (layer instanceof ContentLayer) {
                menu.add(new JMenuItem(new LinkUnlinkMaskAction(layer)));
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup(e);
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                LayerButton.selectLayerIfIconClicked(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup(e);
            }
        }

        private void popup(MouseEvent e) {
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private static class DeleteMaskAction extends AbstractAction {
        private final Layer layer;

        public DeleteMaskAction(Layer layer) {
            super("Delete");
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            layer.deleteMask(true);
        }
    }

    private static class ApplyMaskAction extends AbstractAction {
        private final Layer layer;

        public ApplyMaskAction(Layer layer) {
            super("Apply");
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(layer instanceof ImageLayer)) {
                // actually we should never get here because the popup menu
                // is enabled only for image layers
                Messages.showNotImageLayerError(layer);
                return;
            }

            ((ImageLayer) layer).applyLayerMask(true);

            layer.getComp().imageChanged();
        }
    }

    static class EnableDisableMaskAction extends NamedAction implements LayerListener {
        private final Layer layer;

        public EnableDisableMaskAction(Layer layer) {
            super(calcText(layer));
            this.layer = layer;
            layer.addListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
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
        public void layerStateChanged(Layer layer) {
            assert layer == this.layer;
            refreshText();
        }
    }

    static class LinkUnlinkMaskAction extends NamedAction implements LayerListener {
        private final Layer layer;

        public LinkUnlinkMaskAction(Layer layer) {
            super(calcText(layer));
            this.layer = layer;
            layer.getMask().addListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
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
        public void layerStateChanged(Layer mask) {
            assert mask.getOwner() == this.layer : "this.name = " + this.layer.getName()
                + ", origin.name = " + mask.getOwner().getName()
                + ", this.class = " + this.layer.getClass().getSimpleName()
                + ", origin.class = " + mask.getOwner().getClass().getSimpleName();
            refreshText();
        }
    }
}
