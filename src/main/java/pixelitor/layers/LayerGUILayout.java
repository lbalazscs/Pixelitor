/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Views;
import pixelitor.gui.utils.Theme;
import pixelitor.gui.utils.Themes;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * The layout manager for a {@link LayerGUI}
 */
public class LayerGUILayout implements LayoutManager {
    private Component nameEditor;

    private JCheckBox checkBox;
    private JLabel layerLabel;
    private JLabel maskLabel;
    private JPanel childrenPanel;

    private final boolean layerIconShowsThumbnail;

    public static final String CHECKBOX = "CHECKBOX";
    public static final String LAYER = "LAYER";
    public static final String MASK = "MASK";
    public static final String NAME_EDITOR = "NAME_EDITOR";
    public static final String CHILDREN = "FILTERS";

    // the size of the icon
    private static final int CHECKBOX_WIDTH = 24;
    private static final int CHECKBOX_HEIGHT = 24;

    public static final int SMALL_THUMB_SIZE = 24;
    private static final int GAP = 7;

    private static boolean isNimbus = Themes.getActive().isNimbus();

    // The labels will appear to have thumbSize, but in reality
    // they must be larger in order to leave space to the borders
    private int labelSize;

    private int height;

    public LayerGUILayout(Layer layer) {
        layerIconShowsThumbnail = layer.hasRasterThumbnail();
        updateHeight(Views.thumbSize);
    }

    public void updateHeight(int newThumbSize) {
        if (layerIconShowsThumbnail) {
            height = newThumbSize + 2 * GAP;
        } else {
            height = SMALL_THUMB_SIZE + 2 * GAP;
        }
        labelSize = newThumbSize + 2 * LayerGUI.BORDER_WIDTH;
    }

    @Override
    public void addLayoutComponent(String name, Component c) {
        synchronized (c.getTreeLock()) {
            switch (name) {
                case CHECKBOX -> checkBox = (JCheckBox) c;
                case LAYER -> layerLabel = (JLabel) c;
                case MASK -> maskLabel = (JLabel) c;
                case NAME_EDITOR -> nameEditor = c;
                case CHILDREN -> childrenPanel = (JPanel) c;
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
        } else if (c == childrenPanel) {
            synchronized (c.getTreeLock()) {
                childrenPanel = null;
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public int getPreferredHeight() {
        if (childrenPanel == null) {
            return height;
        } else {
            // TODO the preferred height is 26 * num_filters?
            return height + childrenPanel.getPreferredSize().height;
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
            Dimension cbPrefSize = checkBox.getPreferredSize();
            if (cbPrefSize.width != CHECKBOX_WIDTH || cbPrefSize.height != CHECKBOX_HEIGHT) {
                throw new IllegalStateException("width = " + cbPrefSize.width + ", height = " + cbPrefSize.height);
            }
            checkBox.setBounds(startX, (height - CHECKBOX_HEIGHT) / 2, CHECKBOX_WIDTH, CHECKBOX_HEIGHT);
            startX += (CHECKBOX_WIDTH + GAP - LayerGUI.BORDER_WIDTH);

            // lay out the layer icon
            int layerIconStartX = startX;
            int labelStartY = GAP - LayerGUI.BORDER_WIDTH;
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
            // the textfield in Nimbus has two invisible pixels around it
            int adjustment = isNimbus ? 2 : 0;
            nameEditor.setBounds(startX - adjustment, (height - editorHeight) / 2, remainingWidth - GAP + 2 * adjustment, editorHeight);

            if (childrenPanel != null) {
                Dimension sfSize = childrenPanel.getPreferredSize();
                int sfX = layerIconStartX - adjustment;
                int sfY = height;
                int sfWidth = parent.getWidth() - sfX;
                int sfHeight = sfSize.height;
                childrenPanel.setBounds(sfX, sfY, sfWidth, sfHeight);
            }
        }
    }

    public static int getThumbSize() {
        return Views.thumbSize;
    }

    public static void themeChanged(Theme theme) {
        isNimbus = theme.isNimbus();
    }
}
