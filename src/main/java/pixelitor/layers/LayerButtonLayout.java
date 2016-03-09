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

import pixelitor.gui.ImageComponents;
import pixelitor.utils.AppPreferences;

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

    private final boolean isImageLayer;

    public static final String CHECKBOX = "CHECKBOX";
    public static final String LAYER = "LAYER";
    public static final String MASK = "MASK";
    public static final String NAME_EDITOR = "NAME_EDITOR";

    public static final int SMALL_THUMB_SIZE = 24;
    private static final int GAP = 7;

    public static int thumbSize;

    // The labels will appear to have thumbSize, but in reality
    // they must be larger in order to leave space to the borders
    public static int labelSize;

    private static int height;

    static {
        setThumbSize(AppPreferences.loadThumbSize());
    }

    public LayerButtonLayout(Layer layer) {
        isImageLayer = layer instanceof ImageLayer;
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
            return new Dimension(100, height);
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
            Dimension cbSize = checkBox.getPreferredSize();
            checkBox.setBounds(startX, (height - cbSize.height) / 2, cbSize.width, cbSize.height);
            startX += (cbSize.width + GAP - LayerButton.BORDER_WIDTH);

            // lay out the layer icon
            int labelStartY = GAP - LayerButton.BORDER_WIDTH;
            if (isImageLayer) {
                layerLabel.setBounds(startX, labelStartY, labelSize, labelSize);
                startX += labelSize;
            } else {
                layerLabel.setBounds(startX, (height - SMALL_THUMB_SIZE) / 2, SMALL_THUMB_SIZE, SMALL_THUMB_SIZE);
                startX += SMALL_THUMB_SIZE;
            }

            // lay out the mask
            if (maskLabel != null) {
                // no need to add distance between layer and mask icons
                // because there will be a visual distance because of the borders
                maskLabel.setBounds(startX, labelStartY, labelSize, labelSize);
                startX += (labelSize + GAP);
            } else {
                startX += GAP; // distance between layer icon and text field
            }

            int editorHeight = (int) nameEditor.getPreferredSize().getHeight();

            int remainingWidth = parent.getWidth() - startX;
            int adjustment = 2; // the textfield in Nimbus has two invisible pixels around it
            nameEditor.setBounds(startX - adjustment, (height - editorHeight) / 2, remainingWidth - 3, editorHeight);
        }
    }

    public static int getThumbSize() {
        return thumbSize;
    }

    public static void setThumbSize(int newThumbSize) {
        if (thumbSize == newThumbSize) {
            return;
        }

        thumbSize = newThumbSize;

        labelSize = newThumbSize + 2 * LayerButton.BORDER_WIDTH;

        height = newThumbSize + 2 * GAP;

        ImageComponents.onActiveIC(ic -> {
            LayersContainer.showLayersPanel(ic.getLayersPanel());
            ic.getComp().updateAllIconImages();
        });
    }
}
