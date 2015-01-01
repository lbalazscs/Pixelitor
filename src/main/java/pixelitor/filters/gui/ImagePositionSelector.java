/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.ImageComponents;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * The image selector part of an ImagePositionPanel
 */
public class ImagePositionSelector extends JComponent implements MouseMotionListener, MouseListener {
    private final ImagePositionPanel imagePositionPanel;
    private final ImagePositionParam model;
    private final BufferedImage thumb;
    private static final int CONTROL_SIZE = 5;

    public ImagePositionSelector(ImagePositionPanel imagePositionPanel, ImagePositionParam model, int size) {
        this.imagePositionPanel = imagePositionPanel;
        this.model = model;
        addMouseListener(this);
        addMouseMotionListener(this);

        BufferedImage actualImage = ImageComponents.getActiveImageLayer().get().getImageForFilterDialogs();
        thumb = ImageUtils.createThumbnail(actualImage, size);

        setPreferredSize(new Dimension(thumb.getWidth(), thumb.getHeight()));
    }

    @Override
    public void paintComponent(Graphics g) {
        int totalWidth = thumb.getWidth();
        int totalHeight = thumb.getHeight();

        g.drawImage(thumb, 0, 0, null);

        Graphics2D g2 = (Graphics2D) g;
        int currentX = (int) (model.getRelativeX() * totalWidth);
        int currentY = (int) (model.getRelativeY() * totalHeight);

        // lines
        g.setColor(Color.BLACK);
        g.drawLine(currentX + 1, 0, currentX + 1, totalHeight - 1); // west
        g.drawLine(currentX - 1, 0, currentX - 1, totalHeight - 1); // east

        if (currentY <= totalHeight) {
            Line2D.Float horizontalLineNorth = new Line2D.Float(0, currentY - 1, totalWidth, currentY - 1);
            Line2D.Float horizontalLineSouth = new Line2D.Float(0, currentY + 1, totalWidth, currentY + 1);
            g2.draw(horizontalLineNorth);
            g2.draw(horizontalLineSouth);
        }

        g.setColor(Color.WHITE);
        Line2D.Float verticalLine = new Line2D.Float(currentX, 0, currentX, totalHeight);
        g2.draw(verticalLine);
        if (currentY <= totalHeight) {
            Line2D.Float horizontalLine = new Line2D.Float(0, currentY, totalWidth, currentY);
            g2.draw(horizontalLine);
        }

        // rectangle in the middle
        g.setColor(Color.BLACK);
        g2.draw(new Rectangle2D.Float(currentX - CONTROL_SIZE, currentY - CONTROL_SIZE, CONTROL_SIZE * 2, CONTROL_SIZE * 2));
        g.setColor(Color.WHITE);
        g2.fill(new Rectangle2D.Float(currentX - CONTROL_SIZE + 1, currentY - CONTROL_SIZE + 1, CONTROL_SIZE * 2 - 1, CONTROL_SIZE * 2 - 1));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        moveControl(e);
    }

    private void moveControl(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();

        float relativeX = ((float) mouseX) / thumb.getWidth();
        float relativeY = ((float) mouseY) / thumb.getHeight();
        model.setRelativeX(relativeX);
        model.setRelativeY(relativeY);

        imagePositionPanel.updateSlidersFromModel();

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
        moveControl(e);
        ParamAdjustmentListener listener = model.getAdjustingListener();
        if (listener != null) {
            listener.paramAdjusted();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
