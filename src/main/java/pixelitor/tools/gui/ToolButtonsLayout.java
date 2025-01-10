/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * A layout manager designed specifically for tool buttons.
 * It arranges equal-sized components in a vertical column,
 * but automatically creates additional columns when vertical space is
 * constrained. All components will have the same size,
 * which is specified when creating the layout manager.
 */
public class ToolButtonsLayout implements LayoutManager {
    private final int buttonWidth;
    private final int buttonHeight;
    private final int gap; // gap between components

    public ToolButtonsLayout(int buttonWidth, int buttonHeight, int gap) {
        this.buttonWidth = buttonWidth;
        this.buttonHeight = buttonHeight;
        this.gap = gap;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    private Dimension calculateSize(Container parent) {
        int componentCount = parent.getComponentCount();
        int availableHeight = parent.getHeight();
        if (componentCount == 0 || availableHeight == 0) {
            return new Dimension(0, 0);
        }

        // calculate optimal number of columns based on available height
        int maxRowsInSingleColumn = Math.max(1, (availableHeight + gap) / (buttonHeight + gap));
        int columns = Math.min(componentCount, (int) Math.ceil((double) componentCount / maxRowsInSingleColumn));
        int rows = (int) Math.ceil((double) componentCount / columns);

        return new Dimension(
            columns * buttonWidth + (columns - 1) * gap,
            rows * (buttonHeight + gap) - gap
        );
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return calculateSize(parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return calculateSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        int componentCount = parent.getComponentCount();
        if (componentCount == 0) {
            return;
        }

        // calculate optimal number of columns based on available height
        int availableHeight = parent.getHeight();
        int maxRowsInSingleColumn = Math.max(1, (availableHeight + gap) / (buttonHeight + gap));
        int columns = Math.min(componentCount, (int) Math.ceil((double) componentCount / maxRowsInSingleColumn));
//        int rows = (int) Math.ceil((double) componentCount / columns);

        // calculate starting x position to center the columns
        int totalWidth = columns * buttonWidth + (columns - 1) * gap;
        int startX = (parent.getWidth() - totalWidth) / 2;

        for (int i = 0; i < componentCount; i++) {
            // fills columns horizontally first (left-to-right)
            // before moving to the next row
            int row = i / columns;
            int column = i % columns;

            int x = startX + column * (buttonWidth + gap);
            int y = row * (buttonHeight + gap);

            Component comp = parent.getComponent(i);
            comp.setBounds(x, y, buttonWidth, buttonHeight);
        }
    }
}