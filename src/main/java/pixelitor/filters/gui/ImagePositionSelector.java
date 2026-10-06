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

package pixelitor.filters.gui;

import pixelitor.Views;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Thumbnails;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * A component that displays a thumbnail image and allows users
 * to select a position by clicking or dragging.
 */
public class ImagePositionSelector extends JComponent {
    public static final int THUMBNAIL_SIZE = 100;

    private final ImagePositionParam model;
    private BufferedImage thumb; // a thumbnail image for the background

    public ImagePositionSelector(ImagePositionParam model) {
        this.model = model;

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updatePosition(e, true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updatePosition(e, false);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        createThumbnail();

        setPreferredSize(new Dimension(
            thumb.getWidth(),
            thumb.getHeight()));
    }

    private void createThumbnail() {
        Drawable dr = Views.getActiveDrawable();
        if (dr == null) {
            // no active drawable, e.g. when editing an adjustment layer
            thumb = ImageUtils.createSysCompatibleImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        } else {
            BufferedImage sourceImage = dr.getImageForFilterDialogs();
            thumb = Thumbnails.createThumbnail(sourceImage, THUMBNAIL_SIZE, null);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        int thumbWidth = thumb.getWidth();
        int thumbHeight = thumb.getHeight();

        // offsets to center the thumbnail
        int xOffset = getXOffset();
        int yOffset = getYOffset();

        if (!isEnabled()) {
            g.setColor(Color.GRAY);
            g.drawRect(xOffset, yOffset, thumbWidth - 1, thumbHeight - 1);
            return;
        }

        g.drawImage(thumb, xOffset, yOffset, null);

        // draws the position indicator, consisting of
        // a crosshair and a central square marker
        int x = xOffset + (int) (model.getRelativeX() * thumbWidth);
        int y = yOffset + (int) (model.getRelativeY() * thumbHeight);

        // the crosshair should not be restricted to the thumbnail's
        // dimensions, because users are allowed to select outside the bounds
        CrosshairPainter.paint(g, x, y, getWidth(), getHeight());
    }

    private void updatePosition(MouseEvent e, boolean isAdjusting) {
        if (!isEnabled()) {
            return;
        }

        // offsets for mouse interactions
        int xOffset = getXOffset();
        int yOffset = getYOffset();

        // doesn't clamp mouse coordinates to allow selecting positions outside the image
        double relX = (e.getX() - xOffset) / (double) thumb.getWidth();
        double relY = (e.getY() - yOffset) / (double) thumb.getHeight();

        model.setRelativePosition(relX, relY, true, isAdjusting, true);
    }

    private int getXOffset() {
        return (getWidth() - thumb.getWidth()) / 2;
    }

    private int getYOffset() {
        return (getHeight() - thumb.getHeight()) / 2;
    }
}
