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
 * A panel that shows an image,
 * optionally on a checkerboard
 */
public class ImagePanel extends JPanel {
    protected BufferedImage image;
    private final boolean drawCheckerBoard;
    private CheckerboardPainter checkerboardPainter;

    public ImagePanel(boolean useCheckerBoard) {
        drawCheckerBoard = useCheckerBoard;
        if (useCheckerBoard) {
            checkerboardPainter = ImageUtils.createCheckerboardPainter();
        }
    }

    public void setImage(BufferedImage newImage) {
        if (image != null) {
            image.flush();
        }
        this.image = newImage;
    }

    public void changeImage(BufferedImage newImage) {
        setImage(newImage);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // for the case when the panel is larger than the image
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (image == null) {
            return;
        }

        try {
            if (drawCheckerBoard) {
                Graphics2D g2 = (Graphics2D) g;
                checkerboardPainter.paint(g2, null, image.getWidth(), image.getHeight());
            }
            g.drawImage(image, 0, 0, null);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }
}
