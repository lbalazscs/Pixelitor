/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools;

import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Adds hand-tool-style panning behavior to a scroll pane’s view component.
 */
public class ViewportPanner {
    private int dragStartX;
    private int dragStartY;
    private int maxScrollX;
    private int maxScrollY;

    public void mousePressed(MouseEvent e, JViewport viewport) {
        dragStartX = e.getX();
        dragStartY = e.getY();

        Dimension viewSize = viewport.getViewSize();
        Dimension extentSize = viewport.getExtentSize(); // size of the visible portion of the view, in view coordinates

        maxScrollX = viewSize.width - extentSize.width;
        maxScrollY = viewSize.height - extentSize.height;
    }

    public void mouseDragged(MouseEvent e, JViewport viewport) {
        Point scrollPos = viewport.getViewPosition();
        scrollPos.translate(
            dragStartX - e.getX(),
            dragStartY - e.getY());

        scrollPos.x = Math.clamp(scrollPos.x, 0, maxScrollX);
        scrollPos.y = Math.clamp(scrollPos.y, 0, maxScrollY);

        viewport.setViewPosition(scrollPos);
    }

    /**
     * Adds the "hand tool"-like panning behavior to the given scroll pane.
     */
    public static void enablePanning(JScrollPane scrollPane) {
        scrollPane.setCursor(Cursors.HAND);
        ViewportPanner panner = new ViewportPanner();
        JViewport viewport = scrollPane.getViewport();
        Component panel = viewport.getView();

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                panner.mousePressed(e, viewport);
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                panner.mouseDragged(e, viewport);
            }
        });
    }
}
