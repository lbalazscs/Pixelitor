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

package pixelitor.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * Similar to a left-aligned FlowLayout, but
 * the components are centered vertically
 */
public class ToolSettingsLayout implements LayoutManager {
    private static final int HEIGHT = 38;
    private static final Dimension size = new Dimension(100, HEIGHT);
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
        return size;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return size;
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int numChildren = parent.getComponentCount();
            int x = HORIZONTAL_GAP;
            for (int i = 0; i < numChildren; i++) {
                Component m = parent.getComponent(i);
                Dimension childSize = m.getPreferredSize();
                m.setSize(childSize);
                m.setLocation(x, (HEIGHT - childSize.height) / 2);
                x += (childSize.width + HORIZONTAL_GAP);
            }
        }
    }
}
