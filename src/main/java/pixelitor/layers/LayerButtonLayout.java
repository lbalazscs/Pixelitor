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

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * A layout manager for a layout button
 */
public class LayerButtonLayout implements LayoutManager {
    private Component nameEditor;

    private JCheckBox checkBox;
    private JLabel layerLabel;
    private JLabel maskLabel;

    public static final String CHECKBOX = "CHECKBOX";
    public static final String LAYER = "LAYER";
    public static final String MASK = "MASK";
    public static final String NAME_EDITOR = "NAME_EDITOR";

    private static final int GAP = 7;
    public static final int THUMB_SIZE = 24;

    /**
     * The labels will appear to have THUMB_SIZE, but in reality
     * they must be larger in order to leave space to the borders
     */
    public static final int LABEL_SIZE = THUMB_SIZE + 2 * LayerButton.BORDER_WIDTH;

    private static final int HEIGHT = THUMB_SIZE + 2 * GAP;

    public LayerButtonLayout() {
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            if (CHECKBOX.equals(name)) {
                checkBox = (JCheckBox) comp;
            } else if (LAYER.equals(name)) {
                layerLabel = (JLabel) comp;
            } else if (MASK.equals(name)) {
                maskLabel = (JLabel) comp;
            } else if (NAME_EDITOR.equals(name)) {
                nameEditor = comp;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        if (comp == maskLabel) {
            maskLabel = null;
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            return new Dimension(100, HEIGHT);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int startX = GAP;

            // lay out the checkbox
            checkBox.setBounds(startX, GAP, THUMB_SIZE, THUMB_SIZE);
            startX += (THUMB_SIZE + GAP - LayerButton.BORDER_WIDTH);

            // lay out the layer icon
            int labelStartY = GAP - LayerButton.BORDER_WIDTH;
            layerLabel.setBounds(startX, labelStartY, LABEL_SIZE, LABEL_SIZE);
            startX += LABEL_SIZE;

            // lay out the mask
            if (maskLabel != null) {
                // no need to add distance between layer and mask icons
                // because there will be a visual distance because of the borders
                maskLabel.setBounds(startX, labelStartY, LABEL_SIZE, LABEL_SIZE);
                startX += (LABEL_SIZE + GAP);
            } else {
                startX += GAP; // distance between layer icon and text field
            }

            int editorHeight = (int) nameEditor.getPreferredSize().getHeight();

            int remainingWidth = parent.getWidth() - startX;
            int adjustment = 2; // seems that the textfield has two invisible pixels around it
            nameEditor.setBounds(startX - adjustment, GAP - adjustment, remainingWidth - 3, editorHeight);
        }
    }
}
