/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.OpenImages;
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
 * The image selector part of an {@link ImagePositionParamGUI}
 */
public class ImagePositionSelector extends JComponent implements MouseMotionListener, MouseListener {
    private static final int CENTRAL_SQUARE_SIZE = 5;

    private final ImagePositionParamGUI parentGUI;
    private final ImagePositionParam model;
    private BufferedImage thumb;

    public ImagePositionSelector(ImagePositionParamGUI parentGUI,
                                 ImagePositionParam model, int size) {
        this.parentGUI = parentGUI;
        this.model = model;
        addMouseListener(this);
        addMouseMotionListener(this);

        if (AppContext.isUnitTesting()) {
            return;
        }
        Drawable dr = OpenImages.getActiveDrawable();
        BufferedImage actualImage = dr.getImageForFilterDialogs();
        thumb = ImageUtils.createThumbnail(actualImage, size, null);
        setPreferredSize(new Dimension(thumb.getWidth(), thumb.getHeight()));
    }

    @Override
    public void paintComponent(Graphics g) {
        int thumbWidth = thumb.getWidth();
        int thumbHeight = thumb.getHeight() - 1;

        g.drawImage(thumb, 0, 0, null);

        Graphics2D g2 = (Graphics2D) g;
        int x = (int) (model.getRelativeX() * thumbWidth);
        int y = (int) (model.getRelativeY() * thumbHeight);

        drawLines(g2, x, y, thumbWidth, thumbHeight);

        drawCentralSquare(g2, x, y);
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

    private static void drawCentralSquare(Graphics2D g2, int x, int y) {
        g2.setColor(BLACK);
        g2.draw(new Rectangle2D.Float(
            x - CENTRAL_SQUARE_SIZE, y - CENTRAL_SQUARE_SIZE,
            CENTRAL_SQUARE_SIZE * 2, CENTRAL_SQUARE_SIZE * 2));
        g2.setColor(WHITE);
        g2.fill(new Rectangle2D.Float(
            x - CENTRAL_SQUARE_SIZE + 1, y - CENTRAL_SQUARE_SIZE + 1,
            CENTRAL_SQUARE_SIZE * 2 - 1, CENTRAL_SQUARE_SIZE * 2 - 1));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        moveControl(e, true);
    }

    private void moveControl(MouseEvent e, boolean isAdjusting) {
        if (!isEnabled()) {
            return;
        }
        int mouseX = e.getX();
        int mouseY = e.getY();

        float relX = ((float) mouseX) / thumb.getWidth();
        float relY = ((float) mouseY) / thumb.getHeight();
        model.setRelativeValues(relX, relY, false, isAdjusting, true);

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
        moveControl(e, false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
