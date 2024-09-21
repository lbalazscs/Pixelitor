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

package pixelitor.gui.utils;

import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A panel that displays an image with an optional checkerboard background.
 */
public class ImagePanel extends JPanel {
    protected BufferedImage image;
    private final boolean isCheckerboardEnabled;
    private CheckerboardPainter checkerboardPainter;

    public ImagePanel(boolean useCheckerBoard) {
        isCheckerboardEnabled = useCheckerBoard;
        if (useCheckerBoard) {
            checkerboardPainter = ImageUtils.createCheckerboardPainter();
        }
    }

    public void replaceImage(BufferedImage newImage) {
        // clean up resources of the previous image
        if (image != null) {
            image.flush();
        }

        // sets the new image without repainting
        this.image = newImage;
    }

    public void refreshImage(BufferedImage newImage) {
        replaceImage(newImage);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // needed when the panel is larger than the image
        paintPanelBackground(g);

        if (image == null) {
            return;
        }

        try {
            renderImage(g);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    private void paintPanelBackground(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void renderImage(Graphics g) {
        if (isCheckerboardEnabled) {
            Graphics2D g2 = (Graphics2D) g;
            checkerboardPainter.paint(g2, null, image.getWidth(), image.getHeight());
        }
        g.drawImage(image, 0, 0, null);
    }
}
