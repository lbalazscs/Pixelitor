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
import java.util.ArrayList;
import java.util.List;

/**
 * A layout manager for a layout button
 */
public class LayerButtonLayout implements LayoutManager {
    //    private Component visibilityButton;
    private Component nameEditor;
    //    private Component layerIcon;
    private List<Component> icons = new ArrayList<>(3);

    public static final String ICON = "ICON";
    public static final String NAME_EDITOR = "NAME_EDITOR";

    private static final int GAP = 7;
    public static final int ICON_SIZE = 24;
    private static final int HEIGHT = ICON_SIZE + 2 * GAP;

    public LayerButtonLayout() {
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            if (ICON.equals(name)) {
                icons.add(comp);
            } else if (NAME_EDITOR.equals(name)) {
                nameEditor = comp;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        icons.remove(comp);
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
            for (Component icon : icons) {
                icon.setBounds(startX, GAP, ICON_SIZE, ICON_SIZE);
                startX += ICON_SIZE;
                startX += GAP;
            }

            int editorHeight = (int) nameEditor.getPreferredSize().getHeight();

            int remainingWidth = parent.getWidth() - startX;
            int adjustment = 2; // seems that the textfield has two invisible pixels around it
            nameEditor.setBounds(startX - adjustment, GAP - adjustment, remainingWidth - 3, editorHeight);
        }
    }
}
