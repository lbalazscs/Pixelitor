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

/**
 * Adds hand-tool-style panning behavior to a scroll pane’s view component.
 */
public class ViewportPanner {
    private int dragStartX;
    private int dragStartY;
    private int maxScrollX;
    private int maxScrollY;

    private boolean panningActive;

    public void mousePressed(MouseEvent e, JViewport viewport) {
        dragStartX = e.getX();
        dragStartY = e.getY();

        Dimension viewSize = viewport.getViewSize();
        Dimension extentSize = viewport.getExtentSize(); // size of the visible portion of the view, in view coordinates

        maxScrollX = viewSize.width - extentSize.width;
        maxScrollY = viewSize.height - extentSize.height;
        assert maxScrollX >= 0 && maxScrollY >= 0;

        panningActive = true;
    }

    public void mouseDragged(MouseEvent e, JViewport viewport) {
        if (!panningActive) {
            mousePressed(e, viewport); // treat as the start of a new pan
            return;
        }
        if (maxScrollX == 0 && maxScrollY == 0) {
            return; // content fits entirely within viewport; nothing to scroll
        }

        // e.getX()/e.getY() are relative to the panned view component,
        // which JViewport repositions on every scroll.
        // So these coordinates already reflect the latest scroll offset,
        // which is why comparing them to the fixed dragStartX/Y from
        // mousePressed and adding the result to the *current* view position
        // correctly accumulates the pan distance across the whole gesture.
        Point scrollPos = viewport.getViewPosition();
        scrollPos.translate(
            dragStartX - e.getX(),
            dragStartY - e.getY());

        scrollPos.x = Math.clamp(scrollPos.x, 0, maxScrollX);
        scrollPos.y = Math.clamp(scrollPos.y, 0, maxScrollY);

        viewport.setViewPosition(scrollPos);
    }

    public void mouseReleased() {
        reset();
    }

    public void reset() {
        panningActive = false;
    }

    /**
     * Adds the "hand tool"-like panning behavior to the given scroll pane.
     */
    public static void enablePanning(JScrollPane scrollPane) {
        JViewport viewport = scrollPane.getViewport();
        Component viewComponent = viewport.getView();
        assert viewComponent != null;

        viewComponent.setCursor(Cursors.HAND);
        ViewportPanner panner = new ViewportPanner();

        var mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                panner.mousePressed(e, viewport);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                panner.mouseDragged(e, viewport);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                panner.mouseReleased();
            }
        };

        viewComponent.addMouseListener(mouseHandler);
        viewComponent.addMouseMotionListener(mouseHandler);
    }
}
