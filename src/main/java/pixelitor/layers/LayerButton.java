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

import com.bric.util.JVM;
import pixelitor.AppContext;
import pixelitor.ThreadPool;
import pixelitor.gui.View;
import pixelitor.utils.Icons;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import static javax.swing.BorderFactory.*;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The selectable and draggable component representing
 * a layer in the "Layers" part of the GUI.
 */
public class LayerButton extends JToggleButton implements LayerUI {
    private static final Icon OPEN_EYE_ICON = Icons.load("eye_open.png");
    private static final Icon CLOSED_EYE_ICON = Icons.load("eye_closed.png");

    private static final String uiClassID = "LayerButtonUI";

    public static final Color UNSELECTED_COLOR = new Color(214, 217, 223);
    public static final Color SELECTED_COLOR = new Color(48, 76, 111);

    public static final int BORDER_WIDTH = 2;
    private DragReorderHandler dragReorderHandler;

    // Most often false, but when opening serialized
    // pxc files, the mask might be added before the drag handler
    // and in unit tests the drag handler is not added at all.
    private boolean maskAddedBeforeDragHandler;

    // for debugging only: each layer button has a different id
    private static int idCounter = 0;
    private final int id;

    /**
     * Represents the selection state of the layer and mask icons.
     */
    private enum SelectionState {
        /**
         * The layer is not the active layer.
         */
        UNSELECTED {
            @Override
            public void show(JLabel layerLabel, JLabel maskLabel) {
                layerLabel.setBorder(unselectedIconOnUnselectedLayerBorder);
                if (maskLabel != null) {
                    maskLabel.setBorder(unselectedIconOnUnselectedLayerBorder);
                }
            }
        },
        /**
         * The layer is active, but not in mask editing mode.
         */
        LAYER_SELECTED {
            @Override
            public void show(JLabel layerLabel, JLabel maskLabel) {
                layerLabel.setBorder(selectedBorder);
                if (maskLabel != null) {
                    maskLabel.setBorder(unselectedIconOnSelectedLayerBorder);
                }
            }
        },
        /**
         * The layer is active, and in mask editing mode.
         */
        MASK_SELECTED {
            @Override
            public void show(JLabel layerLabel, JLabel maskLabel) {
                layerLabel.setBorder(unselectedIconOnSelectedLayerBorder);
                if (maskLabel != null) {
                    maskLabel.setBorder(selectedBorder);
                }
            }
        };

        private static final Border lightBorder;

        static {
            if (JVM.isMac) {
                // seems to be a Mac-specific problem: with LineBorder,
                // a one pixel wide line disappears
                lightBorder = createMatteBorder(1, 1, 1, 1, UNSELECTED_COLOR);
            } else {
                lightBorder = createLineBorder(UNSELECTED_COLOR, 1);
            }
        }

        // used only in other borders
        private static final Border darkBorder
            = createLineBorder(SELECTED_COLOR, 1);

        // indicates the selection of a layer or mask icon
        private static final Border selectedBorder
            = createCompoundBorder(lightBorder, darkBorder);

        // the icon is unselected, but it is on a selected layer
        private static final Border unselectedIconOnSelectedLayerBorder
            = createLineBorder(SELECTED_COLOR, BORDER_WIDTH);

        // the icon is unselected, and it is on a unselected layer
        private static final Border unselectedIconOnUnselectedLayerBorder
            = createLineBorder(UNSELECTED_COLOR, BORDER_WIDTH);

        /**
         * Shows a selection state on a given layer and mask icon.
         * The mask argument can be null, if there is no mask.
         */
        public abstract void show(JLabel layerLabel, JLabel maskLabel);
    }

    private SelectionState selectionState;

    private Layer layer;
    private boolean userInteraction = true;

    private JCheckBox visibilityCB;
    private LayerNameEditor nameEditor;
    private JLabel layerIconLabel;
    private JLabel maskIconLabel;

    /**
     * The Y coordinate in the parent when it is not dragging
     */
    private int staticY;

    public LayerButton(Layer layer) {
        assert calledOnEDT() : threadInfo();
        assert !AppContext.isUnitTesting() : "Swing component in unit test";

        this.layer = layer;

        setLayout(new LayerButtonLayout(layer));

        initVisibilityControl();
        initLayerNameEditor();
        configureLayerIcon();

        if (layer.hasMask()) {
            addMaskIcon();
        }

        wireSelectionWithLayerActivation();
        id = idCounter++;
    }

    private void configureLayerIcon() {
        Icon icon = createLayerIcon(layer);
        layerIconLabel = new JLabel(icon);
        if (layer instanceof TextLayer) {
            layerIconLabel.setToolTipText("<html><b>Double-click</b> to edit the text layer.");
        }

        layerIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                layerIconClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                layerIconPressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    layerPopupTriggered(e);
                }
            }
        });

        layerIconLabel.setName("layerIcon");
        add(layerIconLabel, LayerButtonLayout.LAYER);
    }

    private static Icon createLayerIcon(Layer layer) {
        if (layer instanceof TextLayer) {
            return Icons.getTextLayerIcon();
        } else if (layer instanceof AdjustmentLayer) {
            return Icons.getAdjLayerIcon();
        } else {
            return null;
        }
    }

    private void layerIconClicked(MouseEvent e) {
        activateLayerNow();

        int clickCount = e.getClickCount();
        if (clickCount == 1) {
            MaskViewMode.NORMAL.activate(layer);
        } else {
            layer.edit();
        }
    }

    // called when one of the icons is clicked
    private void activateLayerNow() {
        // the layer would be activated anyway, but only in an invokeLayer,
        // and the mask activation expects events to be coming from the active layer
        layer.activate(true);
    }

    private void layerIconPressed(MouseEvent e) {
        // by putting it into mouse pressed, it is consistent
        // with the mask clicks
        if (SwingUtilities.isLeftMouseButton(e)) {
            selectLayerIfIconClicked(e);
        } else if (e.isPopupTrigger()) {
            layerPopupTriggered(e);
        }
    }

    private void layerPopupTriggered(MouseEvent e) {
        JPopupMenu popup = layer.createLayerIconPopupMenu();
        if (popup != null) {
            popup.show(this, e.getX(), e.getY());
        }
    }

    public static void selectLayerIfIconClicked(MouseEvent e) {
        // By adding a mouse listener to the JLabel, it loses the
        // ability to automatically transmit the mouse events to its
        // parent, and therefore the layer cannot be selected anymore
        // by left-clicking on this label. This is the workaround.
        JLabel source = (JLabel) e.getSource();
        LayerButton layerButton = (LayerButton) source.getParent();
        layerButton.setSelected(true);
    }

    private void initVisibilityControl() {
        visibilityCB = new JCheckBox(CLOSED_EYE_ICON);
        visibilityCB.setRolloverIcon(CLOSED_EYE_ICON);

        // when loading pxc files, the layer might not be visible
        visibilityCB.setSelected(layer.isVisible());

        visibilityCB.setToolTipText("<html><b>Click</b> to hide/show this layer.");
        visibilityCB.setSelectedIcon(OPEN_EYE_ICON);
        add(visibilityCB, LayerButtonLayout.CHECKBOX);

        visibilityCB.addItemListener(e ->
            layer.setVisible(visibilityCB.isSelected(), true));
    }

    private void initLayerNameEditor() {
        nameEditor = new LayerNameEditor(this);
        add(nameEditor, LayerButtonLayout.NAME_EDITOR);
        addPropertyChangeListener("name", evt -> updateName());
    }

    private void wireSelectionWithLayerActivation() {
        addItemListener(e -> {
            if (userInteraction) {
                // invoke later, when isSelected() returns the correct value
                EventQueue.invokeLater(this::buttonActivationChanged);
            }
        });
    }

    private void buttonActivationChanged() {
        if (isSelected()) {
            // the layer was just activated
            layer.activate(userInteraction);
        } else {
            // the layer was just deactivated
            nameEditor.disableEditing();
        }
        updateSelectionState();
    }

    @Override
    public void setOpenEye(boolean newVisibility) {
        visibilityCB.setSelected(newVisibility);
    }

    @Override
    public boolean isEyeOpen() {
        return visibilityCB.isSelected();
    }

    public void setUserInteraction(boolean userInteraction) {
        this.userInteraction = userInteraction;
    }

    public void addDragReorderHandler(DragReorderHandler handler) {
        dragReorderHandler = handler;
        handler.attachTo(this);
        handler.attachTo(nameEditor);
        handler.attachTo(layerIconLabel);

        if (maskAddedBeforeDragHandler) {
            assert hasMaskIcon() : "no mask in " + layer.getName();
            handler.attachTo(maskIconLabel);
        }
    }

    public void removeDragReorderHandler(DragReorderHandler handler) {
        handler.detachFrom(this);
        handler.detachFrom(nameEditor);
        handler.detachFrom(layerIconLabel);

        if (hasMaskIcon()) {
            handler.detachFrom(maskIconLabel);
        }
    }

    @Override
    public boolean hasMaskIcon() {
        return maskIconLabel != null;
    }

    public int getStaticY() {
        return staticY;
    }

    public void setStaticY(int staticY) {
        this.staticY = staticY;
    }

    public void dragFinished(int newLayerIndex) {
        layer.changeStackIndex(newLayerIndex);
    }

    public Layer getLayer() {
        return layer;
    }

    @Override
    public String getLayerName() {
        return layer.getName();
    }

    public boolean isNameEditing() {
        return nameEditor.isEditable();
    }

    @Override
    public void updateName() {
        nameEditor.setText(layer.getName());
    }

    @Override
    public void updateLayerIconImageAsync(Layer layer) {
        assert calledOnEDT() : threadInfo();
        if (layer.getClass() == TextLayer.class) { // TODO find a better way
            return;
        }

        Runnable notEDT = () -> {
            BufferedImage thumb = layer.createIconThumbnail();
            assert thumb != null;
            if (thumb != null) {
                SwingUtilities.invokeLater(() -> updateIconOnEDT(layer, thumb));
            }
        };
        ThreadPool.submit(notEDT);
    }

    private void updateIconOnEDT(Layer layer, BufferedImage thumb) {
        assert calledOnEDT() : threadInfo();
        if (layer instanceof LayerMask) {
            if (!hasMaskIcon()) {
                return;
            }
            boolean disabledMask = !((LayerMask) layer).getOwner().isMaskEnabled();
            if (disabledMask) {
                ImageUtils.paintRedXOn(thumb);
            }
            maskIconLabel.setIcon(new ImageIcon(thumb));
        } else {
            layerIconLabel.setIcon(new ImageIcon(thumb));
        }
        repaint();
    }

    @Override
    public void addMaskIcon() {
        assert !hasMaskIcon() : "layer '" + layer.getName() + "' already has mask icon";

        maskIconLabel = new JLabel("", null, CENTER);
        maskIconLabel.setToolTipText("<html>" +
                                     "<b>Click</b> activates mask editing,<br>" +
                                     "<b>Shift-click</b> disables/enables the mask,<br>" +
                                     "<b>Alt-click</b> toggles mask/layer view,<br>" +
                                     "<b>Shift-Alt-click</b> toggles rubylith/normal view,<br>" +
                                     "<b>Right-click</b> shows more options");

        LayerMaskActions.addPopupMenu(maskIconLabel, layer);
        maskIconLabel.setName("maskIcon");
        add(maskIconLabel, LayerButtonLayout.MASK);

        // there is another mouse listener for the right-click popups
        maskIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                maskIconClicked(e);
            }
        });

        if (dragReorderHandler != null) {
            dragReorderHandler.attachTo(maskIconLabel);
            maskAddedBeforeDragHandler = false;
        } else {
            maskAddedBeforeDragHandler = true;
        }

        layer.getMask().updateIconImage();
        revalidate();
    }

    private void maskIconClicked(MouseEvent e) {
        activateLayerNow();

        boolean altClick = e.isAltDown();
        boolean shiftClick = e.isShiftDown();

        if (altClick && shiftClick) {
            // shift-alt-click switches to RUBYLITH
            // except when it already is in RUBYLITH
            View view = layer.getComp().getView();
            if (view.getMaskViewMode() == MaskViewMode.RUBYLITH) {
                MaskViewMode.EDIT_MASK.activate(view, layer);
            } else {
                MaskViewMode.RUBYLITH.activate(view, layer);
            }
        } else if (altClick) {
            // alt-click switches to SHOW_MASK
            // except when it already is in SHOW_MASK
            View view = layer.getComp().getView();
            if (view.getMaskViewMode() == MaskViewMode.SHOW_MASK) {
                MaskViewMode.EDIT_MASK.activate(view, layer);
            } else {
                MaskViewMode.SHOW_MASK.activate(view, layer);
            }
        } else if (shiftClick) {
            // shift-click disables, except when it is already disabled
            layer.setMaskEnabled(!layer.isMaskEnabled(), true);
        } else {
            View view = layer.getComp().getView();

            // don't change SHOW_MASK into EDIT_MASK
            if (view.getMaskViewMode() == MaskViewMode.NORMAL) {
                MaskViewMode.EDIT_MASK.activate(layer);
            }
        }
    }

    @Override
    public void removeMaskIcon() {
        assert maskIconLabel != null;

        // the mask icon label is not going to be used again, remove all listeners
        if (dragReorderHandler != null) { // null in unit tests
            dragReorderHandler.detachFrom(maskIconLabel);
        }

        // remove the left-click and right-click mouse listeners
        MouseListener[] mouseListeners = maskIconLabel.getMouseListeners();
        for (MouseListener mouseListener : mouseListeners) {
            maskIconLabel.removeMouseListener(mouseListener);
        }

        remove(maskIconLabel);
        revalidate();
        repaint();
        maskIconLabel = null;

        maskAddedBeforeDragHandler = false;
    }

    @Override
    public void updateSelectionState() {
        SelectionState newSelectionState;

        if (!isSelected()) {
            newSelectionState = SelectionState.UNSELECTED;
        } else {
            if (layer.isMaskEditing()) {
                newSelectionState = SelectionState.MASK_SELECTED;
            } else {
                newSelectionState = SelectionState.LAYER_SELECTED;
            }
        }

        if (newSelectionState != selectionState) {
            selectionState = newSelectionState;
            selectionState.show(layerIconLabel, maskIconLabel);
        }
    }

    @Override
    public void changeLayer(Layer newLayer) {
        this.layer = newLayer;
        updateName();
        Icon icon = createLayerIcon(layer);
        layerIconLabel.setIcon(icon);
        updateLayerIconImageAsync(layer);

        if (maskIconLabel != null) {
            removeMaskIcon();
        }
        if (newLayer.hasMask()) {
            // the mask icon is re-added because listeners reference the old layer
            addMaskIcon();
        }
        selectionState.show(layerIconLabel, maskIconLabel);
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @Override
    public void updateUI() {
        setUI(new LayerButtonUI());
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "LayerButton{" +
               "name='" + getLayerName() + '\'' +
               "id='" + getId() + '\'' +
               "has mask icon: " + (hasMaskIcon() ? "YES" : "NO") +
               '}';
    }
}
