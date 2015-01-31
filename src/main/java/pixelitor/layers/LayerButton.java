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

import pixelitor.utils.IconUtils;

import javax.swing.*;

/**
 * A GUI element representing a layer in an image
 */
public class LayerButton extends JToggleButton {
    private final Layer layer;

    private static final Icon OPEN_EYE_ICON = IconUtils.loadIcon("eye_open.png");
    private static final Icon CLOSED_EYE_ICON = IconUtils.loadIcon("eye_closed.png");

    private static final String uiClassID = "LayerButtonUI";
    private final JCheckBox visibilityCB;

    private boolean userInteraction = true;
    private final JTextField nameEditor;

    /**
     * The Y coordinate in the parent when it is not dragging
     */
    private int staticY;

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @Override
    public void updateUI() {
        setUI(new LayerButtonUI());
    }

    public LayerButton(final Layer layer) {
        this.layer = layer;
        setLayout(new LayerButtonLayout(5, 5));
        visibilityCB = new JCheckBox(CLOSED_EYE_ICON);
        visibilityCB.setRolloverIcon(CLOSED_EYE_ICON);

        visibilityCB.setSelected(true);
        visibilityCB.setToolTipText("Layer Visibility");
        visibilityCB.setSelectedIcon(OPEN_EYE_ICON);
        add(visibilityCB, LayerButtonLayout.VISIBILITY_BUTTON);

        nameEditor = new LayerNameEditor(this, layer);
        add(nameEditor, LayerButtonLayout.NAME_EDITOR);

        addPropertyChangeListener("name", evt -> nameEditor.setText(getName()));

        addItemListener(e -> {
            if (isSelected()) {
                layer.makeActive(userInteraction);
            }
        });

        visibilityCB.addItemListener(e -> layer.setVisible(visibilityCB.isSelected(), true));
    }

    @Override
    public String toString() {
        return "LayerButton{" +
                "name='" + layer.getName() + '\'' +
                '}';
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

    /**
     * Can be used for debugging
     */
    public String getLayerName() {
        return layer.getName();
    }

    public boolean isNameEditing() {
        return nameEditor.isEditable();
    }

    public boolean isVisibilityChecked() {
        return visibilityCB.isSelected();
    }
}
