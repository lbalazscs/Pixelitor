/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.gui.ImageComponent;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.AddToHistory;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * A GUI element representing a layer in an image
 */
public class LayerButton extends JToggleButton {
    private static final Icon OPEN_EYE_ICON = IconUtils.loadIcon("eye_open.png");
    private static final Icon CLOSED_EYE_ICON = IconUtils.loadIcon("eye_closed.png");

    private static final String uiClassID = "LayerButtonUI";

    public static final Color UNSELECTED_COLOR = new Color(214, 217, 223);
    public static final Color SELECTED_COLOR = new Color(48, 76, 111);

    public static final int BORDER_WIDTH = 2;

    private enum SelectionState {
        UNSELECTED {
            @Override
            public void activate(JLabel layer, JLabel mask) {
                layer.setBorder(unSelectedIconOnUnselectedLayerBorder);
                if (mask != null) {
                    mask.setBorder(unSelectedIconOnUnselectedLayerBorder);
                }
            }
        }, SELECT_LAYER {
            @Override
            public void activate(JLabel layer, JLabel mask) {
                layer.setBorder(selectedBorder);
                if (mask != null) {
                    mask.setBorder(unSelectedIconOnSelectedLayerBorder);
                }
            }
        }, SELECT_MASK {
            @Override
            public void activate(JLabel layer, JLabel mask) {
                layer.setBorder(unSelectedIconOnSelectedLayerBorder);
                if (mask != null) {
                    mask.setBorder(selectedBorder);
                }
            }
        };

        private static final Border lightBorder;

        static {
            if (JVM.isMac) {
                // seems to be a Mac-specific problem: with LineBorder,
                // a one pixel wide line disappears
                lightBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, UNSELECTED_COLOR);
            } else {
                lightBorder = BorderFactory.createLineBorder(UNSELECTED_COLOR, 1);
            }
        }

        private static final Border darkBorder = BorderFactory.createLineBorder(SELECTED_COLOR, 1);
        private static final Border selectedBorder = BorderFactory.createCompoundBorder(lightBorder, darkBorder);
        private static final Border unSelectedIconOnSelectedLayerBorder = BorderFactory.createLineBorder(SELECTED_COLOR, BORDER_WIDTH);
        private static final Border unSelectedIconOnUnselectedLayerBorder = BorderFactory.createLineBorder(UNSELECTED_COLOR, BORDER_WIDTH);

        public abstract void activate(JLabel layer, JLabel mask);
    }

    private SelectionState selectionState = SelectionState.UNSELECTED;

    private final Layer layer;
    private boolean userInteraction = true;

    private JCheckBox visibilityCB;
    private LayerNameEditor nameEditor;
    private final JLabel layerIconLabel;
    private JLabel maskIconLabel;

    /**
     * The Y coordinate in the parent when it is not dragging
     */
    private int staticY;

    public LayerButton(Layer layer) {
        this.layer = layer;

        setLayout(new LayerButtonLayout());

        initVisibilityControl(layer);
        initLayerNameEditor(layer);

        if (layer instanceof TextLayer) {
            Icon textLayerIcon = IconUtils.getTextLayerIcon();
            layerIconLabel = new JLabel(textLayerIcon);
            layerIconLabel.setToolTipText("Double-click to edit the text layer.");
        } else if (layer instanceof AdjustmentLayer) {
            Icon adjLayerIcon = IconUtils.getAdjLayerIcon();
            layerIconLabel = new JLabel(adjLayerIcon);
        } else {
            layerIconLabel = new JLabel("", null, CENTER);
        }

        layerIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickCount = e.getClickCount();
                if (clickCount == 1) {
                    MaskViewMode.NORMAL.activate(layer);
                } else {
                    if (layer instanceof TextLayer) {
                        ((TextLayer) layer).edit(PixelitorWindow.getInstance());
                    } else if (layer instanceof AdjustmentLayer) {
                        ((AdjustmentLayer) layer).configure();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // by putting it into mouse pressed, it is consistent
                // with the mask clicks
                if (SwingUtilities.isLeftMouseButton(e)) {
                    selectLayerIfIconClicked(e);
                }
            }
        });

        configureLayerIcon(layerIconLabel, "layerIcon");
        configureBorders(layer.isMaskEditing());

        add(layerIconLabel, LayerButtonLayout.LAYER);

        wireSelectionWithLayerActivation(layer);
    }

    private static void configureLayerIcon(JLabel layerIcon, String name) {
//        layerIcon.putClientProperty("JComponent.sizeVariant", "mini");
        layerIcon.setName(name);
//        layerIcon.setPreferredSize(new Dimension(LayerButtonLayout.LABEL_SIZE, LayerButtonLayout.LABEL_SIZE));
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

    private void initVisibilityControl(Layer layer) {
        visibilityCB = new JCheckBox(CLOSED_EYE_ICON);
        visibilityCB.setRolloverIcon(CLOSED_EYE_ICON);

        visibilityCB.setSelected(true);
        visibilityCB.setToolTipText("Click to hide/show this layer.");
        visibilityCB.setSelectedIcon(OPEN_EYE_ICON);
        add(visibilityCB, LayerButtonLayout.CHECKBOX);

        visibilityCB.addItemListener(e ->
                layer.setVisible(visibilityCB.isSelected(), AddToHistory.YES));
    }

    private void initLayerNameEditor(Layer layer) {
        nameEditor = new LayerNameEditor(this, layer);
        add(nameEditor, LayerButtonLayout.NAME_EDITOR);
        addPropertyChangeListener("name", evt -> nameEditor.setText(getName()));
    }

    private void wireSelectionWithLayerActivation(Layer layer) {
        addItemListener(e -> {
            if (isSelected()) {
                layer.makeActive(userInteraction ? AddToHistory.YES : AddToHistory.NO);
            } else {
                nameEditor.disableEditing();
                // Invoke later because we can get here in the middle
                // of a new layer activation, when isSelected still
                // returns false, but the layer will be selected during
                // the same event processing.
                SwingUtilities.invokeLater(() ->
                        configureBorders(layer.isMaskEditing()));
            }
        });
    }

    public void setOpenEye(boolean newVisibility) {
        visibilityCB.setSelected(newVisibility);
    }

    public void setUserInteraction(boolean userInteraction) {
        this.userInteraction = userInteraction;
    }

    public void addMouseHandler(LayersMouseHandler mouseHandler) {
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        nameEditor.addMouseListener(mouseHandler);
        nameEditor.addMouseMotionListener(mouseHandler);
    }

    public void removeMouseHandler(LayersMouseHandler mouseHandler) {
        removeMouseListener(mouseHandler);
        removeMouseMotionListener(mouseHandler);
        nameEditor.removeMouseListener(mouseHandler);
        nameEditor.removeMouseMotionListener(mouseHandler);
    }

    public int getStaticY() {
        return staticY;
    }

    public void setStaticY(int staticY) {
        this.staticY = staticY;
    }

    public void dragFinished(int newLayerIndex) {
        layer.dragFinished(newLayerIndex);
    }

    public Layer getLayer() {
        return layer;
    }

    public String getLayerName() {
        return layer.getName();
    }

    public boolean isNameEditing() {
        return nameEditor.isEditable();
    }

    public boolean isVisibilityChecked() {
        return visibilityCB.isSelected();
    }

    public void changeNameProgrammatically(String newName) {
        nameEditor.setText(newName);
    }

    public void updateLayerIconImage(ImageLayer layer) {
        boolean updateMask = layer instanceof LayerMask;

        BufferedImage img = layer.getCanvasSizedSubImage();

        Runnable notEDT = () -> {
            BufferedImage thumb = ImageUtils.createThumbnail(img, LayerButtonLayout.THUMB_SIZE);
            Runnable edt = () -> {
                if (updateMask) {
                    if (maskIconLabel == null) {
                        return;
                    }
                    BufferedImage iconImage = thumb;
                    boolean disabledMask = !layer.getParent().isMaskEnabled();
                    if (disabledMask) {
                        int thumbWidth = thumb.getWidth();
                        int thumbHeight = thumb.getHeight();
                        // thumb is GRAY image, this one needs colors
                        // TODO is is faster if thumb is created this way in createThumbnail?
                        iconImage = ImageUtils.createSysCompatibleImage(thumbWidth, thumbHeight);
                        Graphics2D g = iconImage.createGraphics();
                        g.drawImage(thumb, 0, 0, null);
                        g.setColor(new Color(200, 0, 0));
                        g.setStroke(new BasicStroke(2.5f));
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.drawLine(0, 0, thumbWidth, thumbHeight);
                        g.drawLine(thumbWidth - 1, 0, 0, thumbHeight - 1);
                        g.dispose();
                    }
                    maskIconLabel.setIcon(new ImageIcon(iconImage));
                } else {
                    layerIconLabel.setIcon(new ImageIcon(thumb));
                }
                repaint();
            };
            SwingUtilities.invokeLater(edt);
        };
        new Thread(notEDT).start();
    }

    public void addMaskIconLabel() {
        maskIconLabel = new JLabel("", null, CENTER);
        maskIconLabel.setToolTipText("<html>Shift-click to disable/enable,<br>Alt-click to show mask/layer,<br>Right-click for more options");

        LayerMaskActions.addPopupMenu(maskIconLabel, layer);
        configureLayerIcon(maskIconLabel, "maskIcon");
        configureBorders(layer.isMaskEditing());
        add(maskIconLabel, LayerButtonLayout.MASK);

        // there is another mouse listener for the right-click popups
        maskIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isAltDown()) {
                    // alt-click switches to SHOW_MASK except when it
                    // already is in SHOW_MASK
                    ImageComponent ic = layer.getComp().getIC();
                    if (ic.getMaskViewMode() == MaskViewMode.SHOW_MASK) {
                        MaskViewMode.EDIT_MASK.activate(ic, layer);
                    } else {
                        MaskViewMode.SHOW_MASK.activate(ic, layer);
                    }
                } else if (e.isShiftDown()) {
                    // shift-click disables except when it is already disabled
                    layer.setMaskEnabled(!layer.isMaskEnabled(), AddToHistory.YES);
                } else {
                    MaskViewMode.EDIT_MASK.activate(layer);
                }
            }
        });

        revalidate();
    }

    public void deleteMaskIconLabel() {
        // TODO remove the two mouse listeners?
        remove(maskIconLabel);
        revalidate();
        maskIconLabel = null;
    }

    public void configureBorders(boolean maskEditing) {
        SelectionState newSelectionState;

        if (!isSelected()) {
            newSelectionState = SelectionState.UNSELECTED;
        } else {
            if (maskEditing) {
                newSelectionState = SelectionState.SELECT_MASK;
            } else {
                newSelectionState = SelectionState.SELECT_LAYER;
            }
        }

        if (newSelectionState != selectionState) {
            selectionState = newSelectionState;
            selectionState.activate(layerIconLabel, maskIconLabel);
        }
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
    public String toString() {
        return "LayerButton{" +
                "name='" + getLayerName() + '\'' +
                "is maskIconLabel null: " + (maskIconLabel == null ? "YES" : "NO") +
                '}';
    }
}
