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

package pixelitor.utils;

import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.Views;
import pixelitor.gui.View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

/**
 * Static utility methods related to thumbnails.
 */
public class Thumbnails {
    // the current layer thumbnail size
    private static int maxSize;

    private Thumbnails() {
    }

    public static void updateThumbSize(int newThumbSize) {
        if (maxSize == newThumbSize) {
            return;
        }
        maxSize = newThumbSize;

        // since the layer GUIs are cached, all views have
        // to be notified to update their buttons
        for (View view : Views.getAll()) {
            view.updateThumbSize(newThumbSize);
        }
    }

    public static int getMaxSize() {
        return maxSize;
    }

    public static BufferedImage createThumbnail(BufferedImage src, CheckerboardPainter painter) {
        return createThumbnail(src, maxSize, maxSize, painter);
    }

    /**
     * Creates a thumbnail by scaling the source image to fit within a square of the given size.
     */
    public static BufferedImage createThumbnail(BufferedImage src, int size, CheckerboardPainter painter) {
        return createThumbnail(src, size, size, painter);
    }

    public static BufferedImage createThumbnail(BufferedImage src,
                                                int maxWidth, int maxHeight,
                                                CheckerboardPainter painter) {
        assert src != null;

        Dimension thumbDim = calcThumbDimensions(src.getWidth(), src.getHeight(), maxWidth, maxHeight, true);
        return renderThumbnail(src, thumbDim.width, thumbDim.height, painter);
    }

    public static BufferedImage createCircleThumb(Color color) {
        BufferedImage img = createEmpty();
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, maxSize, maxSize);
        g2.dispose();
        return img;
    }

    public static BufferedImage createEmpty() {
        return ImageUtils.createSysCompatibleImage(maxSize, maxSize);
    }

    public static void paintBackground(Graphics2D g, CheckerboardPainter painter) {
        painter.paint(g, null, maxSize, maxSize);
    }

    public static void paintRedX(BufferedImage thumb) {
        int thumbWidth = thumb.getWidth();
        int thumbHeight = thumb.getHeight();

        Graphics2D g = thumb.createGraphics();

        g.setColor(new Color(200, 0, 0));
        g.setStroke(new BasicStroke(2.5f));
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.drawLine(0, 0, thumbWidth, thumbHeight);
        g.drawLine(thumbWidth - 1, 0, 0, thumbHeight - 1);
        g.dispose();
    }

    /**
     * Calculates the target dimensions if an image needs to be resized
     * to fit into a box of a given size without distorting the aspect ratio.
     */
    public static Dimension calcThumbDimensions(int srcWidth, int srcHeight, int size, boolean upscale) {
        return calcThumbDimensions(srcWidth, srcHeight, size, size, upscale);
    }

    public static Dimension calcThumbDimensions(int srcWidth, int srcHeight, int maxWidth, int maxHeight, boolean upscale) {
        if (!upscale && srcWidth <= maxWidth && srcHeight <= maxHeight) {
            // the image already fits in the box and no up-scaling is needed
            return new Dimension(srcWidth, srcHeight);
        }

        double horScale = (double) maxWidth / srcWidth;
        double verScale = (double) maxHeight / srcHeight;
        double scale = Math.min(horScale, verScale);

        int thumbWidth = Math.max(1, (int) (srcWidth * scale));
        int thumbHeight = Math.max(1, (int) (srcHeight * scale));
        return new Dimension(thumbWidth, thumbHeight);
    }

    private static BufferedImage renderThumbnail(BufferedImage src,
                                                 int thumbWidth, int thumbHeight,
                                                 CheckerboardPainter painter) {
        BufferedImage thumb = ImageUtils.createSysCompatibleImage(thumbWidth, thumbHeight);
        Graphics2D g = thumb.createGraphics();

        if (painter != null) {
            painter.paint(g, null, thumbWidth, thumbHeight);
        }

        // use nearest-neighbor for performance, as thumbnails are small and frequently updated
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, thumbWidth, thumbHeight, null);
        g.dispose();

        return thumb;
    }
}
