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

package pixelitor.tools;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Provides a "hand tool"-like behavior (the contents of a scroll pane can be
 * moved with a mouse drag) to the hand tool and other components
 */
public class HandToolSupport {
    private int startX;
    private int startY;
    private int maxScrollPositionX;
    private int maxScrollPositionY;

    public void mousePressed(MouseEvent e, JViewport viewport) {
        startX = e.getX();
        startY = e.getY();

        Dimension viewSize = viewport.getViewSize();
        Dimension extentSize = viewport.getExtentSize(); // the size of the visible part of the view in view coordinates

        maxScrollPositionX = viewSize.width - extentSize.width;
        maxScrollPositionY = viewSize.height - extentSize.height;
    }

    public void mouseDragged(MouseEvent e, JViewport viewport) {
        int dx = e.getX() - startX;
        int dy = e.getY() - startY;
        Point scrollPos = viewport.getViewPosition();
        scrollPos.x -= dx;
        scrollPos.y -= dy;
        if (scrollPos.x < 0) {
            scrollPos.x = 0;
        }
        if (scrollPos.y < 0) {
            scrollPos.y = 0;
        }
        if (scrollPos.x > maxScrollPositionX) {
            scrollPos.x = maxScrollPositionX;
        }
        if (scrollPos.y > maxScrollPositionY) {
            scrollPos.y = maxScrollPositionY;
        }

        viewport.setViewPosition(scrollPos);
    }

    /**
     * Adds the "hand tool"-like behavior to the given scroll pane
     */
    public static void addBehavior(JScrollPane scrollPane) {
        scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        HandToolSupport support = new HandToolSupport();
        JViewport viewport = scrollPane.getViewport();
        Component panel = viewport.getView();

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                support.mousePressed(e, viewport);
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                support.mouseDragged(e, viewport);
            }
        });

    }
}
