/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * A layout manager for a layout button
 */
public class LayerButtonLayout implements LayoutManager {
    private final int hGap;
    private final int vGap;
    private Component visibilityButton;
    private Component nameEditor;
    private Component layerIcon;

    public static final String VISIBILITY_BUTTON = "VISIBILITY_BUTTON";
    public static final String NAME_EDITOR = "NAME_EDITOR";
    public static final String LAYER_ICON = "LAYER_ICON";

    public LayerButtonLayout(int hGap, int vGap) {
        this.hGap = hGap;
        this.vGap = vGap;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            if (VISIBILITY_BUTTON.equals(name)) {
                visibilityButton = comp;
            } else if (NAME_EDITOR.equals(name)) {
                nameEditor = comp;
            } else {
                layerIcon = comp;
            }
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            int preferredHeight = Math.max((int) visibilityButton.getPreferredSize().getHeight(), (int) nameEditor.getPreferredSize().getHeight());
            preferredHeight += 2 * vGap;
            return new Dimension(100, preferredHeight);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Dimension visButtonSize = visibilityButton.getPreferredSize();
            int visButtonWidth = (int) visButtonSize.getWidth();
            int visButtonHeight = (int) visButtonSize.getHeight();
            int adjustment = 2; // it is necessary for some reason, TODO
            visibilityButton.setBounds(hGap, vGap + adjustment, visButtonWidth, visButtonHeight);

            int editorHeight = (int) nameEditor.getPreferredSize().getHeight();
            int secondElemStart = hGap * 2 + visButtonWidth;
            if (layerIcon != null) {
                int layerIconWidth = (int) layerIcon.getPreferredSize().getWidth();
                int layerIconHeight = (int) layerIcon.getPreferredSize().getHeight();

                layerIcon.setBounds(secondElemStart, vGap + adjustment, layerIconWidth, layerIconHeight);
                int layerIconEnd = secondElemStart + layerIconWidth;
                int remainingWidth = parent.getWidth() - layerIconEnd - 2 * hGap;
                nameEditor.setBounds(layerIconEnd + hGap - 2, vGap, remainingWidth + 2, editorHeight);
            } else {
                int remainingWidth = parent.getWidth() - visButtonWidth - 3 * hGap;
                nameEditor.setBounds(secondElemStart, vGap, remainingWidth, editorHeight);
            }
        }
    }
}
