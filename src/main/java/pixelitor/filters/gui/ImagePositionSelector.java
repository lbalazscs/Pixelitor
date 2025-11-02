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

package pixelitor.filters.gui;

import pixelitor.AppMode;
import pixelitor.Views;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Thumbnails;

import javax.swing.*;
import java.awt.Color;
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
            // This workaround should no longer be necessary.
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
            thumb = Thumbnails.createThumbnail(sourceImage, thumbnailSize, null);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        int thumbWidth = thumb.getWidth();
        int thumbHeight = thumb.getHeight();

        if (!isEnabled()) {
            g.setColor(Color.GRAY);
            g.drawRect(0, 0, thumbWidth - 1, thumbHeight - 1);
            return;
        }

        // no anti-aliasing is needed, because everything
        // is perfectly horizontal or vertical
        Graphics2D g2 = (Graphics2D) g;
        g.drawImage(thumb, 0, 0, null);

        // draws the position indicator, consisting of
        // a crosshair and a central square marker
        int x = (int) (model.getRelativeX() * thumbWidth);
        int y = (int) (model.getRelativeY() * thumbHeight);

        drawCrosshair(g2, x, y, thumbWidth, thumbHeight);
        drawCentralMarker(g2, x, y);
    }

    private static void drawCrosshair(Graphics2D g2, int x, int y, int width, int height) {
        // draw a 1px white crosshair with a 1px black outline
        g2.setColor(BLACK);
        // black outline for vertical line
        if (x > 0) {
            g2.drawLine(x - 1, 0, x - 1, height - 1); // west
        }
        if (x < width - 1) {
            g2.drawLine(x + 1, 0, x + 1, height - 1); // east
        }
        // black outline for horizontal line
        if (y > 0) {
            g2.drawLine(0, y - 1, width - 1, y - 1); // north
        }
        if (y < height - 1) {
            g2.drawLine(0, y + 1, width - 1, y + 1); // south
        }

        g2.setColor(WHITE);
        // white center lines
        g2.drawLine(x, 0, x, height - 1); // vertical
        g2.drawLine(0, y, width - 1, y); // horizontal
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

        // doesn't clamp mouse coordinates to allow selecting positions outside the image
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
