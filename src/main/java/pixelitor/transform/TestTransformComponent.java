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
package pixelitor.transform;

import pixelitor.menus.view.ZoomLevel;

import javax.swing.*;
import java.awt.Graphics;
import java.awt.Graphics2D;

class TestTransformComponent extends JComponent {
    private TransformSupport transformSupport;

    TestTransformComponent(TransformSupport transformSupport) {
        this.transformSupport = transformSupport;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        transformSupport.paintHandles((Graphics2D) g, ZoomLevel.Z100);
    }
}
