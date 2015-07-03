package pixelitor.layers;

import pixelitor.history.AddToHistory;
import pixelitor.tools.FgBgColorSelector;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static pixelitor.Composition.ImageChangeActions.FULL;

public class LayerMaskActions {
    private LayerMaskActions() {
    }

    public static void configureWithPopupMenu(JComponent c) {
        c.addMouseListener(new PopupMouseListener());
    }

    private static class PopupMouseListener extends MouseAdapter {
        private final JPopupMenu menu;

        public PopupMouseListener() {
            menu = new JPopupMenu();
            menu.add(new JMenuItem(new DeleteAnyMaskAction(menu)));
            menu.add(new JMenuItem(new ApplyAnyMaskAction(menu)));
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

    /**
     * An action that can do something with a given mask,
     * not with just the mask of the active layer
     */
    private static abstract class AnyMaskAction extends AbstractAction {
        private final JPopupMenu menu;

        protected AnyMaskAction(String name, JPopupMenu menu) {
            super(name);
            this.menu = menu;
        }

        protected Layer getLayer() {
            Component invoker = menu.getInvoker();
            LayerButton layerButton = (LayerButton) invoker.getParent();
            Layer layer = layerButton.getLayer();
            return layer;
        }
    }

    private static class DeleteAnyMaskAction extends AnyMaskAction {
        public DeleteAnyMaskAction(JPopupMenu menu) {
            super("Delete", menu);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Layer layer = getLayer();
            layer.deleteMask(AddToHistory.YES);
            if (layer.isActive()) {
                layer.getComp().getIC().setShowLayerMask(false);
                FgBgColorSelector.INSTANCE.setLayerMaskEditing(false);
            }
        }
    }

    private static class ApplyAnyMaskAction extends AnyMaskAction {
        public ApplyAnyMaskAction(JPopupMenu menu) {
            super("Apply", menu);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Layer layer = getLayer();
            if (!(layer instanceof ImageLayer)) {
                Dialogs.showNotImageLayerDialog();
                return;
            }

            ((ImageLayer) layer).applyLayerMask(AddToHistory.YES);

            if (layer.isActive()) {
                layer.getComp().getIC().setShowLayerMask(false);
                FgBgColorSelector.INSTANCE.setLayerMaskEditing(false);
            }

            layer.getComp().imageChanged(FULL);
        }
    }
}
