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

package pixelitor.utils;

import pixelitor.tools.gui.ToolSettingsPanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * Custom layout manager for {@link ToolSettingsPanel}.
 *
 * It is similar to a left-aligned FlowLayout, but
 * the components are centered vertically.
 */
public class ToolSettingsLayout implements LayoutManager {
    private static final int HEIGHT = 38;
    private static final Dimension PREFERRED_SIZE = new Dimension(100, HEIGHT);
    private static final int HORIZONTAL_GAP = 5;

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // not used
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // not used
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return PREFERRED_SIZE;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return PREFERRED_SIZE;
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int numChildren = parent.getComponentCount();
            int currentX = HORIZONTAL_GAP;
            for (int i = 0; i < numChildren; i++) {
                Component child = parent.getComponent(i);
                Dimension childSize = child.getPreferredSize();
                child.setSize(childSize);
                child.setLocation(currentX, (HEIGHT - childSize.height) / 2);
                currentX += (childSize.width + HORIZONTAL_GAP);
            }
        }
    }
}
