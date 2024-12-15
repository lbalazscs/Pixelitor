/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import pixelitor.AppMode;
import pixelitor.Views;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * A component that displays a thumbnail image and allows users
 * to select a position by clicking or dragging.
 */
public class ImagePositionSelector extends JComponent implements MouseMotionListener, MouseListener {
    private static final int MARKER_SQUARE_SIZE = 5;

    private final ImagePositionParamGUI parentGUI;
    private final ImagePositionParam model;
    private BufferedImage thumb; // a thumbnail image for the background

    public ImagePositionSelector(ImagePositionParamGUI parentGUI,
                                 ImagePositionParam model,
                                 int thumbnailSize) {
        this.parentGUI = parentGUI;
        this.model = model;

        addMouseListener(this);
        addMouseMotionListener(this);

        if (AppMode.isUnitTesting()) {
            // Had spurious failures on Linux in createTumbnail().
            // Should be fixed, but left the workaround for now.
            return;
        }

        createThumbnail(thumbnailSize);

        setPreferredSize(new Dimension(
            thumb.getWidth(),
            thumb.getHeight()));
    }

    private void createThumbnail(int thumbnailSize) {
        Drawable dr = Views.getActiveDrawable();
        if (dr == null) {
            // running as adjustment layer
            thumb = ImageUtils.createSysCompatibleImage(thumbnailSize, thumbnailSize);
        } else {
            BufferedImage sourceImage = dr.getImageForFilterDialogs();
            thumb = ImageUtils.createThumbnail(sourceImage, thumbnailSize, null);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        // no anti-aliasing is needed, because everything
        // is perfectly horizontal or vertical
        Graphics2D g2 = (Graphics2D) g;
        g.drawImage(thumb, 0, 0, null);

        // draws the position indicator, consisting of
        // intersecting lines and a central square marker
        int thumbWidth = thumb.getWidth();
        int thumbHeight = thumb.getHeight() - 1;
        int x = (int) (model.getRelativeX() * thumbWidth);
        int y = (int) (model.getRelativeY() * thumbHeight);
        drawLines(g2, x, y, thumbWidth, thumbHeight);
        drawCentralMarker(g2, x, y);
    }

    private static void drawLines(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(BLACK);
        g2.drawLine(x + 1, 0, x + 1, height); // vertical west
        g2.drawLine(x - 1, 0, x - 1, height); // vertical east

        if (y < height) {
            g2.drawLine(0, y - 1, width, y - 1); // horizontal north
            g2.drawLine(0, y + 1, width, y + 1); // horizontal south
        }

        g2.setColor(WHITE);
        g2.drawLine(x, 0, x, height); // vertical
        if (y < height) {
            g2.drawLine(0, y, width, y); // horizontal
        }
    }

    private static void drawCentralMarker(Graphics2D g2, int x, int y) {
        g2.setColor(BLACK);
        g2.draw(new Rectangle2D.Float(
            x - MARKER_SQUARE_SIZE, y - MARKER_SQUARE_SIZE,
            MARKER_SQUARE_SIZE * 2, MARKER_SQUARE_SIZE * 2));
        g2.setColor(WHITE);
        g2.fill(new Rectangle2D.Float(
            x - MARKER_SQUARE_SIZE + 1, y - MARKER_SQUARE_SIZE + 1,
            MARKER_SQUARE_SIZE * 2 - 1, MARKER_SQUARE_SIZE * 2 - 1));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updatePosition(e, true);
    }

    private void updatePosition(MouseEvent e, boolean isAdjusting) {
        if (!isEnabled()) {
            return;
        }

        double relX = ((double) e.getX()) / thumb.getWidth();
        double relY = ((double) e.getY()) / thumb.getHeight();

        model.setRelativePosition(relX, relY, false, isAdjusting, true);
        parentGUI.updateSlidersFromModel();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        updatePosition(e, false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
