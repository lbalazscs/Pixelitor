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

package pixelitor.gui.utils;

import org.jdesktop.swingx.painter.TextPainter;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Information associated with a thumbnail image
 */
public class ThumbInfo {
    public static final String PREVIEW_ERROR = "Preview Error";
    public static final String NO_PREVIEW = "No Preview";

    private static final int SIZE_POS_X = 20;
    private static final int SIZE_POS_Y = 10;

    private final BufferedImage thumb;

    // not null if the thumb wasn't generated successfully
    private final String errMsg;

    // these sizes refer to the original image, not to the thumb!
    private final int origWidth;
    private final int origHeight;

    private ThumbInfo(BufferedImage thumb, int origWidth, int origHeight, String errMsg) {
        this.thumb = thumb;
        this.origWidth = origWidth;
        this.origHeight = origHeight;
        this.errMsg = errMsg;
    }

    public static ThumbInfo success(BufferedImage thumb, int origWidth, int origHeight) {
        return new ThumbInfo(thumb, origWidth, origHeight, null);
    }

    public static ThumbInfo failure(int origWidth, int origHeight, String errMsg) {
        return new ThumbInfo(null, origWidth, origHeight, errMsg);
    }

    public static ThumbInfo failure(String errMsg) {
        return failure(-1, -1, errMsg);
    }

    public BufferedImage getThumb() {
        return thumb;
    }

    public void paint(Graphics2D g, JPanel panel) {
        int width = panel.getWidth();
        int height = panel.getHeight();
        if (errMsg != null) {
            g.setColor(WHITE);
            g.fillRect(0, 0, width, height);
            new TextPainter(errMsg, panel.getFont(), Color.RED)
                .paint(g, null, width, height);
            paintImageSize(g);
            return;
        }

        int x = (width - thumb.getWidth()) / 2 + ImagePreviewPanel.EMPTY_SPACE_AT_LEFT;
        int y = (height - thumb.getHeight()) / 2;
        g.drawImage(thumb, x, y, null);

        paintImageSize(g);
    }

    private void paintImageSize(Graphics2D g) {
        if (origWidth == -1 || origHeight == -1) {
            return;
        }

        String msg = "Size: " + origWidth + " x " + origHeight + " pixels";

        g.setColor(BLACK);
        g.drawString(msg, SIZE_POS_X, SIZE_POS_Y);

        g.setColor(WHITE);
        g.drawString(msg, SIZE_POS_X - 1, SIZE_POS_Y - 1);
    }
}
