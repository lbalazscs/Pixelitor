package pixelitor.layers;

import pixelitor.FgBgColors;
import pixelitor.history.AddToHistory;
import pixelitor.menus.NamedAction;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static pixelitor.Composition.ImageChangeActions.FULL;

public class LayerMaskActions {
    private LayerMaskActions() {
    }

    public static void addPopupMenu(JComponent c, Layer layer) {
        c.addMouseListener(new PopupMouseListener(layer));
    }

    private static Layer getLayer(JPopupMenu menu) {
        Component invoker = menu.getInvoker();
        LayerButton layerButton = (LayerButton) invoker.getParent();
        Layer layer = layerButton.getLayer();
        return layer;
    }

    private static class PopupMouseListener extends MouseAdapter {
        private final JPopupMenu menu;

        public PopupMouseListener(Layer layer) {
            menu = new JPopupMenu();
            menu.add(new JMenuItem(new DeleteMaskAction(layer)));
            menu.add(new JMenuItem(new ApplyMaskAction(layer)));
            menu.add(new JMenuItem(new EnableDisableMaskAction(layer)));
            menu.add(new JMenuItem(new LinkUnlinkMaskAction(layer)));
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup(e);
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                // By adding a mouse listener to the JLabel, it loses the
                // ability to automatically transmit the mouse events to its
                // parent, and therefore the layer cannot be selected anymore
                // by left-clicking on this label. This is the workaround.
                JLabel source = (JLabel) e.getSource();
                LayerButton layerButton = (LayerButton) source.getParent();
                layerButton.setSelected(true);
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
            layer.deleteMask(AddToHistory.YES, true);
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
                Messages.showNotImageLayerError();
                return;
            }

            ((ImageLayer) layer).applyLayerMask(AddToHistory.YES);

            if (layer.isActive()) {
                layer.getComp().getIC().setShowLayerMask(false);
                FgBgColors.setLayerMaskEditing(false);
            }

            layer.getComp().imageChanged(FULL);
        }
    }

    static class EnableDisableMaskAction extends NamedAction implements LayerChangeListener {
        private final Layer layer;

        public EnableDisableMaskAction(Layer layer) {
            super(calcName(layer));
            this.layer = layer;
            layer.addLayerChangeObserver(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            layer.setMaskEnabled(!layer.isMaskEnabled(), AddToHistory.YES);
            refreshName();
        }

        private void refreshName() {
            setName(calcName(layer));
        }

        private static String calcName(Layer layer) {
            return layer.isMaskEnabled() ? "Disable" : "Enable";
        }

        @Override
        public void layerStateChanged() {
            refreshName();
        }
    }

    static class LinkUnlinkMaskAction extends NamedAction implements LayerChangeListener {
        private final Layer layer;

        public LinkUnlinkMaskAction(Layer layer) {
            super(calcName(layer));
            this.layer = layer;
            layer.getMask().addLayerChangeObserver(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LayerMask mask = layer.getMask();
            mask.setLinked(!mask.isLinked(), AddToHistory.YES);
            refreshName();
        }

        private void refreshName() {
            setName(calcName(layer));
        }

        private static String calcName(Layer layer) {
            return layer.getMask().isLinked() ? "Unlink" : "Link";
        }

        @Override
        public void layerStateChanged() {
            refreshName();
        }
    }
}
