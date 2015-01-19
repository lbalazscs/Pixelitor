/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.AbstractLayoutPainter;
import org.jdesktop.swingx.painter.TextPainter;
import org.jdesktop.swingx.painter.effects.AreaEffect;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Adds a centered text to the current layer
 */
public class TextFilter extends FilterWithGUI {
    private String text = "Pixelitor";
    private Font font = new Font(Font.SANS_SERIF, Font.BOLD, 50);
    private AreaEffect[] areaEffects;
    private Color color = Color.BLACK;

    private AbstractLayoutPainter.VerticalAlignment verticalAlignment;
    private AbstractLayoutPainter.HorizontalAlignment horizontalAlignment;

    public static final TextFilter INSTANCE = new TextFilter();
    private boolean watermark;

    private TextFilter() {
        super("Text");
        copySrcToDstBeforeRunning = true;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (text.isEmpty()) {
            return src;
        }

        int width = dest.getWidth();
        int height = dest.getHeight();

        TextPainter textPainter = new TextPainter();
        textPainter.setAntialiasing(true);
        textPainter.setText(text);
        textPainter.setFont(font);
        textPainter.setAreaEffects(areaEffects);

        textPainter.setHorizontalAlignment(horizontalAlignment);
        textPainter.setVerticalAlignment(verticalAlignment);

        if (watermark) {
            // the text is with white on black background on the bump map image
            BufferedImage bumpImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bumpImage.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            textPainter.setFillPaint(Color.WHITE);
            textPainter.paint(g, this, width, height);
            g.dispose();

            dest = ImageUtils.bumpMap(src, bumpImage);
        } else {
            textPainter.setFillPaint(color);

            Graphics2D g = dest.createGraphics();
            textPainter.paint(g, this, width, height);
            g.dispose();
        }

        return dest;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setText(String s) {
        text = s;
    }

    public void setAreaEffects(AreaEffect[] areaEffects) {
        this.areaEffects = areaEffects;
    }


    @Override
    public AdjustPanel createAdjustPanel() {
        return new TextFilterAdjustments(this);
    }

    public void setWatermark(boolean watermark) {
        this.watermark = watermark;
    }

    public void setVerticalAlignment(AbstractLayoutPainter.VerticalAlignment verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public void setHorizontalAlignment(AbstractLayoutPainter.HorizontalAlignment horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void randomizeSettings() {
        Random random = new Random();
        text = Long.toHexString(random.nextLong());
    }
}