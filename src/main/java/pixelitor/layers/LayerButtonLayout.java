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

    public static final String VISIBILITY_BUTTON = "VISIBILITY_BUTTON";
    public static final String NAME_EDITOR = "NAME_EDITOR";

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
            int buttonWidth = (int) visibilityButton.getPreferredSize().getWidth();
            int buttonHeight = (int) visibilityButton.getPreferredSize().getHeight();
            int adjustment = 2; // it is necessary for some reason
            visibilityButton.setBounds(hGap, vGap + adjustment, buttonWidth, buttonHeight);
            nameEditor.setBounds(hGap * 2 + buttonWidth, vGap, parent.getWidth() - buttonWidth - 3 * hGap, (int) nameEditor.getPreferredSize().getHeight());
        }
    }
}
