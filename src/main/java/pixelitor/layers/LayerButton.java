/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.PixelitorWindow;
import pixelitor.history.AddToHistory;
import pixelitor.utils.IconUtils;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;

/**
 * A GUI element representing a layer in an image
 */
public class LayerButton extends JToggleButton {
    //    public static final EmptyBorder BORDER = new EmptyBorder(5, 5, 5, 5);
    private final Layer layer;

    private static final Icon OPEN_EYE_ICON = IconUtils.loadIcon("eye_open.png");
    private static final Icon CLOSED_EYE_ICON = IconUtils.loadIcon("eye_closed.png");

    private static final String uiClassID = "LayerButtonUI";
    private JCheckBox visibilityCB;

    private boolean userInteraction = true;
    private JTextField nameEditor; // actually, a LayerNameEditor subclass

    private final JComponent layerIcon;
    private JLabel maskIcon;

    /**
     * The Y coordinate in the parent when it is not dragging
     */
    private int staticY;
//    private final JPanel iconsPanel;

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @Override
    public void updateUI() {
        setUI(new LayerButtonUI());
    }

    public LayerButton(Layer layer) {
        this.layer = layer;

        setLayout(new LayerButtonLayout());

        initVisibilityControl(layer);
        initLayerNameEditor(layer);

        if (layer instanceof TextLayer) {
            Icon textLayerIcon = IconUtils.getTextLayerIcon();
            layerIcon = new JButton(textLayerIcon);

            ((JButton) layerIcon).addActionListener(e -> TextLayer.edit(PixelitorWindow.getInstance(), layer.getComp(), (TextLayer) layer));
        } else if(layer instanceof AdjustmentLayer) {
            Icon adjLayerIcon = IconUtils.getAdjLayerIcon();
            layerIcon = new JButton(adjLayerIcon);

            ((JButton) layerIcon).addActionListener(e -> {
                System.out.println("LayerButton::LayerButton: adjustment layer icon clicked");
            });
        } else {
            layerIcon = new JLabel();
        }

        configureLayerIcon(layerIcon);
        add(layerIcon, LayerButtonLayout.ICON);

        wireSelectionWithLayerActivation(layer);
    }

    private void configureLayerIcon(JComponent layerIcon) {
        layerIcon.putClientProperty("JComponent.sizeVariant", "mini");

        if (layerIcon instanceof JButton) {
            JButton layerButton = (JButton) layerIcon;
            layerButton.setMargin(new Insets(0, 0, 0, 0));
            layerButton.setBorderPainted(false);
        }
        layerIcon.setPreferredSize(new Dimension(LayerButtonLayout.ICON_SIZE, LayerButtonLayout.ICON_SIZE));
    }

    private void initVisibilityControl(Layer layer) {
        visibilityCB = new JCheckBox(CLOSED_EYE_ICON);
        visibilityCB.setRolloverIcon(CLOSED_EYE_ICON);

        visibilityCB.setSelected(true);
        visibilityCB.setToolTipText("Layer Visibility");
        visibilityCB.setSelectedIcon(OPEN_EYE_ICON);
        add(visibilityCB, LayerButtonLayout.ICON);
//        iconsPanel.add(visibilityCB);
        visibilityCB.addItemListener(e -> layer.setVisible(visibilityCB.isSelected(), AddToHistory.YES));
    }

    private void initLayerNameEditor(Layer layer) {
        nameEditor = new LayerNameEditor(this, layer);
        add(nameEditor, LayerButtonLayout.NAME_EDITOR);
//        add(nameEditor, BorderLayout.CENTER);
        addPropertyChangeListener("name", evt -> nameEditor.setText(getName()));
    }

    private void wireSelectionWithLayerActivation(Layer layer) {
        addItemListener(e -> {
            if (isSelected()) {
                layer.makeActive(userInteraction ? AddToHistory.YES : AddToHistory.NO);
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

    public void updateLayerIconImage(BufferedImage img, boolean updateMask) {
        Runnable notEDT = () -> {
            BufferedImage thumb = ImageUtils.createThumbnail(img, LayerButtonLayout.ICON_SIZE);
            Runnable edt = () -> {
                if (updateMask) {
                    if (maskIcon == null) {
                        return;
                    }
                    maskIcon.setIcon(new ImageIcon(thumb));
                    maskIcon.paintImmediately(maskIcon.getBounds());
                } else {
                    ((JLabel) layerIcon).setIcon(new ImageIcon(thumb));
                    layerIcon.paintImmediately(layerIcon.getBounds());
                }
            };
            SwingUtilities.invokeLater(edt);
        };
        new Thread(notEDT).start();
    }

    public void addMaskIcon() {
        maskIcon = new JLabel();
        LayerMaskActions.configureWithPopupMenu(maskIcon);
        configureLayerIcon(maskIcon);
        add(maskIcon, LayerButtonLayout.ICON);
        revalidate();
    }

    public void deleteMaskIcon() {
        remove(maskIcon);
        revalidate();
        maskIcon = null;
    }

    @Override
    public String toString() {
        return "LayerButton{" +
                "name='" + getLayerName() + '\'' +
                '}';
    }
}
