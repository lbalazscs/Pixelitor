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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
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
    private LayerNameEditor nameEditor;

    private final JComponent layerIcon;
    private JLabel maskIconLabel;

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
        } else if (layer instanceof AdjustmentLayer) {
            Icon adjLayerIcon = IconUtils.getAdjLayerIcon();
            layerIcon = new JButton(adjLayerIcon);

            ((JButton) layerIcon).addActionListener(e -> {
                System.out.println("LayerButton::LayerButton: adjustment layer icon clicked");
            });
        } else {
            layerIcon = new JLabel("", null, CENTER);
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
            } else {
                nameEditor.disableEditing();
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

//        System.out.println("LayerButton::updateLayerIconImage: CALLED, updateMask = " + updateMask);

        BufferedImage img = layer.getCanvasSizedSubImage();

        Runnable notEDT = () -> {
            BufferedImage thumb = ImageUtils.createThumbnail(img, LayerButtonLayout.ICON_SIZE);
            Runnable edt = () -> {
                if (updateMask) {
                    if (maskIconLabel == null) {
                        return;
                    }
                    BufferedImage iconImage = thumb;
                    if (!layer.isMaskEnabled()) {
                        int thumbWidth = thumb.getWidth();
                        int thumbHeight = thumb.getHeight();
                        // thumb is GRAY image, this one needs colors
                        // TODO is is faster if thumb is created this way in createThumbnail?
                        iconImage = ImageUtils.createCompatibleImage(thumbWidth, thumbHeight);
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
                    ((JLabel) layerIcon).setIcon(new ImageIcon(thumb));
                }
                repaint();
            };
            SwingUtilities.invokeLater(edt);
        };
        new Thread(notEDT).start();
    }

    public void addMaskIconLabel() {
        maskIconLabel = new JLabel("", null, CENTER);
        LayerMaskActions.addPopupMenu(maskIconLabel, layer);
        configureLayerIcon(maskIconLabel);
        add(maskIconLabel, LayerButtonLayout.ICON);
        revalidate();
    }

    public void deleteMaskIconLabel() {
        remove(maskIconLabel);
        revalidate();
        maskIconLabel = null;
    }

    @Override
    public String toString() {
        return "LayerButton{" +
                "name='" + getLayerName() + '\'' +
                "is maskIconLabel null: " + (maskIconLabel == null ? "YES" : "NO") +
                '}';
    }
}
