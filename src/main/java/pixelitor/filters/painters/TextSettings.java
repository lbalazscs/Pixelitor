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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.AbstractLayoutPainter;
import org.jdesktop.swingx.painter.TextPainter;
import pixelitor.colors.ColorUtils;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * Text settings for the text filter and text layers
 */
public class TextSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private String text;
    private final Font font;
    private final AreaEffects areaEffects;
    private final Color color;
    private final AbstractLayoutPainter.VerticalAlignment verticalAlignment;
    private final AbstractLayoutPainter.HorizontalAlignment horizontalAlignment;
    private final boolean watermark;

    public TextSettings(String text, Font font, Color color, AreaEffects areaEffects, AbstractLayoutPainter.HorizontalAlignment horizontalAlignment, AbstractLayoutPainter.VerticalAlignment verticalAlignment, boolean watermark) {
        this.areaEffects = areaEffects;
        this.color = color;
        this.font = font;
        this.horizontalAlignment = horizontalAlignment;
        this.text = text;
        this.verticalAlignment = verticalAlignment;
        this.watermark = watermark;
    }

    // copy constructor
    public TextSettings(TextSettings other) {
        text = other.text;
        font = other.font;
        // we can share even mutable objects, since they are re-created
        // after every editing
        areaEffects = other.areaEffects;
        color = other.color;
        verticalAlignment = other.verticalAlignment;
        horizontalAlignment = other.horizontalAlignment;
        watermark = other.watermark;
    }

    public AreaEffects getAreaEffects() {
        return areaEffects;
    }

    public Color getColor() {
        return color;
    }

    public Font getFont() {
        return font;
    }

    public AbstractLayoutPainter.HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public AbstractLayoutPainter.VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public boolean isWatermark() {
        return watermark;
    }

    public void randomizeText() {
        Random random = new Random();
        text = Long.toHexString(random.nextLong());
    }

    public void configurePainter(TextPainter painter) {
        painter.setAntialiasing(true);
        painter.setText(text);
        painter.setFont(font);
        if (areaEffects != null) {
            painter.setAreaEffects(areaEffects.asArray());
        }
        painter.setHorizontalAlignment(horizontalAlignment);
        painter.setVerticalAlignment(verticalAlignment);
    }

    public BufferedImage watermarkImage(BufferedImage src, TextPainter textPainter) {
        BufferedImage dest;
        int width = src.getWidth();
        int height = src.getHeight();
        // the text is with white on black background on the bump map image
        BufferedImage bumpImage = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics2D g = bumpImage.createGraphics();
        g.setColor(BLACK);
        g.fillRect(0, 0, width, height);
        textPainter.setFillPaint(WHITE);
        textPainter.paint(g, this, width, height);
        g.dispose();

        dest = ImageUtils.bumpMap(src, bumpImage, null);
        return dest;
    }

    public static TextSettings createRandomSettings(Random rand) {
        TextSettings ts = new TextSettings(Utils.getRandomString(10),
                new Font(Font.SANS_SERIF, Font.BOLD, 100),
                ColorUtils.getRandomColor(false),
                AreaEffects.createRandom(rand),
                AbstractLayoutPainter.HorizontalAlignment.CENTER,
                AbstractLayoutPainter.VerticalAlignment.CENTER,
                rand.nextBoolean());
        return ts;
    }
}
