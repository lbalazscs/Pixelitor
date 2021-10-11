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

import pixelitor.OpenImages;
import pixelitor.utils.AppPreferences;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * The layout manager for a {@link LayerButton}
 */
public class LayerButtonLayout implements LayoutManager {
    private Component nameEditor;

    private JCheckBox checkBox;
    private JLabel layerLabel;
    private JLabel maskLabel;
    private JLabel sfLabel;
    private JCheckBox sfCheckBox;

    private final boolean layerIconShowsThumbnail;

    public static final String CHECKBOX = "CHECKBOX";
    public static final String LAYER = "LAYER";
    public static final String MASK = "MASK";
    public static final String NAME_EDITOR = "NAME_EDITOR";
    public static final String SMART_FILTER_LABEL = "SF_LABEL";
    public static final String SMART_FILTER_CHECKBOX = "SF_CHECKBOX";

    public static final int SMALL_THUMB_SIZE = 24;
    private static final int GAP = 7;

    public static int thumbSize;

    // The labels will appear to have thumbSize, but in reality
    // they must be larger in order to leave space to the borders
    private int labelSize;

    private int height;

    static {
        setStaticThumbSize(AppPreferences.loadThumbSize());
    }

    public LayerButtonLayout(Layer layer) {
        layerIconShowsThumbnail = layer.hasIconThumbnail();
        thumbSizeChanged(thumbSize);
    }

    public void thumbSizeChanged(int newThumbSize) {
        if (layerIconShowsThumbnail) {
            height = newThumbSize + 2 * GAP;
        } else {
            height = SMALL_THUMB_SIZE + 2 * GAP;
        }
        labelSize = newThumbSize + 2 * LayerButton.BORDER_WIDTH;
    }

    @Override
    public void addLayoutComponent(String name, Component c) {
        synchronized (c.getTreeLock()) {
            switch (name) {
                case CHECKBOX -> checkBox = (JCheckBox) c;
                case LAYER -> layerLabel = (JLabel) c;
                case MASK -> maskLabel = (JLabel) c;
                case NAME_EDITOR -> nameEditor = c;
                case SMART_FILTER_LABEL -> sfLabel = (JLabel) c;
                case SMART_FILTER_CHECKBOX -> sfCheckBox = (JCheckBox) c;
                default -> throw new IllegalStateException();
            }
        }
    }

    @Override
    public void removeLayoutComponent(Component c) {
        if (c == maskLabel) {
            synchronized (c.getTreeLock()) {
                maskLabel = null;
            }
        } else if (c == sfLabel) {
            synchronized (c.getTreeLock()) {
                sfLabel = null;
            }
        } else if (c == sfCheckBox) {
            synchronized (c.getTreeLock()) {
                sfCheckBox = null;
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public int getPreferredHeight() {
        if (sfLabel == null) {
            return height;
        } else {
            return height + 26;
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            return new Dimension(100, getPreferredHeight());
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
            int layerIconStartX = startX;
            int labelStartY = GAP - LayerButton.BORDER_WIDTH;
            if (layerIconShowsThumbnail) {
                layerLabel.setBounds(startX, labelStartY, labelSize, labelSize);
                startX += labelSize;
            } else {
                layerLabel.setBounds(startX, (height - SMALL_THUMB_SIZE) / 2, SMALL_THUMB_SIZE, SMALL_THUMB_SIZE);
                startX += SMALL_THUMB_SIZE;
            }

            // lay out the mask
            if (maskLabel != null) {
                // no need to add distance between the layer and mask icons
                // because there will be a visual distance due to the borders
                maskLabel.setBounds(startX, labelStartY, labelSize, labelSize);
                startX += (labelSize + GAP);
            } else {
                startX += GAP; // the distance between the layer icon and the text field
            }

            int editorHeight = (int) nameEditor.getPreferredSize().getHeight();

            int remainingWidth = parent.getWidth() - startX;
            int adjustment = 2; // the textfield in Nimbus has two invisible pixels around it
            nameEditor.setBounds(startX - adjustment, (height - editorHeight) / 2, remainingWidth - 3, editorHeight);

            if (sfLabel != null) {
                sfCheckBox.setBounds(layerIconStartX - adjustment, height, cbSize.width, cbSize.height);
                int fullCBSize = cbSize.width + GAP;
                remainingWidth = parent.getWidth() - layerIconStartX - fullCBSize;
                sfLabel.setBounds(layerIconStartX + fullCBSize, height, remainingWidth, cbSize.height);
            }
        }
    }

    public static int getThumbSize() {
        return thumbSize;
    }

    public static void setStaticThumbSize(int newThumbSize) {
        if (thumbSize == newThumbSize) {
            return;
        }

        thumbSize = newThumbSize;

        OpenImages.thumbSizeChanged(newThumbSize);
    }
}
