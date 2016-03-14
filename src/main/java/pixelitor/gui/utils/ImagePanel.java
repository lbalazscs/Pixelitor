/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

public class ImagePanel extends JPanel {
    protected BufferedImage image;
    private final boolean drawCheckerBoard;
    private CheckerboardPainter checkerboardPainter;

    public ImagePanel(boolean drawCheckerBoard) {
        this.drawCheckerBoard = drawCheckerBoard;
        if (drawCheckerBoard) {
            checkerboardPainter = ImageUtils.createCheckerboardPainter();
        }
    }

    // used for the original
    public void setImage(BufferedImage image) {
        this.image = image;
    }

    // used for the preview
    public void updateImage(BufferedImage newImage) {
        if (image != null) {
            image.flush();
        }

        image = newImage;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // otherwise strange artifacts happen when the panel is larger than the image
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
