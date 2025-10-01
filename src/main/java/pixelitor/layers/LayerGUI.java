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

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.AppMode;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.Themes;
import pixelitor.utils.Icons;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * The selectable and draggable component representing
 * a layer in the "Layers" part of the GUI.
 */
public class LayerGUI extends JToggleButton implements LayerUI {
    public static final int BORDER_WIDTH = 2;

    private static final Icon OPEN_EYE_ICON = Icons.load("eye_open.png", "eye_open_dark.png");
    private static final Icon CLOSED_EYE_ICON = Icons.load("eye_closed.png", "eye_closed_dark.png");

    public static final Color UNSELECTED_COLOR = new Color(214, 217, 223);
    public static final Color SELECTED_COLOR = new Color(48, 76, 111);
    private static final Color SELECTED_DARK_COLOR = new Color(16, 16, 16);
    private static final Color SEMI_SELECTED_COLOR = new Color(131, 146, 167);
    private static final Color SEMI_SELECTED_DARK_COLOR = new Color(38, 39, 40);

    private SelectionState selectionState;

    // this field can't be called parent, because the setters/getters
    // would conflict with the methods in java.awt.Component
    private LayerGUI parentUI;

    private final List<LayerGUI> children = new ArrayList<>();
    private JPanel childrenPanel;

    // for debugging only: each layer GUI has a different id
    private static int idCounter = 0;
    private final int uniqueId;

    private Layer layer;
    private final LayerGUILayout layout;
    private boolean reactToItemEvents = true;

    private JCheckBox visibilityCB;
    private LayerNameEditor nameEditor;
    private JLabel layerIconLabel;
    private JLabel maskIconLabel;

    private DragReorderHandler dragReorderHandler;

    // most often false, but when opening serialized pxc files,
    // the mask/smart filter label might be added before the drag handler,
    // and in unit tests the drag handler isn't added at all
    private boolean delayedDragHandler;

    /**
     * The Y coordinate in the parent when it is not dragged.
     */
    private int staticY;

    public LayerGUI(Layer layer) {
        assert calledOnEDT() : callInfo();
        assert !AppMode.isUnitTesting() : "Swing component in unit test";

        this.layer = layer;

        layout = new LayerGUILayout(layer);
        setLayout(layout);

        initLayerVisibilityCB();
        initLayerNameEditor();
        updateChildrenPanel();

        configureLayerIcon();

        if (layer.hasMask()) {
            addMaskIcon();
        }

        bindSelectionToLayerActivation();
        uniqueId = idCounter++;
    }

    @Override
    public void updateChildrenPanel() {
        if (!(layer instanceof CompositeLayer holder)) {
            if (childrenPanel != null) {
                // Only composite layers should have a children panel.
                // However, after holder rasterization we could
                // have one for the resulting image layer, so remove it.
                remove(childrenPanel);
                childrenPanel = null;
                children.clear();
            }
            return;
        }
        int numChildLayers = holder.getNumLayers();
        if (numChildLayers > 0) {
            // Ensure that we have an empty children panel.
            // The child GUIs will be added later.
            if (childrenPanel == null) {
                childrenPanel = new JPanel(new VerticalLayout());
            } else {
                childrenPanel.removeAll();
            }
        } else { // a holder with zero children
            if (childrenPanel != null) {
                // all children have been removed
                remove(childrenPanel);
                childrenPanel = null;
            }
        }

        // TODO it's not elegant to detach all child GUIs and
        //   then reattach those that weren't changed.
        for (LayerGUI child : children) {
            // Detach the child UI only if this component is still its parent.
            // This prevents detaching a UI that has been re-parented by another
            // component during an operation like undo/redo of grouping.
            if (child.getParentUI() == this) {
                child.detach();
            }
        }
        children.clear();

        // children are added from the bottom up
        for (int i = numChildLayers - 1; i >= 0; i--) {
            Layer child = holder.getLayer(i);
            LayerGUI childUI = (LayerGUI) child.createUI();
            childUI.setParentUI(this);
            children.add(childUI);

            // when duplicating a smart object with filters
            // this is null, and it's only set later
            if (dragReorderHandler != null) {
                childUI.setDragReorderHandler(dragReorderHandler);
            }
            childrenPanel.add(childUI);
        }
        if (numChildLayers > 0) {
            add(childrenPanel, LayerGUILayout.CHILDREN);
        }
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
                if (e.isPopupTrigger()) {
                    layerPopupTriggered(e);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    // by putting it into mouse pressed, it is
                    // consistent with the mask clicks
                    selectLayerIfIconClicked(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    layerPopupTriggered(e);
                }
            }
        });

        layerIconLabel.setName("layerIcon");
        add(layerIconLabel, LayerGUILayout.LAYER);
    }

    private static Icon createLayerIcon(Layer layer) {
        if (layer instanceof TextLayer) {
            return Icons.getTextLayerIcon();
        } else if (layer.getClass() == AdjustmentLayer.class) {
            return Icons.getAdjLayerIcon();
        } else if (layer.getClass() == SmartFilter.class) {
            return Icons.getSmartFilterIcon();
        } else {
            // for other layer types, the icon depends on the contents
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
        // the layer would be activated anyway, but only in an invokeLater,
        // and the mask activation expects events to be coming from the active layer
        layer.activate();
    }

    private void layerPopupTriggered(MouseEvent e) {
        JPopupMenu popup = layer.createLayerIconPopupMenu();
        if (popup != null) {
            if (AppMode.isDevelopment()) {
                popup.addSeparator();
                popup.add(new TaskAction("Internal State...", () ->
                    Debug.showTree(layer, layer.getTypeString())));
            }

            popup.show(this, e.getX(), e.getY());
        }
    }

    public static void selectLayerIfIconClicked(MouseEvent e) {
        // By adding a mouse listener to the JLabel, it loses the
        // ability to automatically forward the mouse events to its
        // parent, and therefore the layer cannot be selected anymore
        // by left-clicking on this label. This is the workaround.
        JLabel source = (JLabel) e.getSource();
        LayerGUI layerGUI = (LayerGUI) source.getParent();
        layerGUI.setSelected(true);
    }

    private void initLayerVisibilityCB() {
        visibilityCB = createVisibilityCheckBox(false);

        visibilityCB.setSelected(layer.isVisible());
        visibilityCB.setToolTipText("<html><b>Click</b> to hide/show this layer.<br><b>Alt-click</b> to isolate this layer.");
        add(visibilityCB, LayerGUILayout.CHECKBOX);
    }

    private JCheckBox createVisibilityCheckBox(boolean smartFilter) {
        JCheckBox cb = new JCheckBox(CLOSED_EYE_ICON) {
            @Override
            public void setUI(ButtonUI ui) {
                super.setUI(ui);
                // after an LF change, it's necessary to reset the border to null
                setBorder(null);
            }

            @Override
            protected void processMouseEvent(MouseEvent e) {
                // isolating works after a theme-change only if the
                // mouse event processing is overridden at this level

                if (smartFilter) {
                    super.processMouseEvent(e);
                } else if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                    boolean altDown = (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == MouseEvent.ALT_DOWN_MASK;
                    if (altDown) {
                        layer.isolate();
                    } else {
                        // normal behaviour
                        boolean newVisibility = !visibilityCB.isSelected();
                        layer.setVisible(newVisibility, true, true);
                    }
                }
            }
        };
        cb.setRolloverIcon(CLOSED_EYE_ICON);
        cb.setSelectedIcon(OPEN_EYE_ICON);
        cb.setFocusPainted(false);
        cb.setIconTextGap(0);
        cb.setBorder(null);
        cb.setBorderPainted(false);

        return cb;
    }

    private void initLayerNameEditor() {
        nameEditor = new LayerNameEditor(this);
        add(nameEditor, LayerGUILayout.NAME_EDITOR);
        addPropertyChangeListener("name", evt -> updateName());
    }

    private void bindSelectionToLayerActivation() {
        addItemListener(e -> {
            if (reactToItemEvents) {
                // invoke later, when isSelected() returns the correct value
                EventQueue.invokeLater(this::buttonActivationChanged);
            }
        });
    }

    private void buttonActivationChanged() {
        if (isSelected()) {
            // during comp actions, the active layer might already be inside the layer
            if (!layer.contains(layer.getComp().getActiveLayer())) {
                layer.activate();
            }
        } else {
            // the layer was just deactivated
            nameEditor.disableEditing();
        }
        updateSelectionState();
    }

    @Override
    public void setOpenEye(boolean newVisibility) {
        visibilityCB.setSelected(newVisibility);
        layer.setVisible(newVisibility, true, true);
    }

    @Override
    public boolean isEyeOpen() {
        return visibilityCB.isSelected();
    }

    public void setReactToItemEvents(boolean reactToItemEvents) {
        this.reactToItemEvents = reactToItemEvents;
    }

    private void setDragReorderHandler(DragReorderHandler handler) {
        if (dragReorderHandler != null) {
            return; // don't attach twice
        }
        attachDragHandler(handler);
    }

    public void attachDragHandler(DragReorderHandler handler) {
        assert dragReorderHandler == null;
        assert handler != null;

        dragReorderHandler = handler;
        handler.attachTo(this);
        handler.attachTo(nameEditor);
        handler.attachTo(layerIconLabel);

        if (delayedDragHandler) {
            if (maskIconLabel != null) {
                handler.attachTo(maskIconLabel);
            }
        }

        for (LayerGUI child : children) {
            // if it was already set, then the call will be ignored
            child.setDragReorderHandler(handler);
        }
    }

    private void detachDragHandler() {
        if (dragReorderHandler == null) {
            return;
        }

        dragReorderHandler.detachFrom(this);
        dragReorderHandler.detachFrom(nameEditor);
        dragReorderHandler.detachFrom(layerIconLabel);

        if (hasMaskIcon()) {
            dragReorderHandler.detachFrom(maskIconLabel);
        }

        for (LayerGUI child : children) {
            child.detachDragHandler();
        }

        dragReorderHandler = null;
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

    @Override
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
        assert calledOnEDT() : callInfo();
        assert layer.hasRasterIcon();

        // the synchronous update avoids starting a filter twice
        // TODO dubious design
        boolean synchronousUpdate = layer instanceof CompositeLayer;

        if (synchronousUpdate) {
            BufferedImage thumb = layer.createIconThumbnail();
            assert thumb != null;
            if (thumb != null) {
                updateIconOnEDT(layer, thumb);
            }
            return;
        }

        Runnable notEDT = () -> {
            BufferedImage thumb = layer.createIconThumbnail();
            assert thumb != null;
            if (thumb != null) {
                SwingUtilities.invokeLater(() -> updateIconOnEDT(layer, thumb));
            }
        };

        new Thread(notEDT).start();
    }

    private void updateIconOnEDT(Layer layer, BufferedImage thumb) {
        assert calledOnEDT() : callInfo();
        if (layer instanceof LayerMask mask) {
            if (!hasMaskIcon()) {
                return;
            }
            boolean disabledMask = !mask.getOwner().isMaskEnabled();
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
        add(maskIconLabel, LayerGUILayout.MASK);

        // there is another mouse listener for the right-click popups
        maskIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                maskIconClicked(e);
            }
        });

        if (dragReorderHandler != null) {
            dragReorderHandler.attachTo(maskIconLabel);
            delayedDragHandler = false;
        } else {
            delayedDragHandler = true;
        }

        // don't call layer.getMask().updateIconImage(); because
        // it requires an ui, which could be constructed right now.
        updateLayerIconImageAsync(layer.getMask());
        revalidate();
    }

    private void maskIconClicked(MouseEvent e) {
        activateLayerNow();

        boolean altClick = e.isAltDown();
        boolean shiftClick = e.isShiftDown();
        View view = layer.getComp().getView();

        if (altClick && shiftClick) {
            // shift-alt-click switches to RUBYLITH
            // except when it already is in RUBYLITH
            if (view.getMaskViewMode() == MaskViewMode.RUBYLITH) {
                MaskViewMode.EDIT_MASK.activate(view, layer);
            } else {
                MaskViewMode.RUBYLITH.activate(view, layer);
            }
        } else if (altClick) {
            // alt-click switches to VIEW_MASK
            // except when it already is in VIEW_MASK
            if (view.getMaskViewMode() == MaskViewMode.VIEW_MASK) {
                MaskViewMode.EDIT_MASK.activate(view, layer);
            } else {
                MaskViewMode.VIEW_MASK.activate(view, layer);
            }
        } else if (shiftClick) {
            // shift-click toggles the enabled-disabled state
            layer.setMaskEnabled(!layer.isMaskEnabled(), true);
        } else { // plain click, without key modifiers
            // don't change SHOW_MASK or RUBYLITH into EDIT_MASK
            if (!view.getMaskViewMode().editMask()) {
                MaskViewMode.EDIT_MASK.activate(layer);
            }
            // ...but make sure that the layer is notified even if
            // the view already was in mask editing mode
            // (this is important for smart filters)
            layer.setMaskEditing(true);
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
        GUIUtils.removeAllMouseListeners(maskIconLabel);

        remove(maskIconLabel);
        revalidate();
        repaint();
        maskIconLabel = null;

        delayedDragHandler = false;
    }

    @Override
    public void updateSelectionState() {
        if (!layer.isActive()) {
            setSelectionState(SelectionState.INACTIVE);
        } else if (layer.isMaskEditing()) {
            setSelectionState(SelectionState.MASK_SELECTED);
        } else {
            setSelectionState(SelectionState.LAYER_SELECTED);
        }
    }

    private void setSelectionState(SelectionState newSelectionState) {
        if (newSelectionState != selectionState) {
            selectionState = newSelectionState;
            selectionState.applyBorderStyles(layerIconLabel, maskIconLabel);
        }
    }

    @Override
    public void changeLayer(Layer newLayer) {
        this.layer = newLayer;
        updateName();
        Icon icon = createLayerIcon(layer);
        if (icon != null) { // has fix icon
            layerIconLabel.setIcon(icon);
        } else {
            updateLayerIconImageAsync(layer);
        }

        if (maskIconLabel != null) {
            removeMaskIcon();
        }
        if (newLayer.hasMask()) {
            addMaskIcon();
        }
        updateSelectionState();
        updateChildrenPanel();
    }

    @Override
    public void updateUI() {
        // don't use any UI
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!layer.getTopLevelLayer().isActiveTopLevel()) {
            // no custom painting if not selected or semi-selected
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        // save Graphics settings
        Color origColor = g.getColor();
        Object origAA = g2.getRenderingHint(KEY_ANTIALIASING);

        // paint a rounded rectangle with the
        // selection color on the selected layer GUI
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(determineSelectedColor());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // restore Graphics settings
        g.setColor(origColor);
        g2.setRenderingHint(KEY_ANTIALIASING, origAA);
    }

    private Color determineSelectedColor() {
        boolean darkTheme = Themes.getActive().isDark();
        if (layer.isActive()) {
            return darkTheme ? SELECTED_DARK_COLOR : SELECTED_COLOR;
        } else {
            return darkTheme ? SEMI_SELECTED_DARK_COLOR : SEMI_SELECTED_COLOR;
        }
    }

    @Override
    public int getId() {
        return uniqueId;
    }

    public int getPreferredHeight() {
        return layout.getPreferredHeight();
    }

    public void updateThumbSize(int newThumbSize) {
        layout.updateHeight(newThumbSize);
        for (LayerGUI child : children) {
            child.updateThumbSize(newThumbSize);
        }
    }

    @Override
    public void setParentUI(LayerUI parentUI) {
        this.parentUI = (LayerGUI) parentUI;
    }

    public LayerGUI getParentUI() {
        return parentUI;
    }

    @Override
    public void detach() {
        setParentUI(null);
        detachDragHandler();
    }

    public List<LayerGUI> getChildren() {
        return children;
    }

    public boolean isEmbedded() {
        return parentUI != null;
    }

    @Override
    public boolean checkInvariants() {
        if (parentUI != null) {
            if (!parentUI.containsChild(this)) {
                throw new AssertionError("parent UI ('%s') doesn't contain this UI ('%s')"
                    .formatted(parentUI.getLayerName(), this.getLayerName()));
            }
        }
        if (!layer.isTopLevel()) {
            if (parentUI == null) {
                throw new AssertionError("null parentUI in '%s' UI, holder class = '%s'"
                    .formatted(getLayerName(), layer.getHolder().getClass().getSimpleName()));
            }

            LayerUI holderUI = ((CompositeLayer) layer.getHolder()).getUI();
            if (holderUI != parentUI) {
                throw new AssertionError("mismatched UIs: holderUI = '%s', parentUI = '%s'"
                    .formatted(holderUI.getLayerName(), parentUI.getLayerName()));
            }
        }
        return true;
    }

    private boolean containsChild(LayerGUI layerGUI) {
        return children.contains(layerGUI);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);
        node.addInt("unique id", uniqueId);

        node.addPresence("dragReorderHandler", dragReorderHandler);
        node.addPresence("parentUI", parentUI);

        for (LayerGUI child : children) {
            node.add(child.createDebugNode("child " + child.getLayer().getName()));
        }

        node.addBoolean("lateDragHandler", delayedDragHandler);
        node.addAsString("selectionState", selectionState);
        node.addString("layer name", layer.getName());

        return node;
    }

    @Override
    public String toString() {
        return "LayerGUI{" +
            "name='" + getLayerName() + '\'' +
            ", id='" + getId() + '\'' +
            ", has mask icon: " + (hasMaskIcon() ? "YES" : "NO") +
            '}';
    }
}
